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
		return doc.positionToOffset(this);
	}
	public long getPositionNumber() {
		return pos;
	}
}
