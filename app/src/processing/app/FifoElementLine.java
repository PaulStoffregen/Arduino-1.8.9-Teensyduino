package processing.app;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.AttributeSet;

// https://www.comp.nus.edu.sg/~cs3283/ftp/Java/swingConnect/text/element_interface/element_interface.html

public class FifoElementLine implements Element
{
	public final Document doc;
	public final int index;
	public int head;
	public int len;

	public FifoElementLine(Document d, int i) {
		doc = d;
		index = i;
		head = 0;
		len = 0;
	}

	public void set(int new_head, int new_len) {
		head = new_head;
		len = new_len;
	}

	public Document getDocument() {
		System.out.println("ElementLine[" + index + "].getDocument");
		return doc;
	}
	public Element getParentElement() {
		System.out.println("ElementLine[" + index + "]: getParentElement");
		return null;
	}
	public String getName() {
		//System.out.println("ElementLine[" + index + "]: getName");
		return "leaf[" + index + "]";
	}
	public AttributeSet getAttributes() {
		System.out.println("ElementLine[" + index + "]: getAttributes");
		return null;
	}
	public int getStartOffset() {
		System.out.println("ElementLine[" + index + "]: getStartOffset -> " + head);
		return head;
	}
	public int getEndOffset() {
		int end = head + len - 1;
		System.out.println("ElementLine[" + index + "]: getEndOffset -> " + end);
		return end;
	}
	public int getElementIndex(int offset) {
		System.out.println("ElementLine[" + index + "]: getElementIndex, offset=" + offset);
		return -1;
	}
	public int getElementCount() {
		System.out.println("ElementLine[" + index + "]: getElementCount");
		return 0;
	}
	public Element getElement(int index) {
		System.out.println("ElementLine[" + index + "]: getElement, index=" + index);
		return null;
	}
	public boolean isLeaf() {
		System.out.println("ElementLine[" + index + "]: isLeaf");
		return true;
	}
}

 
