/*
 * This file is part of Arduino.
 *
 * Copyright 2015 Arduino LLC (http://www.arduino.cc/)
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

package cc.arduino.contributions;

import cc.arduino.contributions.libraries.LibraryInstaller;
import cc.arduino.contributions.libraries.filters.UpdatableLibraryPredicate;
import cc.arduino.contributions.packages.ContributionInstaller;
import cc.arduino.contributions.packages.filters.UpdatablePlatformPredicate;
import cc.arduino.view.NotificationPopup;
import processing.app.Base;
import processing.app.BaseNoGui;
import processing.app.Editor;
import processing.app.I18n;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.TimerTask;

import static processing.app.I18n.tr;

public class ContributionsSelfCheck extends TimerTask {

  private final Base base;
  private final HyperlinkListener hyperlinkListener;
  private final ContributionInstaller contributionInstaller;
  private final LibraryInstaller libraryInstaller;
  private final ProgressListener progressListener;

  private volatile boolean cancelled;
  private volatile NotificationPopup notificationPopup;

  public ContributionsSelfCheck(Base base, HyperlinkListener hyperlinkListener, ContributionInstaller contributionInstaller, LibraryInstaller libraryInstaller) {
    this.base = base;
    this.hyperlinkListener = hyperlinkListener;
    this.contributionInstaller = contributionInstaller;
    this.libraryInstaller = libraryInstaller;
    this.progressListener = new NoopProgressListener();
    this.cancelled = false;
  }

  @Override
  public void run() {
    updateContributionIndex();
    updateLibrariesIndex();

    boolean updatablePlatforms = checkForUpdatablePlatforms();

    boolean updatableLibraries = checkForUpdatableLibraries();

    if (!updatableLibraries && !updatablePlatforms) {
      return;
    }

    String text;
    if (updatableLibraries && !updatablePlatforms) {
      text = I18n.format(tr("Updates available for some of your {0}libraries{1}"), "<a href=\"http://librarymanager/DropdownUpdatableLibrariesItem\">", "</a>");
    } else if (!updatableLibraries && updatablePlatforms) {
      text = I18n.format(tr("Updates available for some of your {0}boards{1}"), "<a href=\"http://boardsmanager/DropdownUpdatableCoresItem\">", "</a>");
    } else {
      text = I18n.format(tr("Updates available for some of your {0}boards{1} and {2}libraries{3}"), "<a href=\"http://boardsmanager/DropdownUpdatableCoresItem\">", "</a>", "<a href=\"http://librarymanager/DropdownUpdatableLibrariesItem\">", "</a>");
    }

    if (cancelled) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      Editor ed = base.getActiveEditor();
      notificationPopup = new NotificationPopup(ed, hyperlinkListener, text);
      if (ed.isFocused()) {
        notificationPopup.begin();
        return;
      }

      // If the IDE is not focused wait until it is focused again to
      // display the notification, this avoids the annoying side effect
      // to "steal" the focus from another application.
      WindowFocusListener wfl = new WindowFocusListener() {
        @Override
        public void windowLostFocus(WindowEvent evt) {
        }

        @Override
        public void windowGainedFocus(WindowEvent evt) {
          notificationPopup.begin();
          for (Editor e : base.getEditors())
            e.removeWindowFocusListener(this);
        }
      };
      for (Editor e : base.getEditors())
        e.addWindowFocusListener(wfl);
    });
  }

  static boolean checkForUpdatablePlatforms() {
    return BaseNoGui.indexer.getPackages().stream()
      .flatMap(pack -> pack.getPlatforms().stream())
      .anyMatch(new UpdatablePlatformPredicate());
  }

  static boolean checkForUpdatableLibraries() {
    return BaseNoGui.librariesIndexer.getIndex().getLibraries().stream()
      .anyMatch(new UpdatableLibraryPredicate());
  }

  @Override
  public boolean cancel() {
    cancelled = true;
    if (notificationPopup != null) {
      notificationPopup.close();
    }
    return super.cancel();
  }

  private void updateLibrariesIndex() {
    if (cancelled) {
      return;
    }
    try {
      libraryInstaller.updateIndex(progressListener);
    } catch (Exception e) {
      // ignore
    }
  }

  private void updateContributionIndex() {
    if (cancelled) {
      return;
    }
    try {
      contributionInstaller.updateIndex(progressListener);
    } catch (Exception e) {
      // ignore
    }
  }
}
