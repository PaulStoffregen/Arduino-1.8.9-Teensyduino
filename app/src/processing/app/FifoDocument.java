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

/*
JTextArea stores its text in a Document object.  The default Document
implementation must handle insertion of text at arbitrary locations,
with custom attibutes (bold, italic, font, color, etc), and also
removal from arbitrary locations which may partially overlap any
number of previously inserted pieces of text with differing
attributes.  To manage this storage, a Document provides a tree of
Elements which represent the structure of the entire Document.  While
powerful for handling arbitrary text documents, creating new Element
objects for each insertion consumes far too much memory and CPU time
when many millions of relatively small insertions are performed every
second.

FifoDocument replaces the normal Document with a very limited Document
optimized for the Arduino Serial Monitor.

Text insertion is only allowed to append to the end of the document.
Any text removal deletes the entire document.  Text attributes are
ignored.  UndoableEditListener is not supported.  Only the minimum
Document features needed by the Arduino Serial Monitor are implemented.

FifoDocument uses a fixed size buffer.  Data is automatically
discarded by FifoDocument as the Arduino Serial Monitor keeps adding
more incoming chars, according to 1 of 2 memory management stratagies.

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
CHANGE events are never used, because we do not support attributes.
*/

public class FifoDocument implements Document
{
	private int char_head;
	private int char_tail;
	final private int char_size;
	final private int char_threshold;
	private char[] char_buf;
	private long char_total;

	FifoElementRoot line_root;
	private int line_head;
	private int line_tail;
	final private int line_size;
	final private int line_threshold;
	private FifoElementLine[] line_buf;
	private boolean last_line_incomplete;

	private List<DocumentListener> listeners;
	private boolean scrolling = true;
	FifoEvent insertEvent;
	FifoEvent removeEvent;

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
		last_line_incomplete = false;

		// Allocate other misc stuff
		listeners = new ArrayList<DocumentListener>();
		insertEvent = new FifoEvent(this, DocumentEvent.EventType.INSERT);
		removeEvent = new FifoEvent(this, DocumentEvent.EventType.REMOVE);
	}

