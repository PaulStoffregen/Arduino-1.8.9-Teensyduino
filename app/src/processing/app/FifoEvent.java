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
		System.out.println("Event: getOffset -> " + char_offset);
		return char_offset;
	}
	public int getLength() {
		System.out.println("Event: getLength -> " + char_length);
		return char_length;
	}
	public Document getDocument() {
		System.out.println("Event: getDocument");
		return doc;
	}
	public DocumentEvent.EventType getType() {
		System.out.println("Event: getType -> " + type);
		return type;
	}
	public DocumentEvent.ElementChange getChange(Element elem) {
		System.out.println("Event: getChange (Element:" + elem.getName() + ")");
		// TODO: is this ever called for leaf Elements?
		return this;
	}

// DocumentEvent.ElementChange interface

	public Element getElement() {
		System.out.println("EventChange: getElement");
		if (type != DocumentEvent.EventType.CHANGE) return null;
		return doc.getElement(line_offset);
	}
	public int getIndex() {
		System.out.println("EventChange: getIndex");
		return line_offset;
	}
	public Element[] getChildrenRemoved() {
		System.out.print("EventChange: getChildrenRemoved -> ");
		if (type != DocumentEvent.EventType.REMOVE) {
			System.out.println("null");
			return null;
		}
		FifoElementLine[] array = doc.getElementArray(line_offset, line_length);
		System.out.print("[ ");
		for (FifoElementLine e : array) {
			System.out.print(e.index + " ");
		}
		System.out.println("]");
		return array;
	}
	public Element[] getChildrenAdded() {
		System.out.print("EventChange: getChildrenAdded -> ");
		if (type != DocumentEvent.EventType.INSERT) {
			System.out.println("null");
			return null;
		}
		FifoElementLine[] array = doc.getElementArray(line_offset, line_length);
		System.out.print("[ ");
		for (FifoElementLine e : array) {
			System.out.print(e.index + " ");
		}
		System.out.println("]");
		return array;
	}

}
