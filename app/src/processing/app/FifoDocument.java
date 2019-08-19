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

DocumentFIFO replaces the normal Document with a very limited Document
optimized for the Arduino Serial Monitor.

Text insertion is only allowed to append to the end of the document.
Any text removal deletes the entire document.  Text attributes are
ignored.  UndoableEditListener is not supported.  Only the minimum
Document features needed by the Arduino Serial Monitor are implemented.

DocumentFIFO uses a fixed size buffer.  Data is automatically
discarded by DocumentFIFO as the Arduino Serial Monitor keeps adding
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
	private char[] char_buf;

	FifoElementRoot line_root;
	private int line_head;
	private int line_tail;
	final private int line_size;
	private FifoElementLine[] line_buf;
	private boolean last_line_complete;

	private List<DocumentListener> listeners;
	private boolean scrolling = true;
	FifoEvent insertEvent;
	FifoEvent removeEvent;

	public FifoDocument(int size) {
		// Allocate the big buffer to hold raw character data
		char_size = size;
		char_head = 0;
		char_tail = 0;
		char_buf = new char[char_size];

		// Allocate the list of Elements to represent the lines
		line_root = new FifoElementRoot(this);
		line_size = size / 10;
		line_head = 0;
		line_tail = 0;
		line_buf = new FifoElementLine[line_size];
		for (int i=0; i < line_size; i++) {
			line_buf[i] = new FifoElementLine(this, i);
		}
		last_line_complete = false;

		// Allocate other misc stuff
		listeners = new ArrayList<DocumentListener>();
		insertEvent = new FifoEvent(this, DocumentEvent.EventType.INSERT);
		removeEvent = new FifoEvent(this, DocumentEvent.EventType.REMOVE);
	}

////////////////////////////////////////////////////////
// Document Interface - interesting functions
////////////////////////////////////////////////////////

	public void insertString(int offs, String s, AttributeSet a) throws BadLocationException {
		int len = s.length();
		System.out.println("Document: **INSERT** insertString, offset=" + offs + ", len=" + len);

		if (len <= 0) return;

		int insert_offset = char_head;
		int line_offset = line_head;

		/*int newline_count = 0;
		int newline_offset = 0;
		while (true) {
			newline_offset = indexOf('\n', newline_offset);
			if (newline_offset < 0) break;
			newline_count++;
		}*/


		// simple test, store each incoming write to a single element
		s.getChars(0, len, char_buf, char_head);
		line_buf[line_head].set(char_head, len);
		line_head += 1;
		char_head += len;

		for (DocumentListener d : listeners) {
			insertEvent.setCharOffsetLength(insert_offset, len);
			insertEvent.setLineOffsetLength(line_offset, 1);
			d.insertUpdate(insertEvent);
		}
	}
	public void remove(int offset, int len) throws BadLocationException {
		System.out.println("Document: remove, offset=" + offset + ", len=" + len);
	}

	public void getText(int offset, int length, Segment txt) throws BadLocationException {
		System.out.println("Document: getText, offset=" + offset + ", len=" + length);
		if (length < 0 || offset < 0) {
			throw new BadLocationException("negative input not allowed", 0);
		}
		if (offset + length > char_head) {
			throw new BadLocationException("access beyond data", offset + length);
		}
		txt.array = char_buf;
		txt.offset = offset;
		txt.count = length;
		txt.setPartialReturn(false);
		System.out.println("  text=" + txt.toString());
		System.out.println();
	}
	public int getLength() {
		System.out.println("Document: getLength -> " + char_head);
		return char_head;
	}

////////////////////////////////////////////////////////
// Element support functions
////////////////////////////////////////////////////////

	public int getElementCount() {
		return line_head;
	}
	public FifoElementLine getElement(int index) {
		return line_buf[index];
	}
	public int getElementIndex(int offset) {
		int num = line_head;
		if (num < 1) return 0;
		for (int i=0; i < num; i++) { // TODO: binary search
			int begin = line_buf[i].head;
			int end = line_buf[i].head + line_buf[i].len - 1;
			if (offset >= begin && offset <= end)  return i;
		}
		return num - 1;
	}
	public FifoElementLine[] getElementArray(int offset, int length) {
		// TODO: should we pre-allocate and reuse this array to avoid garbage collection?
		FifoElementLine[] list = new FifoElementLine[length];
		for (int i=0; i < length; i++) {
			list[i] = line_buf[offset + i];
		}
		return list;
	}

////////////////////////////////////////////////////////
// Document Interface - boring boilerplate stuff
////////////////////////////////////////////////////////

	public void addDocumentListener(DocumentListener listener) {
		System.out.println("Document: addDocumentListener " + listener);
		listeners.add(listener);
	}
	public void removeDocumentListener(DocumentListener listener) {
		System.out.println("Document: removeDocumentListener");
		listeners.remove(listener);
	}
	public String getText(int offset, int length) throws BadLocationException {
		System.out.println("Document: getText (String)");
		return "";
	}
	public void addUndoableEditListener(UndoableEditListener listener) {
		System.out.println("Document: addUndoableEditListener " + listener);
	}
	public void removeUndoableEditListener(UndoableEditListener listener) {
		System.out.println("Document: removeUndoableEditListener");
	}
	public Object getProperty(Object key) {
		System.out.println("Document: getProperty " + key + "   " + key.getClass().getName());
		if (key.toString().equals("i18n")) return false;
		if (key.toString().equals("tabSize")) return 4;

		return null;
		//return false; // "i18n"
	}
	public void putProperty(Object key, Object value) {
		System.out.println("Document: putProperty " + key + "  " + value);
	}
	public Position getStartPosition() {
		System.out.println("Document: getStartPosition");
		return null;
	}
	public Position getEndPosition() {
		System.out.println("Document: getEndPosition");
		return null;
	}
	public Position createPosition(int offs) throws BadLocationException {
		System.out.println("Document: createPosition");
		return null;
	}
	public Element[] getRootElements() {
		System.out.println("Document: getRootElements");
		return null;
	}
	public Element getDefaultRootElement() {
		System.out.println("Document: getDefaultRootElement");
		return line_root;
	}
	public void render(Runnable r) {
		System.out.println("Document: Runnable");
	}


}

