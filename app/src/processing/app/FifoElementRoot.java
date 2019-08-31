package processing.app;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.AttributeSet;

// Only a single instance of FifoElementRoot is created within FifoDocument.
// This root element represents the entire document.  JTextArea uses it to
// get the Line Elements when it knows the line number, or to search for the
// line number when it knows only a character offset.  Actual implementation
// is done within FifoDocument.

// https://www.comp.nus.edu.sg/~cs3283/ftp/Java/swingConnect/text/element_interface/element_interface.html

public class FifoElementRoot implements Element
{
	private final FifoDocument doc;

	public FifoElementRoot(FifoDocument d) {
		doc = d;
	}

	// Called by JTextArea to find lines

	public int getElementCount() {
		int num = doc.getElementCount();
		doc.println("Root: getElementCount -> " + num);
		return num;
	}
	public Element getElement(int offset) {
		doc.println("Root: getElement, offset=" + offset);
		return doc.getElement(offset);
	}
	public Document getDocument() {
		//doc.println("ElementRoot.getDocument");
		return doc;
	}
	public int getElementIndex(int char_offset) {
		int line_offset = doc.getLineOffset(char_offset);
		/* FifoElementLine line = doc.getElement(line_offset);
		doc.println("Root: getElementIndex, char offset = " + char_offset
			+ " -> line offset = " + line_offset
			+ " -> char offset = " + doc.charIndexToOffset(line.getIndex())
			+ ", len = " + line.getLength()); */
		return line_offset;
	}
	public int getStartOffset() {
		doc.println("Root: getStartOffset -> 0");
		return 0;
	}
	public int getEndOffset() {
		int len = doc.getCharCount();
		doc.println("Root: getEndOffset -> " + len);
		return len;
	}

	// Never used (for root Element)

	public Element getParentElement() {
		doc.println("Root: getParentElement");
		return null;
	}
	public String getName() {
		//doc.println("ElementRoot: getName");
		return "root";
	}
	public AttributeSet getAttributes() {
		doc.println("Root: getAttributes");
		return null;
	}
	public boolean isLeaf() {
		doc.println("Root: isLeaf");
		return false;
	}
}


