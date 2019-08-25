/*
  Copyright (c) 2014 Paul Stoffregen <paul@pjrc.com>

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

// Previously TextAreaFIFO was responsible for automatically trimming
// old buffered data.  Now FifoDocument is responsible for managing
// the FIFO.  But FifoDocument needs to know if the serial monitor
// AutoScroll button is active, so this TextAreaFIFO class still
// exists only to instantiate FifoDocument and pass it the state of
// the AutoScroll button.

public class TextAreaFIFO extends JTextArea {

  public final FifoDocument doc;

  public TextAreaFIFO(int max) {
    doc = new FifoDocument(max);
    setDocument(doc);
  }

  public void setScolling(boolean mode) {
    doc.setScrollingMode(mode);
  }
}
