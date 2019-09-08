/*
  Copyright (c) 2019 Paul Stoffregen <paul@pjrc.com>

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.util.List;
import java.util.ArrayList;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.Element;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Segment;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.SwingUtilities;

/*
FifoDocument replaces the Java's AbstractDocument with a very
limited Document optimized for the Arduino Serial Monitor.  It is
capable of running with sustained high-speed USB data transfer.

JTextArea stores text in a Document object.  The default Document
implementation must handle insertion of text at arbitrary locations,
with custom attributes (bold, italic, font, color, etc), and also
removal from arbitrary locations which may partially overlap any
number of previously inserted pieces of text with differing
attributes.

To manage this storage, normally a GapContent class is used.  While
GapContent's performance is described as "generally cheap" in the
Java documentation, each insertion or removal of data results in many
object instances created, updated, and others discarded for later
Java garbage collection.  When subjected to substantially faster than
12 Mbit/sec sustained data flow, the net result is far too much CPU
time consumed, excessive memory use, and far too much load placed on
Java garbage collection.

Unfortunately, Java's AbstractDocument.Content interface is designed
around the assumption of variable size data storage, which changes in
size only when functions are called to insert and remove data.  To
used fixed-size storage and avoid excessive load on Java's garbage
collection, we must replace the entire Document class.  Fortunately,
the Document interface provides an event-based notification system
for changes, which allows FifoDocument to work.

FifoDocument imposes many limitations.  Text insertion is only allowed
to append to the end of the document.  Any text removal deletes the
entire document.  Text attributes are ignored.  UndoableEditListener
is not supported.  Only the minimum Document features needed by the
Arduino Serial Monitor are implemented.

FifoDocument uses two fixed size buffers, one for raw character data,
the other to track which characters belong to which lines.  JTextArea
requires any Document to provide 1 "root" Element, which gives access
to Element instances for each line of data.  Java's Element interface
documentation implies a more complex hierarchy can be used, but for
the Arduino Serial Monitor we only implement the minimum required 2
tiers, a single root and a list of all the lines.  FifoDocument also
implements DocumentEvent, to notify JTextArea of changes, and the
Position interface which JTextArea uses to track selected text when
the user clicks and drags their mouse.

Data is automatically discarded by FifoDocument as the Arduino Serial
Monitor keeps adding more incoming chars, according to 1 of 2 memory
management strategies.

The "scrolling" strategy automatically discards the oldest buffered
data when appending would exceed 60% of the buffer full.  During
sustained use, 40% of the buffer remains unused in preparation for
switching to "still" strategy.

The "still" strategy allows the buffer to reach 100% full.  It then
discards all new incoming text when the buffer is full.  Losing the
newest data may seem horrible, but the asset which matters most is
human attention span.  When the user turns off Autoscroll, even if
they are currently viewing only the end of the buffered text, they may
choose to scroll up and inspect older data.  We must not break the
user's concentration!  The "still" strategy guarantees the user keeps
access to ALL already-buffered data while reading.

While in "scrolling" mode, we send both INSERT and REMOVE events to
DocumentListeners.  In "still" mode, only INSERT events are sent.

Position marking is implemented with 64 long counting of the total
number of characters.  See the comments below for the Position.
*/

public class FifoDocument implements Document
{
	private int char_head;
	private int char_tail;
	private final int char_size;
	private final int char_threshold;
	private char[] char_buf;
	private long char_total;

	private FifoElementRoot line_root;
	private int line_head;
	private int line_tail;
	private final int line_size;
	private final int line_threshold;
	private FifoElementLine[] line_buf;
	private boolean last_line_incomplete;
	private int newline_offset[];

	private final List<DocumentListener> listeners;
	private boolean scrolling = true;
	private final FifoEvent insertEvent;
	private final FifoEvent removeEvent;
	private final FifoPosition startPosition;
	private final FifoPosition endPosition;

