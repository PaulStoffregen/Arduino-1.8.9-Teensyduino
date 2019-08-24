package processing.app;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.event.DocumentEvent;

// Each time the FifoDocument changes, a FifoEvent instance is given
// all registered listeners.  FifoDocument creates 2 instances of this
// class, one to notify listeners of insertions (which occur as the
// Arduino Serial Monitor appends data to JTextArea) and the other
// to notify of removal (which usually occurs while scrolling, where
// old data is automatically deleted).

// Even through you might reasonably expect JTextArea would "know"
// how FifoDocument has changed, since it calls FifoDocument's
// insertString() and remove(), the system is designed to send
// changes, then later respond to these DocumentEvent notifications.
// This event response is the key ingredient which allows FifoDocument
// to automatically discard data and have JTextArea properly update,
// even though it did not call any function to initiate data deletion.

public class FifoEvent implements DocumentEvent, DocumentEvent.ElementChange
{
	private final FifoDocument doc;
	private final DocumentEvent.EventType type;
	private int char_offset = 0;
	private int char_length = 0;
	private int line_offset = 0;
	private int line_length = 0;
	private Element appended = null;

	public FifoEvent(FifoDocument d, DocumentEvent.EventType t) {
		doc = d;
		type = t;
	}

	public void setCharRange(int offset, int length) {
		char_offset = offset;
		char_length = length;
	}
	public void setLineRange(int offset, int length) {
		line_offset = offset;
		line_length = length;
	}
	public void setAppended(Element e) {
		appended = e;
	}

// DocumentEvent interface

	public int getOffset() {
		doc.println("Event: getOffset -> " + char_offset);
		return char_offset;
	}
	public int getLength() {
		doc.println("Event: getLength -> " + char_length);
		return char_length;
	}
	public Document getDocument() {
		doc.println("Event: getDocument");
		return doc;
	}
	public DocumentEvent.EventType getType() {
		doc.println("Event: getType -> " + type);
		return type;
	}
	public DocumentEvent.ElementChange getChange(Element elem) {
		//doc.println("Event: getChange (Element:" + elem.getName() + ")");
		// TODO: is this ever called for leaf Elements?
		return this;
	}

// DocumentEvent.ElementChange interface

	public Element getElement() {
		doc.println("EventChange: getElement");
		return appended;
	}
	public int getIndex() {
		doc.println("EventChange: getIndex");
		return line_offset;
	}
	public Element[] getChildrenRemoved() {
		//doc.print("EventChange: getChildrenRemoved -> ");
		if (type != DocumentEvent.EventType.REMOVE) {
			//doc.println("null");
			return null;
		}
		FifoElementLine[] array = doc.getElementArray(line_offset, line_length);
		//doc.print("[ ");
		//for (FifoElementLine e : array) {
			//doc.print(e.getIndex() + " ");
		//}
		//doc.println("]");
		return array;
	}
	public Element[] getChildrenAdded() {
		//doc.print("EventChange: getChildrenAdded -> ");
		if (type != DocumentEvent.EventType.INSERT) {
			//doc.println("null");
			return null;
		}
		FifoElementLine[] array = doc.getElementArray(line_offset, line_length);
		//doc.print("[ ");
		//for (FifoElementLine e : array) {
			//doc.print(e.getName() + " ");
		//}
		//doc.println("]");
		return array;
	}

}
