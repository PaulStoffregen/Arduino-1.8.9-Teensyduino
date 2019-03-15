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

package processing.app.windows;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ListComPortsParserTest {

  @Test
  public void shouldFindVIDPID() throws Exception {
    String listComPortsOutput = "COM26 - FTDI - FTDIBUS\\VID_0403+PID_6001+A6004CCFA\\0000\nCOM24 - PJRC.COM, LLC. - USB\\VID_16C0&PID_0483\\12345";

    assertEquals("0X0403_0X6001", new ListComPortsParser().extractVIDAndPID(listComPortsOutput, "COM26"));
    assertEquals("0X16C0_0X0483", new ListComPortsParser().extractVIDAndPID(listComPortsOutput, "COM24"));
  }

  @Test
  public void shouldFindVIDPID2() throws Exception {
    String listComPortsOutput = "COM1 - (Standard port types) - ACPI\\PNP0501\\1\n" +
      "COM3 - IVT Corporation - {F12D3CF8-B11D-457E-8641-BE2AF2D6D204}\\IVTCOMM\\1&27902E60&2&0001\n" +
      "COM4 - IVT Corporation - {F12D3CF8-B11D-457E-8641-BE2AF2D6D204}\\IVTCOMM\\1&27902E60&2&0002\n" +
      "COM18 - FTDI - FTDIBUS\\VID_0403+PID_0000+A9EPHBR7A\\0000";

    assertEquals("0X0403_0X0000", new ListComPortsParser().extractVIDAndPID(listComPortsOutput, "COM18"));
  }

  @Test
  public void shouldNotBeFooledByCOMPortsWithSimilarNames() throws Exception {
    String listComPortsOutput = "COM1 - (Standard port types) - ACPI\\PNP0501\\1\n" +
      "COM2 - (Standard port types) - ACPI\\PNP0501\\2\n" +
      "COM12 - Arduino LLC (www.arduino.cc) - USB\\VID_2341&PID_8041&MI_00\\8&AB76839&0&0000\n" +
      "COM3 - FTDI - FTDIBUS\\VID_0403+PID_6015+DA00WSEWA\\0000";

    assertEquals("0X2341_0X8041", new ListComPortsParser().extractVIDAndPID(listComPortsOutput, "COM12"));
    assertNull(new ListComPortsParser().extractVIDAndPID(listComPortsOutput, "COM1"));
  }

}
