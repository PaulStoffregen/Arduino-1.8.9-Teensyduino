package processing.app;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.event.DocumentEvent;

public class FifoEvent implements DocumentEvent, DocumentEvent.ElementChange
{
	final public FifoDocument doc;
	final public DocumentEvent.EventType type;
	int char_offset = 0;
	int char_length = 0;
	public int line_offset = 0;
	public int line_length = 0;

	public FifoEvent(FifoDocument d, DocumentEvent.EventType t) {
		doc = d;
		type = t;
	}

// DocumentEvent interface
	public void setCharOffsetLength(int offset, int length) {
		char_offset = offset;
		char_length = length;
	}
	public void setLineOffsetLength(int offset, int length) {
		line_offset = offset;
		line_length = length;
	}
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
		doc.println("Event: getChange (Element:" + elem.getName() + ")");
		// TODO: is this ever called for leaf Elements?
		return this;
	}

// DocumentEvent.ElementChange interface

	public Element getElement() {
		doc.println("EventChange: getElement");
		if (type != DocumentEvent.EventType.CHANGE) return null;
		return doc.getElement(line_offset);
	}
	public int getIndex() {
		doc.println("EventChange: getIndex");
		return line_offset;
	}
	public Element[] getChildrenRemoved() {
		doc.print("EventChange: getChildrenRemoved -> ");
		if (type != DocumentEvent.EventType.REMOVE) {
			doc.println("null");
			return null;
		}
		FifoElementLine[] array = doc.getElementArray(line_offset, line_length);
		doc.print("[ ");
		for (FifoElementLine e : array) {
			doc.print(e.index + " ");
		}
		doc.println("]");
		return array;
	}
	public Element[] getChildrenAdded() {
		doc.print("EventChange: getChildrenAdded -> ");
		if (type != DocumentEvent.EventType.INSERT) {
			doc.println("null");
			return null;
		}
		FifoElementLine[] array = doc.getElementArray(line_offset, line_length);
		doc.print("[ ");
		for (FifoElementLine e : array) {
			doc.print(e.index + " ");
		}
		doc.println("]");
		return array;
	}

}