	public FifoDocument(int size) {
		// Allocate the big buffer to hold raw character data
		char_size = size;
		char_threshold = (char_size * 6) / 10;
		char_head = 0;
		char_tail = 0;
		char_buf = new char[char_size];
		char_total = 0;

		// Allocate the list of Elements to represent the lines
		line_root = new FifoElementRoot(this);
		line_size = size / 10;
		line_threshold = (line_size * 6) / 10;
		line_head = 0;
		line_tail = 0;
		line_buf = new FifoElementLine[line_size];
		for (int i=0; i < line_size; i++) {
			line_buf[i] = new FifoElementLine(this, i);
		}
		newline_offset = new int[char_size - char_threshold];
		last_line_incomplete = false;

		// Allocate other misc stuff
		listeners = new ArrayList<DocumentListener>();
		insertEvent = new FifoEvent(this, DocumentEvent.EventType.INSERT);
		removeEvent = new FifoEvent(this, DocumentEvent.EventType.REMOVE);
		startPosition = new FifoPosition(this, 0);
		endPosition = new FifoPosition(this, Long.MAX_VALUE);
	}
	public synchronized void setScrollingMode(boolean mode) {
		scrolling = mode;
	}
	public synchronized void free() {
		char_head = char_tail = 0;
		line_head = line_tail = 0;
		char_buf = null;
		line_buf = null;
		newline_offset = null;
		System.gc();
	}

// Throughout FifoDocument, the word "offset" means the zero-based location
// of a character or line, as seen by rest of Arduino.  The words "index",
// "head" and "tail" mean the physical locations of data inside the fixed
// size char_buf and line_buf arrays.

	public synchronized int charIndexToOffset(int index) {
		if (char_buf == null) return 0;
		int offset = index - char_tail - 1;
		if (offset < 0) offset += char_size;
		return offset;
	}
	public synchronized int lineIndexToOffset(int index) {
		if (char_buf == null) return 0;
		int offset = index - line_tail - 1;
		if (offset < 0) offset += line_size;
		return offset;
	}

////////////////////////////////////////////////////////
// Direct Append
////////////////////////////////////////////////////////

