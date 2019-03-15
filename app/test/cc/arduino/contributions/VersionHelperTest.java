/*
 * This file is part of Arduino.
 *
 * Copyright 2016 Arduino LLC (http://www.arduino.cc/)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import com.github.zafarkhaja.semver.Version;

public class VersionHelperTest {

  public void assertOptionalEquals(String expected, Optional<Version> value) {
    assertTrue(value.isPresent());
    assertEquals(expected, value.get().toString());
  }

  @Test
  public void testVersions() throws Exception {
    assertOptionalEquals("1.0.0", VersionHelper.valueOf("1.0.0"));
    assertOptionalEquals("1.0.0", VersionHelper.valueOf("1.0"));
    assertOptionalEquals("1.0.0", VersionHelper.valueOf("1"));
    assertOptionalEquals("1.0.0-abc", VersionHelper.valueOf("1.0.0-abc"));
    assertOptionalEquals("1.0.0-abc", VersionHelper.valueOf("1.0-abc"));
    assertOptionalEquals("1.0.0-abc", VersionHelper.valueOf("1-abc"));
    assertOptionalEquals("1.0.0+abc", VersionHelper.valueOf("1.0.0+abc"));
    assertOptionalEquals("1.0.0+abc", VersionHelper.valueOf("1.0+abc"));
    assertOptionalEquals("1.0.0+abc", VersionHelper.valueOf("1+abc"));
    assertOptionalEquals("1.0.0-def+abc", VersionHelper.valueOf("1.0.0-def+abc"));
    assertOptionalEquals("1.0.0-def+abc", VersionHelper.valueOf("1.0-def+abc"));
    assertOptionalEquals("1.0.0-def+abc", VersionHelper.valueOf("1-def+abc"));
    assertOptionalEquals("1.0.0+def-abc", VersionHelper.valueOf("1.0.0+def-abc"));
    assertOptionalEquals("1.0.0+def-abc", VersionHelper.valueOf("1.0+def-abc"));
    assertOptionalEquals("1.0.0+def-abc", VersionHelper.valueOf("1+def-abc"));
  }

}
