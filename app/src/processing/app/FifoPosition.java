package processing.app;

import javax.swing.text.Position;

public class FifoPosition implements Position
{
	private final FifoDocument doc;
	private final long pos;

	public FifoPosition(FifoDocument document, long position) {
		doc = document;
		pos = position;
	}
	public int getOffset() {
		int offset = doc.positionToOffset(this);
		doc.println("Position: getOffset, pos=" + pos + " -> offset=" + offset);
		return offset;
	}
	public long getPositionNumber() {
		return pos;
	}
}
