/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2007 Ben Fry and Casey Reas

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

package processing.app.macosx;

import com.apple.eawt.*;
import com.apple.eawt.AppEvent.AppReOpenedEvent;

import processing.app.Base;
import processing.app.Editor;

import java.io.File;
import java.util.List;


/**
 * Deal with issues related to thinking different. This handles the basic
 * Mac OS X menu commands (and apple events) for open, about, prefs, etc.
 * <p>
 * Based on OSXAdapter.java from Apple DTS.
 * </p>
 * As of 0140, this code need not be built on platforms other than OS X,
 * because of the new platform structure which isolates through reflection.
 */
public class ThinkDifferent {

  private static final int MAX_WAIT_FOR_BASE = 30000;

  static public void init() {
    Application application = Application.getApplication();

    application.addAppEventListener(new AppReOpenedListener() {
      @Override
        public void appReOpened(AppReOpenedEvent aroe) {
          try {
            if (Base.INSTANCE.getEditors().size() == 0) {
              Base.INSTANCE.handleNew();
            }
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
      }
    });
    application.setAboutHandler(new AboutHandler() {
      @Override
      public void handleAbout(AppEvent.AboutEvent aboutEvent) {
        new Thread(() -> {
          if (waitForBase()) {
            Base.INSTANCE.handleAbout();
          }
        }).start();
      }
    });
    application.setPreferencesHandler(new PreferencesHandler() {
      @Override
      public void handlePreferences(AppEvent.PreferencesEvent preferencesEvent) {
        new Thread(() -> {
          if (waitForBase()) {
            Base.INSTANCE.handlePrefs();
          }
        }).start();
      }
    });
    application.setOpenFileHandler(new OpenFilesHandler() {
      @Override
      public void openFiles(final AppEvent.OpenFilesEvent openFilesEvent) {
        new Thread(() -> {
          if (waitForBase()) {
            for (File file : openFilesEvent.getFiles()) {
              System.out.println(file);
              try {
                Base.INSTANCE.handleOpen(file);
                List<Editor> editors = Base.INSTANCE.getEditors();
                if (editors.size() == 2 && editors.get(0).getSketchController().isUntitled()) {
                  Base.INSTANCE.handleClose(editors.get(0));
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
          }
        }).start();
      }
    });
    application.setQuitHandler(new QuitHandler() {
      @Override
      public void handleQuitRequestWith(AppEvent.QuitEvent quitEvent, QuitResponse quitResponse) {
        new Thread(() -> {
          if (waitForBase()) {
            if (Base.INSTANCE.handleQuit()) {
              quitResponse.performQuit();
            } else {
              quitResponse.cancelQuit();
            }
          }
        }).start();
      }
    });
  }

  private static boolean waitForBase() {
    int slept = 0;
    while (Base.INSTANCE == null) {
      if (slept >= MAX_WAIT_FOR_BASE) {
        return false;
      }
      sleep(100);
      slept += 100;
    }
    return true;
  }

  private static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      //ignore
    }
  }

}
