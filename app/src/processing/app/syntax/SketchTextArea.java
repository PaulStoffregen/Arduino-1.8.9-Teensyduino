/*
 * This file is part of Arduino.
 *
 * Copyright 2015 Ricardo JL Rufino (ricardo@criativasoft.com.br)
 * Copyright 2015 Arduino LLC
 *
 * Arduino is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * As a special exception, you may use this file as part of a free software
 * library without restriction.  Specifically, if other files instantiate
 * templates or use macros or inline functions from this file, or you compile
 * this file and link it with other files to produce an executable, this
 * file does not by itself cause the resulting executable to be covered by
 * the GNU General Public License.  This exception does not however
 * invalidate any other reasons why the executable file might be covered by
 * the GNU General Public License.
 */

package processing.app.syntax;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.KeyStroke;
import javax.swing.event.EventListenerList;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;

import org.apache.commons.compress.utils.IOUtils;
import org.fife.ui.rsyntaxtextarea.LinkGenerator;
import org.fife.ui.rsyntaxtextarea.LinkGeneratorResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenImpl;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextAreaUI;

import processing.app.Base;
import processing.app.PreferencesData;
import processing.app.helpers.OSUtils;

/**
 * Arduino Sketch code editor based on RSyntaxTextArea (http://fifesoft.com/rsyntaxtextarea)
 *
 * @author Ricardo JL Rufino (ricardo@criativasoft.com.br)
 * @since 1.6.4
 */
public class SketchTextArea extends RSyntaxTextArea {

  private final static Logger LOG = Logger.getLogger(SketchTextArea.class.getName());

  private PdeKeywords pdeKeywords;

  public SketchTextArea(RSyntaxDocument document, PdeKeywords pdeKeywords) throws IOException {
    super(document);
    this.pdeKeywords = pdeKeywords;
    installFeatures();
    fixCtrlDeleteBehavior();
  }

  public void setKeywords(PdeKeywords keywords) {
    pdeKeywords = keywords;
    setLinkGenerator(new DocLinkGenerator(pdeKeywords));
  }

  private void installFeatures() throws IOException {
    setTheme(PreferencesData.get("editor.syntax_theme", "default"));

    setLinkGenerator(new DocLinkGenerator(pdeKeywords));

    setSyntaxEditingStyle(SYNTAX_STYLE_CPLUSPLUS);
  }

  private void setTheme(String name) throws IOException {
    InputStream defaultXmlInputStream = null;
    try {
      defaultXmlInputStream = processing.app.Theme.getThemeResource("theme/syntax/" + name + ".xml").getInputStream();
      Theme theme = Theme.load(defaultXmlInputStream);
      theme.apply(this);
    } finally {
      IOUtils.closeQuietly(defaultXmlInputStream);
    }

    setEOLMarkersVisible(processing.app.Theme.getBoolean("editor.eolmarkers"));
    setBackground(processing.app.Theme.getColor("editor.bgcolor"));
    setHighlightCurrentLine(processing.app.Theme.getBoolean("editor.linehighlight"));
    setCurrentLineHighlightColor(processing.app.Theme.getColor("editor.linehighlight.color"));
    setCaretColor(processing.app.Theme.getColor("editor.caret.color"));
    setSelectedTextColor(null);
    setUseSelectedTextColor(false);
    setSelectionColor(processing.app.Theme.getColor("editor.selection.color"));
    setMatchedBracketBorderColor(processing.app.Theme.getColor("editor.brackethighlight.color"));
    setHyperlinkForeground((Color) processing.app.Theme.getStyledFont("url", getFont()).get("color"));

    setSyntaxTheme(TokenTypes.DATA_TYPE, "data_type");
    setSyntaxTheme(TokenTypes.FUNCTION, "function");
    setSyntaxTheme(TokenTypes.RESERVED_WORD, "reserved_word");
    setSyntaxTheme(TokenTypes.RESERVED_WORD_2, "reserved_word_2");
    setSyntaxTheme(TokenTypes.VARIABLE, "variable");
    setSyntaxTheme(TokenTypes.OPERATOR, "operator");
    setSyntaxTheme(TokenTypes.COMMENT_DOCUMENTATION, "comment1");
    setSyntaxTheme(TokenTypes.COMMENT_EOL, "comment1");
    setSyntaxTheme(TokenTypes.COMMENT_KEYWORD, "comment1");
    setSyntaxTheme(TokenTypes.COMMENT_MARKUP, "comment1");
    setSyntaxTheme(TokenTypes.COMMENT_MULTILINE, "comment2");
    setSyntaxTheme(TokenTypes.LITERAL_BOOLEAN, "literal_boolean");
    setSyntaxTheme(TokenTypes.LITERAL_CHAR, "literal_char");
    setSyntaxTheme(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, "literal_string_double_quote");
    setSyntaxTheme(TokenTypes.PREPROCESSOR, "preprocessor");

    setColorForToken(TokenTypes.IDENTIFIER, "editor.fgcolor");
    setColorForToken(TokenTypes.WHITESPACE, "editor.eolmarkers.color");
  }

