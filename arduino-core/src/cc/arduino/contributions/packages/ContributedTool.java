/*
 * This file is part of Arduino.
 *
 * Copyright 2014 Arduino LLC (http://www.arduino.cc/)
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

package cc.arduino.contributions.packages;

import cc.arduino.contributions.DownloadableContribution;
import processing.app.Platform;

import java.io.File;
import java.util.List;

public abstract class ContributedTool {

  public abstract String getName();

  public abstract String getVersion();

  public abstract List<HostDependentDownloadableContribution> getSystems();

  private boolean installed;

  public boolean isInstalled() {
    return installed;
  }

  public void setInstalled(boolean installed) {
    this.installed = installed;
  }

  private File installedFolder;

  public File getInstalledFolder() {
    return installedFolder;
  }

  public void setInstalledFolder(File installedFolder) {
    this.installedFolder = installedFolder;
  }

  private boolean builtIn;

  public boolean isBuiltIn() {
    return builtIn;
  }

  public void setBuiltIn(boolean builtIn) {
    this.builtIn = builtIn;
  }

  private ContributedPackage contributedPackage;

  public ContributedPackage getPackage() {
    return contributedPackage;
  }

  public void setPackage(ContributedPackage pack) {
    contributedPackage = pack;
  }

  public String getPackager() {
    return contributedPackage.getName();
  }

  public DownloadableContribution getDownloadableContribution(Platform platform) {
    for (HostDependentDownloadableContribution c : getSystems()) {
      if (c.isCompatible(platform))
        return c;
    }
    return null;
  }

  @Override
  public String toString() {
    return toString(null);
  }

  public String toString(Platform platform) {
    String res;
    res = "Tool name : " + getName() + " " + getVersion() + "\n";
    for (HostDependentDownloadableContribution sys : getSystems()) {
      res += "     sys";
      if (platform != null) {
        res += sys.isCompatible(platform) ? "*" : " ";
      }
      res += " : " + sys + "\n";
    }
    return res;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ContributedTool)) {
      return false;
    }

    ContributedTool obj1 = (ContributedTool) obj;
    return getName().equals(obj1.getName()) && getVersion().equals(obj1.getVersion());
  }
}