	public char[] getBuffer() {
		return char_buf;
	}
	public synchronized int getAppendIndex() {
		int head = char_head + 1;
		if (head >= char_size) head = 0;
		return head;
	}
	public synchronized int getAvailableToAppend() {
		int head = char_head + 1;
		if (head >= char_size) head = 0;
		int tail = char_tail + 1;
		if (tail >= char_size) tail = 0;
		int available;
		if (head < tail) {
			// free space is from head to tail-1
			available = tail - head - 1;
		} else {
			// free space is from head to end of buffer
			available = char_size - head;
		}
		if (available > char_size - char_threshold) {
			available = char_size - char_threshold;
		}
		// TODO: if not scolling and last line is complete
		//       and line count is at line threshold, return 0
		return available;
	}
	public synchronized void processAppended(int char_len) {
		if (SwingUtilities.isEventDispatchThread() == false) {
			System.err.println("FifoDocument processAppended called from wrong thread");
		}
		//System.out.println("processAppended, len = " + char_len);
		int chead = char_head + 1;
		if (chead >= char_size) chead = 0;

		// count how many newline chars, and record their offsets
		int newline_count = 0;
		for (int i=0; i < char_len; i++) {
			if (char_buf[chead + i] == '\n') newline_offset[newline_count++] = i;
		}

		// compute how many lines this will add to the line buffer
		int line_len;
		if (newline_count > 0 && newline_offset[newline_count - 1] == char_len - 1) {
			// new data ends with a newline
			if (last_line_incomplete) {
				line_len = newline_count - 1;
			} else {
				line_len = newline_count;
			}
		} else {
			// new data ends partial line (no newline at end)
			if (last_line_incomplete) {
				line_len = newline_count;
			} else {
				line_len = newline_count + 1;
			}
		}
		println("processAppended, newline_count = " + newline_count
			+ ", line_len = " + line_len);
		println("  TEXT=\"" + new String(char_buf, chead, char_len) + "\"");

		if (scrolling) {
			// scrolling mode: delete old data as needed to stay under thresholds
			int char_count = getCharCount();
			int line_count = getElementCount();

			int ctail = char_tail;
			int ctail_begin = ctail + 1; // index of 1st deleted char
			if (ctail_begin >= char_size) ctail_begin = 0;
			int cdelete = 0;             // number of chars deleted

			int ltail = line_tail;
			int ltail_begin = ltail + 1; // index of 1st deleted line
			if (ltail_begin >= line_size) ltail_begin = 0;
			int ldelete = 0;             // number of lines deleted

			FifoElementLine shortened = null;

			// step 1: delete old lines until not over line_threshold
			int lines_to_delete = line_count + line_len - line_threshold;
			if (lines_to_delete > 0) {
				println("delete step #1, " + lines_to_delete + " lines");
				ldelete = lines_to_delete;
				line_count -= lines_to_delete;
				do {
					if (++ltail >= line_size) ltail = 0;
					int len = line_buf[ltail].getLength();
					ctail += len;
					if (ctail >= char_size) ctail -= char_size;
					char_count -= len;
					cdelete += len;
				} while (--lines_to_delete > 0);
			}

			// step 2: keep deleting old lines until not over char_threshold
			while ((char_count + char_len > char_threshold) && (line_count > 1)) {
				if (++ltail >= line_size) ltail = 0;
				int len = line_buf[ltail].getLength();
				println("delete step #2, line with " + len
					+ " chars, lines=" + line_count + ", chars=" + char_count
					+ ", thres=" + char_threshold + ", clen=" + char_len);
				ctail += len;
				if (ctail >= char_size) ctail -= char_size;
				char_count -= len;
				cdelete += len;
				line_count--;
				ldelete++;
			}

			// step 3: if still over char_threshold with only 1 last line, shorten it
			int chars_to_delete = char_count + char_len - char_threshold;
			if (chars_to_delete > 0) {
				println("delete step #3, shorten last line by " + chars_to_delete + " chars");
				int last = ltail + 1;
				if (last >= line_size) last = 0;
				shortened = line_buf[last];
				int index = shortened.getIndex();
				int len = shortened.getLength();
				index += chars_to_delete;
				if (index >= char_size) index -= char_size;
				len -= chars_to_delete;
				shortened.set(index, len);
				ctail += chars_to_delete;
				if (ctail >= char_size) ctail -= char_size;
				char_count -= chars_to_delete;
				cdelete += chars_to_delete;
			}

			// if anything was deleted, transmit a remove event
			if (cdelete > 0 || ldelete > 0) {
				char_tail = ctail;
				line_tail = ltail;
				removeEvent.setLineRange(ltail_begin, ldelete);
				removeEvent.setCharRange(ctail_begin, cdelete);
				removeEvent.setAppended(shortened);
				for (DocumentListener d : listeners) {
					d.removeUpdate(removeEvent);
				}
			}
		} else {
			// need to make sure all these new lines can fit.
			int line_count = getElementCount();
			if (line_count + line_len >= line_size) {
				// TODO: truncate input to only lines which fit

				return; // ugly kludge for now, just discard everything new
			}
		}
		if (char_len <= 0) return;

		// new data is now considered part of the buffer, advance head index
		char_head += char_len;
		if (char_head >= char_size) char_head -= char_size;
		char_total += char_len;

		// add text to last line
		int npos = 0;
		int nindex = 0;
		if (last_line_incomplete) {
			if (newline_count > 0) {
				npos = newline_offset[0] + 1;
				line_buf[line_head].increaseLength(npos);
				nindex = 1;
				last_line_incomplete = false;
			} else {
				line_buf[line_head].increaseLength(char_len);
			}
			insertEvent.setAppended(line_buf[line_head]);
		} else {
			insertEvent.setAppended(null);
		}

		// add lines to Element list
		int lhead = line_head + 1;
		if (lhead > line_size) lhead = 0;
		if (line_len > 0) {
			//for (int i = (last_line_incomplete ? 1 : 0); i < newline_count; i++) {
			while (nindex < newline_count) {
				addLine(chead + npos, newline_offset[nindex] - npos + 1);
				npos = newline_offset[nindex] + 1;
				nindex++;
			}
			// add remaining text as last incomplete line
			if (npos >= char_len) {
				last_line_incomplete = false;
			} else {
				addLine(chead + npos, char_len - npos);
				last_line_incomplete = true;
			}
		}

		// transmit insert event
		insertEvent.setCharRange(charIndexToOffset(chead), char_len);
		insertEvent.setLineRange(lineIndexToOffset(lhead), line_len);
		for (DocumentListener d : listeners) {
			d.insertUpdate(insertEvent);
		}

	}


////////////////////////////////////////////////////////
// Document Interface - interesting functions
////////////////////////////////////////////////////////

	public synchronized void insertString(int offs, String s, AttributeSet a) throws BadLocationException {
		// Text can only be added by direct append
	}
	private void addLine(int index, int len) {
		if (index >= char_size) index -= char_size;
		line_head++;
		if (line_head >= line_size) line_head = 0;
		println(" addLine, " + len + " chars, into line index = " + line_head);
		line_buf[line_head].set(index, len);
	}

