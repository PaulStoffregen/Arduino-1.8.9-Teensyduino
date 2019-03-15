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

package cc.arduino.contributions.packages.filters;

import cc.arduino.contributions.VersionComparator;
import cc.arduino.contributions.packages.ContributedPlatform;
import processing.app.BaseNoGui;

import java.util.List;
import java.util.function.Predicate;

public class UpdatablePlatformPredicate implements Predicate<ContributedPlatform> {

  @Override
  public boolean test(ContributedPlatform contributedPlatform) {
    String packageName = contributedPlatform.getParentPackage().getName();
    String architecture = contributedPlatform.getArchitecture();

    ContributedPlatform installed = BaseNoGui.indexer.getInstalled(packageName, architecture);
    if (installed == null) {
      return false;
    }

    List<ContributedPlatform> platforms = BaseNoGui.indexer.getIndex().findPlatforms(packageName, architecture);
    return platforms.stream()
      .filter(platform -> VersionComparator.greaterThan(platform.getParsedVersion(), installed.getParsedVersion()))
      .count() > 0;
  }
}
