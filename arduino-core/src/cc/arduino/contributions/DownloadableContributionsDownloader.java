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

package cc.arduino.contributions;

import cc.arduino.utils.FileHash;
import cc.arduino.utils.Progress;
import cc.arduino.utils.network.FileDownloader;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import static processing.app.I18n.format;
import static processing.app.I18n.tr;

public class DownloadableContributionsDownloader {

  private final File stagingFolder;

  public DownloadableContributionsDownloader(File _stagingFolder) {
    stagingFolder = _stagingFolder;
  }

  public File download(DownloadableContribution contribution, Progress progress, final String statusText, ProgressListener progressListener) throws Exception {
    return download(contribution, progress, statusText, progressListener, false);
  }

  public File download(DownloadableContribution contribution, Progress progress, final String statusText, ProgressListener progressListener, boolean noResume) throws Exception {
    URL url = new URL(contribution.getUrl());
    Path outputFile = Paths.get(stagingFolder.getAbsolutePath(), contribution.getArchiveFileName());

    // Ensure the existence of staging folder
    Files.createDirectories(stagingFolder.toPath());

    if (!hasChecksum(contribution) && Files.exists(outputFile)) {
      Files.delete(outputFile);
    }

    boolean downloaded = false;
    while (true) {
      // Need to download or resume downloading?
      if (!Files.isRegularFile(outputFile, LinkOption.NOFOLLOW_LINKS) || (Files.size(outputFile) < contribution.getSize())) {
        download(url, outputFile.toFile(), progress, statusText, progressListener, noResume);
        downloaded = true;
      }

      // Test checksum
      progress.setStatus(tr("Verifying archive integrity..."));
      progressListener.onProgress(progress);
      if (hasChecksum(contribution)) {
        String checksum = contribution.getChecksum();
        String algo = checksum.split(":")[0];
        String crc = FileHash.hash(outputFile.toFile(), algo);
        if (!crc.equalsIgnoreCase(checksum)) {
          // If the file has not been downloaded it may be a leftover of
          // a previous download that failed. In this case delete it and
          // try to download it again.
          if (!downloaded) {
            Files.delete(outputFile);
            downloaded = true; // Redundant to avoid loops in case delete fails
            continue;
          }

          // Otherwise throw the error.
          throw new Exception(tr("CRC doesn't match, file is corrupted. It may be a temporary problem, please retry later."));
        }
      }

      // Download completed successfully
      break;
    }

    contribution.setDownloaded(true);
    contribution.setDownloadedFile(outputFile.toFile());
    return outputFile.toFile();
  }

  private boolean hasChecksum(DownloadableContribution contribution) {
    String checksum = contribution.getChecksum();
    if (checksum == null || checksum.isEmpty()) {
      return false;
    }

    String algo = checksum.split(":")[0];

    return algo != null && !algo.isEmpty();
  }

  public void download(URL url, File tmpFile, Progress progress, String statusText, ProgressListener progressListener) throws Exception {
    download(url, tmpFile, progress, statusText, progressListener, false);
  }

  public void download(URL url, File tmpFile, Progress progress, String statusText, ProgressListener progressListener, boolean noResume) throws Exception {
    FileDownloader downloader = new FileDownloader(url, tmpFile);
    downloader.addObserver((o, arg) -> {
      FileDownloader me = (FileDownloader) o;
      String msg = "";
      if (me.getDownloadSize() != null) {
        long downloaded = (me.getInitialSize() + me.getDownloaded()) / 1000;
        long total = (me.getInitialSize() + me.getDownloadSize()) / 1000;
        msg = format(tr("Downloaded {0}kb of {1}kb."), downloaded, total);
      }
      progress.setStatus(statusText + " " + msg);
      progress.setProgress(me.getProgress());
      progressListener.onProgress(progress);
    });
    downloader.download(noResume);
    if (!downloader.isCompleted()) {
      throw new Exception(format(tr("Error downloading {0}"), url), downloader.getError());
    }
  }

}
