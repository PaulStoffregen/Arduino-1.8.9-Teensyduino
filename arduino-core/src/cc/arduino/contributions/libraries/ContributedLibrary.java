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

package cc.arduino.contributions.libraries;

import cc.arduino.contributions.DownloadableContribution;
import processing.app.I18n;
import processing.app.packages.UserLibrary;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static processing.app.I18n.tr;

public abstract class ContributedLibrary extends DownloadableContribution {

  public abstract String getName();

  public abstract String getMaintainer();

  public abstract String getAuthor();

  public abstract String getWebsite();

  public abstract String getCategory();

  public abstract void setCategory(String category);

  public abstract String getLicense();

  public abstract String getParagraph();

  public abstract String getSentence();

  public abstract List<String> getArchitectures();

  public abstract List<String> getTypes();

  public abstract List<ContributedLibraryReference> getRequires();

  public abstract List<String> getProvidesIncludes();

  public static final Comparator<ContributedLibrary> CASE_INSENSITIVE_ORDER = (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());

  private Optional<UserLibrary> installedLib = Optional.empty();

  public Optional<UserLibrary> getInstalledLibrary() {
    return installedLib;
  }

  public boolean isLibraryInstalled() {
    return installedLib.isPresent();
  }

  public void setInstalledUserLibrary(UserLibrary installed) {
    this.installedLib = Optional.of(installed);
  }

  public void unsetInstalledUserLibrary() {
    installedLib = Optional.empty();
  }

  public boolean isIDEBuiltIn() {
    if (!installedLib.isPresent()) {
      return false;
    }
    return installedLib.get().isIDEBuiltIn();
  }

  /**
   * Returns <b>true</b> if the library declares to support the specified
   * architecture (through the "architectures" property field).
   *
   * @param reqArch
   * @return
   */
  public boolean supportsArchitecture(String reqArch) {
    return getArchitectures().contains(reqArch) || getArchitectures().contains("*");
  }

  /**
   * Returns <b>true</b> if the library declares to support at least one of the
   * specified architectures.
   *
   * @param reqArchs A List of architectures to check
   * @return
   */
  public boolean supportsArchitecture(List<String> reqArchs) {
    if (reqArchs.contains("*"))
      return true;
    for (String reqArch : reqArchs)
      if (supportsArchitecture(reqArch))
        return true;
    return false;
  }

  @Override
  public String toString() {
    return I18n.format(tr("Version {0}"), getParsedVersion());
  }

  public String info() {
    String res = "";
    res += "  ContributedLibrary : " + getName() + "\n";
    res += "              author : " + getAuthor() + "\n";
    res += "          maintainer : " + getMaintainer() + "\n";
    res += "             version : " + getParsedVersion() + "\n";
    res += "             website : " + getUrl() + "\n";
    res += "            category : " + getCategory() + "\n";
    res += "             license : " + getLicense() + "\n";
    res += "             descrip : " + getSentence() + "\n";
    if (getParagraph() != null && !getParagraph().isEmpty())
      res += "                       " + getParagraph() + "\n";
    res += "       architectures : ";
    if (getArchitectures() != null)
      for (String a : getArchitectures()) {
        res += a + ",";
      }
    res += "\n";
    res += "            requires :\n";
    if (getRequires() != null)
      for (ContributedLibraryReference r : getRequires()) {
        res += "                       " + r;
      }
    res += "\n";

    // DownloadableContribution
    res += super.toString();

    return res;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ContributedLibrary)) {
      return false;
    }
    ContributedLibrary other = (ContributedLibrary) obj;
    String thisVersion = getParsedVersion();
    String otherVersion = other.getParsedVersion();

    boolean versionEquals = (thisVersion != null && otherVersion != null
                             && thisVersion.equals(otherVersion));

    // Important: for legacy libs, versions are null. Two legacy libs must
    // always pass this test.
    if (thisVersion == null && otherVersion == null)
      versionEquals = true;

    String thisName = getName();
    String otherName = other.getName();

    boolean nameEquals = thisName == null || otherName == null || thisName.equals(otherName);

    return versionEquals && nameEquals;
  }
}