	public void remove(int offset, int len) throws BadLocationException {
		if (char_buf == null) return;
		println("Document: remove, offset=" + offset + ", len=" + len);
		if (SwingUtilities.isEventDispatchThread()) {
			deleteEverything();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					deleteEverything();
				}
			});
		}
	}
	private synchronized void deleteEverything() {
		int char_len = char_head - char_tail;
		if (char_len == 0) return; // already empty
		if (char_len < 0) char_len += char_size;

		int line_len = line_head - line_tail;
		if (line_len < 0) line_len += line_size;

		removeEvent.setCharRange(0, char_len);
		removeEvent.setLineRange(0, line_len);
		for (DocumentListener d : listeners) {
			d.removeUpdate(removeEvent);
		}
		char_tail = char_head;
		line_tail = line_head;
		last_line_incomplete = false;
	}

	public synchronized void getText(int offset, int length, Segment txt) throws BadLocationException {
		println("Document: getText, offset=" + offset + ", len=" + length);
		if (length < 0 || offset < 0) {
			//System.out.println("FifoDocument.getText ****NEGATIVE NUMBER ERROR****");
			throw new BadLocationException("negative input not allowed", 0);
		}
		int chartotallen = char_head - char_tail;
		if (chartotallen < 0) chartotallen += char_size;
		if (offset + length > chartotallen) {
			throw new BadLocationException("access beyond data", offset + length);
		}
		txt.array = char_buf;
		int index = offset + char_tail + 1;
		if (index >= char_size) index -= char_size;
		txt.offset = index;

		if (index + length < char_size) {
			txt.count = length;
			txt.setPartialReturn(false);
		} else {
			txt.count = char_size - index;
			txt.setPartialReturn(true);
		}
		println("  text=" + txt.toString());
	}
	public synchronized int getCharCount() {
		int len = char_head - char_tail;
		if (len < 0) len += char_size;
		return len;
	}
	public synchronized int getLength() {
		int len = getCharCount();
		println("Document: getLength -> " + len);
		return len;
	}

////////////////////////////////////////////////////////
// Element support functions
////////////////////////////////////////////////////////

	public synchronized int getElementCount() {
		if (char_buf == null) return 0;
		int len = line_head - line_tail;
		if (len < 0) len += line_size;
		return len;
	}
	public synchronized FifoElementLine getElement(int offset) {
		if (char_buf == null) return new FifoElementLine(this, 0);
		int index = offset + line_tail + 1;
		if (index >= line_size) index -= line_size;
		return line_buf[index];
	}
	public synchronized int getLineOffset(int char_offset) {
		if (char_buf == null) return 0;
		// search for the line which contains char_offset
		if (char_offset <= 0) return 0;
		return getLineOffset_BinarySearch(char_offset);
		/*
		int line_offset_linear = getLineOffset_LinearSearch(char_offset);
		int line_offset_binary = getLineOffset_BinarySearch(char_offset);
		if (line_offset_binary != line_offset_linear) {
			System.err.println("Error, FifoDocument line search results differ!");
		}
		return line_offset_linear;
		*/
	}
	private int getLineOffset_LinearSearch(int char_offset) {
		if (char_buf == null) return 0;
		int line_count = getElementCount();
		if (line_count < 1) return 0;
		for(int line_offset = 0; line_offset < line_count; line_offset++) {
			FifoElementLine line = getElement(line_offset);
			int line_begin_offset = line.getStartOffset();
			int line_end_offset = line_begin_offset + line.getLength();
			if (char_offset >= line_begin_offset && char_offset < line_end_offset) {
				//System.err.println("found char " + char_offset
					//+ " at line " + line_offset + " (chars "
					//+ line_begin_offset + " - "
					//+ (line_end_offset - 1) + ")");
				return line_offset;
			}
		}
		return line_count - 1;
	}
	private int getLineOffset_BinarySearch(int char_offset) {
		int begin = 0;
		int end = getElementCount() - 1;
		if (end < 1) return 0;
		while (begin <= end) {
			int middle = begin + (end - begin) / 2;
			FifoElementLine line = getElement(middle);
			int line_begin_offset = line.getStartOffset();
			int line_end_offset = line_begin_offset + line.getLength();
			//System.err.println("begin=" + begin + ", middle=" + middle
				//+ ", end=" + end + " -> chars " + line_begin_offset
				//+ " - " + (line_end_offset - 1));
			if (char_offset >= line_begin_offset && char_offset < line_end_offset) {
				//System.err.println("found at line " + middle);
				return middle;
			} else if (char_offset >= line_begin_offset) {
				begin = middle + 1;
			} else {
				end = middle - 1;
			}
		}
		return getElementCount() - 1;
	}
	public synchronized FifoElementLine[] getElementArray(int offset, int length) {
		// TODO: should we pre-allocate and reuse this array to avoid garbage collection?
		if (char_buf == null) return null;
		FifoElementLine[] list = new FifoElementLine[length];
		int index = offset + line_tail + 1;
		if (index >= line_size) index -= line_size;
		for (int i=0; i < length; i++) {
			list[i] = line_buf[index++];
			if (index >= line_size) index -= line_size;
		}
		return list;
	}