////////////////////////////////////////////////////////
// Document Interface - interesting functions
////////////////////////////////////////////////////////

	public void insertString(int offs, String s, AttributeSet a) throws BadLocationException {
		// TODO: check that offs == total data stored
		int char_len = s.length();
		if (char_len <= 0) return;
		// TODO: check if char_len >= char_threshold, what to do???
		println("Document: **INSERT** insertString, offset=" + offs + ", len=" + char_len);
		println("TEXT=\"" + s + "\"");

		// count how many lines this string will add
		int line_len = 0;
		int npos = 0;
		if (last_line_incomplete) {
			npos = s.indexOf('\n');
			if (npos >= 0) {
				npos++;
				println(" first " + npos + " chars add to last line");
			} else {
				println(" all " + char_len + " chars add to last line");
			}
		}
		if (npos >= 0) {
			while (true) {
				if (npos >= char_len) break;
				int next_pos = s.indexOf('\n', npos);
				line_len++;
				if (next_pos >= 0) {
					int len = next_pos - npos + 1;
					println("line " + line_len + " has " + len + " chars");
				} else {
					int len = char_len - npos;
					print("line " + line_len + " has " + len + " chars");
					println(" (leaves last line incomplete)");
				}
				if (next_pos < 0) break;
				npos = next_pos + 1;
			}
		}
		println(" adds " + line_len + " lines");
		// TODO: what to do if line_len >= line_threshold ???


		if (scrolling) {
			// if scrolling, delete old data as needed to stay under thresholds
			int char_count = getLength();
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
				println("delete step #2, line with " + len + " chars");
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
			// TODO: if still, truncate input as needed, recompute lines

		}


		// copy string contents to char_buf
		int chead = char_head + 1;
		if (chead >= char_size) chead = 0;
		if (chead + char_len <= char_size) {
			s.getChars(0, char_len, char_buf, chead);
		} else {
			int remain = char_size - chead;
			println("copy crossed end, chead = " + chead
				+ ", size = " + char_size + ", remain = " + remain);
			s.getChars(0, remain, char_buf, chead);
			s.getChars(remain, char_len, char_buf, 0);
		}
		char_head += char_len;
		if (char_head >= char_size) char_head -= char_size;
		char_total += char_len;

		// add text to last line
		if (last_line_incomplete) {
			npos = s.indexOf('\n');
			if (npos >= 0) {
				line_buf[line_head].len += ++npos;
			} else {
				line_buf[line_head].len += char_len;
			}
			insertEvent.setAppended(line_buf[line_head]);
		} else {
			insertEvent.setAppended(null);
			npos = 0;
		}

		// add lines to Element list
		int lhead = line_head + 1;
		if (lhead > line_size) lhead = 0;
		if (npos >= 0) {
			while (true) {
				if (npos >= char_len) {
					last_line_incomplete = false;
					break;
				}
				int next_pos = s.indexOf('\n', npos);
				if (next_pos < 0) {
					addLine(chead + npos, char_len - npos);
					last_line_incomplete = true;
					break;
				}
				addLine(chead + npos, next_pos - npos + 1);
				npos = next_pos + 1;
			}
		}

		// transmit insert event
		insertEvent.setCharRange(charIndexToOffset(chead), char_len);
		insertEvent.setLineRange(lineIndexToOffset(lhead), line_len);
		for (DocumentListener d : listeners) {
			d.insertUpdate(insertEvent);
		}
	}
	public void addLine(int index, int len) {
		if (index >= char_size) index -= char_size;
		line_head++;
		if (line_head >= line_size) line_head = 0;
		println(" addLine, " + len + " chars, into line index = " + line_head);
		line_buf[line_head].set(index, len);
	}

	public void remove(int offset, int len) throws BadLocationException {
		println("Document: remove, offset=" + offset + ", len=" + len);
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
		char_head = 0;
		char_tail = 0;
		line_head = 0;
		line_tail = 0;
		last_line_incomplete = false;
	}

	public void getText(int offset, int length, Segment txt) throws BadLocationException {
		println("Document: getText, offset=" + offset + ", len=" + length);
		if (length < 0 || offset < 0) {
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
	public int getLength() {
		int len = char_head - char_tail;
		if (len < 0) len += char_size;
		println("Document: getLength -> " + len);
		return len;
	}

////////////////////////////////////////////////////////
// Element support functions
////////////////////////////////////////////////////////

	public int getElementCount() {
		int len = line_head - line_tail;
		if (len < 0) len += line_size;
		return len;
	}
	public FifoElementLine getElement(int offset) {
		int index = offset + line_tail + 1;
		if (index >= line_size) index -= line_size;
		return line_buf[index];
	}
	public int getElementIndex(int offset) {
		int num = line_head;
		if (num < 1) return 0;
		for (int i=0; i < num; i++) { // TODO: binary search
			int begin = line_buf[i].index;
			int end = line_buf[i].index + line_buf[i].len - 1;
			if (offset >= begin && offset <= end)  return i;
		}
		return num - 1;
	}
	public FifoElementLine[] getElementArray(int offset, int length) {
		// TODO: should we pre-allocate and reuse this array to avoid garbage collection?
		FifoElementLine[] list = new FifoElementLine[length];
		int index = offset + line_tail + 1;
		if (index >= line_size) index -= line_size;
		for (int i=0; i < length; i++) {
			list[i] = line_buf[index++];
			if (index >= line_size) index -= line_size;
		}
		return list;
	}
	public int charIndexToOffset(int index) {
		int offset = index - char_tail - 1;
		if (offset < 0) offset += char_size;
		return offset;
	}
	public int lineIndexToOffset(int index) {
		int offset = index - line_tail - 1;
		if (offset < 0) offset += line_size;
		return offset;
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
	public String getText(int offset, int length) throws BadLocationException {
		println("Document: getText (String)");
		return "";
	}
	public void addUndoableEditListener(UndoableEditListener listener) {
		println("Document: addUndoableEditListener " + listener);
	}
	public void removeUndoableEditListener(UndoableEditListener listener) {
		println("Document: removeUndoableEditListener");
	}
	public Object getProperty(Object key) {
		println("Document: getProperty " + key + "   " + key.getClass().getName());
		if (key.toString().equals("i18n")) return false;
		if (key.toString().equals("tabSize")) return 4;

		return null;
		//return false; // "i18n"
	}
	public void putProperty(Object key, Object value) {
		println("Document: putProperty " + key + "  " + value);
	}
	public Position getStartPosition() {
		println("Document: getStartPosition");
		return null;
	}
	public Position getEndPosition() {
		println("Document: getEndPosition");
		return null;
	}
	public Position createPosition(int offs) throws BadLocationException {
		println("Document: createPosition");
		return null;
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
// Debug printing - everything goes through here
////////////////////////////////////////////////////////

	public void print(String str) {
		//System.out.print(str);
	}
	public void println(String str) {
		//System.out.println(str);
	}

}

