/*
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

import cc.arduino.packages.BoardPort;
import processing.app.legacy.PApplet;
import processing.app.PreferencesData;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.UIManager;
import javax.swing.UIDefaults;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.*;
import static processing.app.I18n.tr;

public class TeensyPipeMonitor extends AbstractTextMonitor {

	//public final boolean debug = false;
	public final boolean debug = true;
	private String teensyname=null;
	private String openport=null;
	Process program=null;
	inputPipeListener listener=null;
	errorPipeListener errors=null;

	public TeensyPipeMonitor(BoardPort port) {
		super(port);
		textArea.setFifo(new FifoDocument(10000000));
		if (debug) System.out.println("TeensyPipeMonitor ctor, port=" + port.getAddress());
		String[] pieces = port.getLabel().trim().split("[\\(\\)]");
		if (pieces.length > 2 && pieces[1].startsWith("Teensy")) {
			teensyname = pieces[1];
		} else {
			teensyname = "Teensy";
		}
		serialRates.hide();
		disconnect();

		onClearCommand(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clear();
			}
		});
		onSendCommand(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String s = textField.getText();
				switch (lineEndings.getSelectedIndex()) {
				  case 1: s += "\n"; break;
				  case 2: s += "\r"; break;
				  case 3: s += "\r\n"; break;
				}
				byte[] b = s.getBytes(); // TODO: is this UTF8?
				if (program != null) {
					OutputStream out = program.getOutputStream();
					if (out != null) {
						try {
							out.write(b);
							out.flush();
							//System.out.println("wrote " + b.length);
						} catch (Exception e1) { }
					}
				}
				textField.setText("");
			}
		});
		//setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				window_close();
			}
		});
	}

	private void clear() {
		textArea.select(0, 0);
		textArea.setCaretPosition(0);
		textArea.setText("");
	}

	public void open() throws Exception {
		String port = getBoardPort().getAddress();
		if (debug) System.out.println("TeensyPipeMonitor open " + port);
		if (openport != null && port.equals(openport) && program != null
		   && listener != null && listener.isAlive()
		   && errors != null && errors.isAlive()) {
			// correct port is already open
			if (debug) System.out.println("TeensyPipeMonitor port already open");
			return;
		}
		if (program != null || listener != null || errors != null) {
			if (debug) System.out.println("TeensyPipeMonitor close old before open");
			close();
		}
		String[] cmdline;
		String command = BaseNoGui.getHardwarePath() + File.separator +
			"tools" + File.separator + "teensy_serialmon";
		//if (PreferencesData.getBoolean("upload.verbose")) {
			//cmdline = new String[3];
			//cmdline[0] = command;
			//cmdline[1] = "-v";
			//cmdline[2] = port;
		//} else {
			cmdline = new String[2];
			cmdline[0] = command;
			cmdline[1] = port;
		//}
		try {
			program = Runtime.getRuntime().exec(cmdline);
		} catch (Exception e1) {
			System.err.println("Unable to run teensy_serialmon");
			program = null;
		}
		if (program != null) {
			openport = new String(port);
			clear();
			listener = new inputPipeListener();
			listener.input = program.getInputStream();
			listener.output = this;
			listener.start();
			errors = new errorPipeListener();
			errors.input = program.getErrorStream();
			errors.output = this;
			errors.start();
			super.open();
		}
	}

	public void close() throws Exception {
		if (debug) System.out.println("TeensyPipeMonitor close");
		if (program != null) {
			program.destroy();
			program = null;
		}
		if (listener != null) {
			if (listener.isAlive()) listener.interrupt();
			listener = null;
		}
		if (errors != null) {
			if (errors.isAlive()) errors.interrupt();
			errors = null;
		}
		openport = null;
		setTitle("[offline] (" + teensyname + ")");
		super.close();
	}

	public void opened(String device, String usbtype) {
		if (debug) System.out.println("opened, dev=" + device + ", name=" + usbtype);
		clear();
		setTitle(device + " (" + teensyname + ") " + usbtype);
		// setting these to null for system default
		// gives a wrong gray background on Windows
		// so assume black text on white background
		textArea.setForeground(Color.BLACK);
		textArea.setBackground(Color.WHITE);
		enableWindow(true);
	}

	public void disconnect() {
		if (debug) System.out.println("disconnect");
		setTitle("[offline] (" + teensyname + ")");
		//UIDefaults ui = UIManager.getDefaults(); // more trouble than it's worth??
		Color fg=null, bg=null;
		//fg = ui.getColor("TextArea.inactiveForeground");
		//if (fg == null) fg = ui.getColor("TextField.inactiveForeground");
		//if (fg == null) fg = ui.getColor("TextPane.inactiveForeground");
		if (fg == null) fg = Color.BLACK;
		//bg = ui.getColor("TextArea.inactiveBackground");
		//if (bg == null) bg = ui.getColor("TextField.inactiveBackground");
		//if (bg == null) bg = ui.getColor("TextPane.inactiveBackground");
		if (bg == null) bg = new Color(238, 238, 238);
		enableWindow(false);
		//System.out.println("disabled foreground = " + fg);
		//System.out.println("disabled background = " + bg);
		textArea.setEnabled(true);  // enable so users can copy text to clipboard
		textArea.setForeground(fg);
		textArea.setBackground(bg); // but try to make it look sort-of disabled
		textArea.invalidate();
	}

	public void window_close() {
		if (debug) System.out.println("window_close");
		textArea.getFifo().free();
		dispose();
		((JFrame)SwingUtilities.getRoot(this)).dispose();
	}
}


class inputPipeListener extends Thread
{
	InputStream input;
	TeensyPipeMonitor output;
	FifoDocument doc;

	public void run() {
		setName("TeensyPipeMonitor inputPipeListener");
		doc = output.textArea.getFifo();
		final char[] buffer = doc.getBuffer();
		InputStreamReader reader = new InputStreamReader(input);
		//BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		try {
			while (output.program != null) {
				int offset = doc.getAppendIndex();
				int length = doc.getAvailableToAppend();
				if (length <= 0) {
					// fifo is full, so we must discard input
					if (reader.ready()) {
						long discard = reader.skip(1000000);
					} else {
						sleep(1);
					}
					if (output.autoscrollBox.isSelected()) {
						// if AutoScroll is on and buffer is full
						// cause FifoDocument to free some space
						update_gui(0, true);
					}
					continue;
				}
				int count = 0;
				int target = 500000;
				if (target > length) target = length;
				long begin = System.currentTimeMillis();

				while (count < target && output.program != null) {
					if (reader.ready()) {
						int to_read = length - count;
						if (to_read > 32768) {
							// limit read size, helps keep slow
							// computers more responsive
							to_read = 32768;
						}
						int r = reader.read(buffer, offset + count, to_read);
						if (r > 0) {
							count += r;
						}
					} else {
						sleep(1);
					}
					if (count > 0 && System.currentTimeMillis() - begin >= 33) {
						break;
					}
				}
				boolean scroll = output.autoscrollBox.isSelected();
				update_gui(count, scroll);
				if (scroll) output.textArea.setCaretPosition(doc.getLength());
			}
		} catch (Exception e) {
			if (output.debug) System.out.println("inputPipeListener exception: " + e);
			//e.printStackTrace();
		} finally {
			try {
				output.close();
			} catch (Exception e) {
				output.disconnect();
			}
			if (output.debug) System.out.println("inputPipeListener thread exit");
			input = null;
			output = null;
			doc = null;
		}
	}
	private void update_gui(int chars_added, boolean auto_scroll) {
		// TODO: perhaps we can preprocess the data on this thread
		// where the preprocessing step would compute the newline
		// offsets and advance the head pointer.  Then we could
		// call invokeLater rather than having to wait.  The only
		// reason we must wait is doc.getAppendIndex() and
		// doc.getAvailableToAppend() will return wrong info and
		// we would clobber this data with the next incoming data.
		final Runnable do_update = new Runnable() {
			public void run() {
				doc.setScrollingMode(auto_scroll);
				doc.processAppended(chars_added);
				output.textArea.invalidate();
			}
		};
		int retry = 0;
		while (true) {
			try {
				// https://www.javamex.com/tutorials/threads/invokelater.shtml
				SwingUtilities.invokeAndWait(do_update);
				return;
			} catch (InterruptedException e) {
				if (output.debug) System.out.println("GUI update interrupted");
				if (output.program == null) return;
				if (++retry > 4) return;
			} catch (Exception e) {
				if (output.debug) System.err.println("ERROR: GUI update failed");
				e.printStackTrace();
				break;
			}
		}
	}
}

class errorPipeListener extends Thread
{
	InputStream input;
	TeensyPipeMonitor output;

	public void run() {
		setName("TeensyPipeMonitor errorPipeListener");
		InputStreamReader reader = new InputStreamReader(input);
		BufferedReader in = new BufferedReader(reader);
		try {
			while (output.program != null) {
				String line = in.readLine();
				//System.err.print("line: ");
				if (line.startsWith("Opened ")) {
					String parts[] = line.trim().split(" ", 3);
					if (parts.length == 3) {
						output.opened(parts[1], parts[2]);
					}
				} else if (line.startsWith("Disconnect ")) {
					output.disconnect();
				} else {
					System.err.println(line);
				}
			}
		} catch (Exception e) {
		} finally {
			if (output.debug) System.out.println("errorPipeListener thread exit");
			input = null;
			output = null;
		}
	}

}

