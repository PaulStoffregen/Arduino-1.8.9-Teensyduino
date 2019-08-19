package processing.app;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.AttributeSet;

// https://www.comp.nus.edu.sg/~cs3283/ftp/Java/swingConnect/text/element_interface/element_interface.html

public class FifoElementRoot implements Element
{
	final FifoDocument doc;

	public FifoElementRoot(FifoDocument d) {
		doc = d;
	}

	public Document getDocument() {
		System.out.println("ElementRoot.getDocument");
		return doc;
	}
	public Element getParentElement() {
		System.out.println("ElementRoot: getParentElement");
		return null;
	}
	public String getName() {
		//System.out.println("ElementRoot: getName");
		return "root";
	}
	public AttributeSet getAttributes() {
		System.out.println("ElementRoot: getAttributes");
		return null;
	}
	public int getStartOffset() {
		System.out.println("ElementRoot: getStartOffset");
		return 0;
	}
	public int getEndOffset() {
		System.out.println("ElementRoot: getEndOffset");
		return 0;
	}
	public int getElementIndex(int offset) {
		int index = doc.getElementIndex(offset);
		System.out.println("ElementRoot: getElementIndex, offset=" + offset + " -> index=" + index);
		return doc.getElementIndex(offset);
	}

	public int getElementCount() {
		int num = doc.getElementCount();
		System.out.println("ElementRoot: getElementCount -> " + num);
		return num;
	}
	public Element getElement(int index) {
		System.out.println("ElementRoot: getElement, index=" + index);
		return doc.getElement(index);
	}
	public boolean isLeaf() {
		System.out.println("ElementRoot: isLeaf");
		return false;
	}
}

 
