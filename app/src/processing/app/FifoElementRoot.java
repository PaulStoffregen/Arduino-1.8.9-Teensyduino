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

	// Called by JTextArea to find lines

	public int getElementCount() {
		int num = doc.getElementCount();
		doc.println("ElementRoot: getElementCount -> " + num);
		return num;
	}
	public Element getElement(int offset) {
		doc.println("ElementRoot: getElement, offset=" + offset);
		return doc.getElement(offset);
	}
	public Document getDocument() {
		//doc.println("ElementRoot.getDocument");
		return doc;
	}
	public int getElementIndex(int offset) {
		int i = doc.getElementIndex(offset);
		doc.println("ElementRoot: getElementIndex, offset=" + offset + " -> index=" + i);
		return i;
	}

	// Never used (for root Element)

	public Element getParentElement() {
		doc.println("ElementRoot: getParentElement");
		return null;
	}
	public String getName() {
		//doc.println("ElementRoot: getName");
		return "root";
	}
	public AttributeSet getAttributes() {
		doc.println("ElementRoot: getAttributes");
		return null;
	}
	public int getStartOffset() {
		doc.println("ElementRoot: getStartOffset");
		return 0;
	}
	public int getEndOffset() {
		doc.println("ElementRoot: getEndOffset");
		int len = doc.getCharCount();
		//if (len <= 0) return 0;
		return len;
	}
	public boolean isLeaf() {
		doc.println("ElementRoot: isLeaf");
		return false;
	}
}

 