  private void setColorForToken(int tokenType, String colorKeyFromTheme) {
    Style style = getSyntaxScheme().getStyle(tokenType);
    style.foreground = processing.app.Theme.getColor(colorKeyFromTheme);
    getSyntaxScheme().setStyle(tokenType, style);
  }

  private void setSyntaxTheme(int tokenType, String id) {
    Style style = getSyntaxScheme().getStyle(tokenType);

    Map<String, Object> styledFont = processing.app.Theme.getStyledFont(id, style.font);
    style.foreground = (Color) styledFont.get("color");
    style.font = (Font) styledFont.get("font");

    getSyntaxScheme().setStyle(tokenType, style);
  }

  public boolean isSelectionActive() {
    return this.getSelectedText() != null;
  }

  @Override
  protected RTAMouseListener createMouseListener() {
    return new SketchTextAreaMouseListener(this);
  }

  public void getTextLine(int line, Segment segment) {
    try {
      int offset = getLineStartOffset(line);
      int end = getLineEndOffset(line);
      getDocument().getText(offset, end - offset, segment);
    } catch (BadLocationException ignored) {
    }
  }

  private static class DocLinkGenerator implements LinkGenerator {

    private final PdeKeywords pdeKeywords;

    public DocLinkGenerator(PdeKeywords pdeKeywords) {
      this.pdeKeywords = pdeKeywords;
    }

    @Override
    public LinkGeneratorResult isLinkAtOffset(RSyntaxTextArea textArea, final int offs) {
      Token token = textArea.modelToToken(offs);
      if (token == null) {
        return null;
      }

      String reference = pdeKeywords.getReference(token.getLexeme());

      if (reference != null || (token.getType() == TokenTypes.DATA_TYPE || token.getType() == TokenTypes.VARIABLE || token.getType() == TokenTypes.FUNCTION)) {

        return new LinkGeneratorResult() {

          @Override
          public int getSourceOffset() {
            return offs;
          }

          @Override
          public HyperlinkEvent execute() {

            LOG.fine("Open Reference: " + reference);

            Base.showReference("Reference/" + reference);

            return null;
          }
        };
      }

      return null;
    }
  }


  /**
   * Handles http hyperlinks.
   * NOTE (@Ricardo JL Rufino): Workaround to enable hyperlinks by default: https://github.com/bobbylight/RSyntaxTextArea/issues/119
   */
  private class SketchTextAreaMouseListener extends RTextAreaMutableCaretEvent {

    private Insets insets;
    private boolean isScanningForLinks;
    private int hoveredOverLinkOffset = -1;

    SketchTextAreaMouseListener(RTextArea textArea) {
      super(textArea);
      insets = new Insets(0, 0, 0, 0);
    }

    /**
     * Notifies all listeners that have registered interest for notification
     * on this event type.  The listener list is processed last to first.
     *
     * @param e The event to fire.
     * @see EventListenerList
     */
    private void fireHyperlinkUpdate(HyperlinkEvent e) {
      // Guaranteed to return a non-null array
      Object[] listeners = listenerList.getListenerList();
      // Process the listeners last to first, notifying
      // those that are interested in this event
      for (int i = listeners.length - 2; i >= 0; i -= 2) {
        if (listeners[i] == HyperlinkListener.class) {
          ((HyperlinkListener) listeners[i + 1]).hyperlinkUpdate(e);
        }
      }
    }

