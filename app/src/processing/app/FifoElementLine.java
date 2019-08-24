package processing.app;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.AttributeSet;


// A large fixed-size array of instances of this class are created within FifoDocument.
// JTextArea uses the Root Element to find the Line Element instances.  This class
// really serves no purpose other than allowing JTextArea to find the character offsets
// for the beginning and ending of each line.  It then uses those offsets to fetch the
// text from FifoDocument.

// https://www.comp.nus.edu.sg/~cs3283/ftp/Java/swingConnect/text/element_interface/element_interface.html

public class FifoElementLine implements Element
{
	private FifoDocument doc;
	private final int n;
	private int index;
	private int len;

	public FifoElementLine(FifoDocument d, int i) {
		doc = d;
		n = i;
		index = 0;
		len = 0;
	}

	// Called by JTextArea to access lines

	public int getStartOffset() {
		int offset = doc.charIndexToOffset(index);
		//doc.println("ElementLine[" + n + "]: getStartOffset -> " + offset);
		return offset;
	}
	public int getEndOffset() {
		int offset = doc.charIndexToOffset(index) + len;
		//doc.println("ElementLine[" + n + "]: getEndOffset -> " + offset);
		return offset;
	}
	public Document getDocument() {
		//doc.println("ElementLine[" + n + "].getDocument");
		return doc;
	}
	public boolean isLeaf() {
		//doc.println("ElementLine[" + n + "]: isLeaf");
		return true;
	}
	public AttributeSet getAttributes() {
		//doc.println("ElementLine[" + n + "]: getAttributes");
		return null;
	}

	// Called by FifoDocument

	public void set(int new_index, int new_len) {
		index = new_index;
		len = new_len;
	}
	public int getLength() {
		return len;
	}
	public int getIndex() {
		return index;
	}
	public void increaseLength(int adder) {
		len += adder;
	}

	// Never used (for leaf / line Elements)

	public Element getParentElement() {
		doc.println("ElementLine[" + n + "]: getParentElement");
		return null;
	}
	public String getName() {
		//doc.println("ElementLine[" + n + "]: getName");
		return "leaf[" + n + "]";
	}
	public int getElementIndex(int offset) {
		doc.println("ElementLine[" + n + "]: getElementIndex, offset=" + offset);
		return -1;
	}
	public int getElementCount() {
		doc.println("ElementLine[" + n + "]: getElementCount");
		return 0;
	}
	public Element getElement(int index) {
		doc.println("ElementLine[" + n + "]: getElement, index=" + index);
		return null;
	}
}

 