////////////////////////////////////////////////////////
// Position support functions
////////////////////////////////////////////////////////

	// Position represents a location within a document, which automatically
	// updates as the document is changed.  To implement Position for a FIFO,
	// a 64 bit long is used to track the total number of characters ever
	// added to the FIFO.  If the 480 Mbit/sec USB data rate of 50 Mbyte/sec
	// is sustained, 63 unsigned bits will take 5845 years to overflow.
	// Positions are created by simply recording the 64 bit absolute position
	// within the entire history of the data stream.

	public synchronized Position createPosition(int offset) throws BadLocationException {
		println("Document: createPosition");
		if (offset < 0) throw new BadLocationException("negative input not allowed", 0);
		if (offset == 0 || char_buf == null) return startPosition;
		int length = getCharCount();
		if (offset > length) throw new BadLocationException("beyond end", offset);
		long pos = char_total - length + offset;
		println("  offset=" + offset + " -> pos=" + pos);
		return new FifoPosition(this, char_total - length + offset);
	}
	public Position getStartPosition() {
		println("Document: getStartPosition");
		return startPosition;
	}
	public Position getEndPosition() {
		println("Document: getEndPosition");
		return endPosition;
	}
	public synchronized int positionToOffset(FifoPosition p) {
		long location = p.getPositionNumber();
		int length = getCharCount();
		long first_location = char_total - length;
		if (location <= first_location) return 0;
		if (location >= char_total) return length - 1;
		return (int)(location - first_location);
	}
	public synchronized String getText(int offset, int length) throws BadLocationException {
		println("Document: getText (String), offset=" + offset + ", len=" + length);
		if (length < 0 || offset < 0) {
			throw new BadLocationException("negative input not allowed", 0);
		}
		int chartotallen = char_head - char_tail;
		if (chartotallen < 0) chartotallen += char_size;
		if (offset + length > chartotallen) {
			throw new BadLocationException("access beyond data", offset + length);
		}

		String s;
		int index = offset + char_tail + 1;
		if (index >= char_size) index -= char_size;
		if (index + length < char_size) {
			s = new String(char_buf, index, length);
		} else {
			int remain = char_size - index;
			String s1 = new String(char_buf, index, remain);
			String s2 = new String(char_buf, 0, length - remain);
			s = s1 + s2;
		}
		println("  text=" + s);
		return s;
	}

////////////////////////////////////////////////////////
// Document Interface - boring boilerplate stuff
////////////////////////////////////////////////////////

	public void addDocumentListener(DocumentListener listener) {
		println("Document: addDocumentListener " + listener);
		listeners.add(listener);
	}
	public void removeDocumentListener(DocumentListener listener) {
		println("Document: removeDocumentListener");
		listeners.remove(listener);
	}
	public void addUndoableEditListener(UndoableEditListener listener) {
		println("Document: addUndoableEditListener " + listener);
	}
	public void removeUndoableEditListener(UndoableEditListener listener) {
		println("Document: removeUndoableEditListener");
	}
	public Object getProperty(Object key) {
		println("Document: getProperty " + key + "   " + key.getClass().getName());
		// TODO: does this stuff matter?
		//if (key.toString().equals("i18n")) return false;
		//if (key.toString().equals("tabSize")) return 4;
		return null;
	}
	public void putProperty(Object key, Object value) {
		println("Document: putProperty " + key + "  " + value);
	}
	public Element[] getRootElements() {
		println("Document: getRootElements");
		return null;
	}
	public Element getDefaultRootElement() {
		println("Document: getDefaultRootElement");
		return line_root;
	}
	public void render(Runnable r) {
		println("Document: Runnable");
	}

////////////////////////////////////////////////////////
// Debug printing - (almost) everything goes through here
////////////////////////////////////////////////////////

	private void actual_print(String str) {
		//System.out.print(str); // comment this line to suppress debug printing
	}

	private long prior_milliseconds = 0;

	public void print(String str) {
		long now = System.currentTimeMillis();
		if (now - prior_milliseconds > 100) actual_print("\n\n");
		prior_milliseconds = now;
		actual_print(str);
	}
	public void println(String str) {
		print(str + "\n");
	}
}