    private HyperlinkEvent createHyperlinkEvent(MouseEvent e) {
      HyperlinkEvent he = null;

      Token t = viewToToken(e.getPoint());
      if (t != null) {
        // Copy token, viewToModel() unfortunately modifies Token
        t = new TokenImpl(t);
      }

      if (t != null && t.isHyperlink()) {
        URL url = null;
        String desc = null;
        try {
          String temp = t.getLexeme();
          // URI's need "http://" prefix for web URL's to work.
          if (temp.startsWith("www.")) {
            temp = "http://" + temp;
          }
          url = new URL(temp);
        } catch (MalformedURLException mue) {
          desc = mue.getMessage();
        }
        he = new HyperlinkEvent(SketchTextArea.this, HyperlinkEvent.EventType.ACTIVATED, url, desc);
      }

      return he;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (getHyperlinksEnabled()) {
        HyperlinkEvent he = createHyperlinkEvent(e);
        if (he != null) {
          fireHyperlinkUpdate(he);
        }
      }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

      super.mouseMoved(e);

      if (!getHyperlinksEnabled()) {
        return;
      }

//      LinkGenerator linkGenerator = getLinkGenerator();

      // GitHub issue RSyntaxTextArea/#25 - links identified at "edges" of editor
      // should not be activated if mouse is in margin insets.
      insets = getInsets(insets);
      if (insets != null) {
        int x = e.getX();
        int y = e.getY();
        if (x <= insets.left || y < insets.top) {
          if (isScanningForLinks) {
            stopScanningForLinks();
          }
          return;
        }
      }

      isScanningForLinks = true;
      Token t = viewToToken(e.getPoint());
      if (t != null) {
        // Copy token, viewToModel() unfortunately modifies Token
        t = new TokenImpl(t);
      }
      Cursor c2;
      if (t != null && t.isHyperlink()) {
        if (hoveredOverLinkOffset == -1 ||
          hoveredOverLinkOffset != t.getOffset()) {
          hoveredOverLinkOffset = t.getOffset();
          repaint();
        }
        c2 = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
      }
//      else if (t!=null && linkGenerator!=null) {
//        int offs = viewToModel(e.getPoint());
//        LinkGeneratorResult newResult = linkGenerator.
//            isLinkAtOffset(SketchTextArea.this, offs);
//        if (newResult!=null) {
//          // Repaint if we're at a new link now.
//          if (linkGeneratorResult==null ||
//              !equal(newResult, linkGeneratorResult)) {
//            repaint();
//          }
//          linkGeneratorResult = newResult;
//          hoveredOverLinkOffset = t.getOffset();
//          c2 = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
//        }
//        else {
//          // Repaint if we've moved off of a link.
//          if (linkGeneratorResult!=null) {
//            repaint();
//          }
//          c2 = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
//          hoveredOverLinkOffset = -1;
//          linkGeneratorResult = null;
//        }
//      }
      else {
        c2 = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        hoveredOverLinkOffset = -1;
        //  linkGeneratorResult = null;
      }
      if (getCursor() != c2) {
        setCursor(c2);
        // TODO: Repaint just the affected line(s).
        repaint(); // Link either left or went into.
      }
    }

    private void stopScanningForLinks() {
      if (isScanningForLinks) {
        Cursor c = getCursor();
        isScanningForLinks = false;
        if (c.getType() == Cursor.HAND_CURSOR) {
          setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
          repaint(); // TODO: Repaint just the affected line.
        }
      }
    }

  }

  @Override
  protected RTextAreaUI createRTextAreaUI() {
    return new SketchTextAreaUI(this);
  }

  private void fixCtrlDeleteBehavior() {
    int modifier = OSUtils.isMacOS()? InputEvent.ALT_MASK : InputEvent.CTRL_MASK;
    KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, modifier);
    getInputMap().put(keyStroke, SketchTextAreaEditorKit.rtaDeleteNextWordAction);
  }
}
