package processing.app.helpers;

import processing.app.BaseNoGui;
import processing.app.I18n;
import processing.app.PreferencesData;
import processing.app.debug.TargetBoard;
import processing.app.debug.TargetPackage;
import processing.app.debug.TargetPlatform;
import processing.app.legacy.PApplet;

import java.io.File;
import java.util.*;

import static processing.app.I18n.tr;

public class CommandlineParser {

  private enum ACTION {
    GUI, NOOP, VERIFY("--verify"), UPLOAD("--upload"), GET_PREF("--get-pref"), INSTALL_BOARD("--install-boards"), INSTALL_LIBRARY("--install-library"), VERSION("--version");

    final String value;

    ACTION() {
      this.value = null;
    }

    ACTION(String value) {
      this.value = value;
    }
  }

  private final String[] args;
  private final Map<String, ACTION> actions;
  private ACTION action = ACTION.GUI;
  private boolean doVerboseBuild = false;
  private boolean doVerboseUpload = false;
  private boolean doUseProgrammer = false;
  private boolean preserveTempFiles;
  private boolean noUploadPort = false;
  private boolean forceSavePrefs = false;
  private String getPref;
  private String boardToInstall;
  private String libraryToInstall;
  private Optional<String> uploadPort = Optional.empty();
  private final List<String> filenames = new LinkedList<>();

  public CommandlineParser(String[] args) {
    this.args = args;

    actions = new HashMap<>();
    actions.put("--verify", ACTION.VERIFY);
    actions.put("--upload", ACTION.UPLOAD);
    actions.put("--get-pref", ACTION.GET_PREF);
    actions.put("--install-boards", ACTION.INSTALL_BOARD);
    actions.put("--install-library", ACTION.INSTALL_LIBRARY);
    actions.put("--version", ACTION.VERSION);
  }

  public void parseArgumentsPhase1() {
    for (int i = 0; i < args.length; i++) {
      ACTION a = actions.get(args[i]);
      if (a != null) {
        if (action != ACTION.GUI && action != ACTION.NOOP) {
          Set<String> strings = actions.keySet();
          String[] valid = strings.toArray(new String[strings.size()]);
          String mess = I18n.format(tr("Can only pass one of: {0}"), PApplet.join(valid, ", "));
          BaseNoGui.showError(null, mess, 3);
        }
        if (a == ACTION.GET_PREF) {
          i++;
          if (i < args.length) {
            getPref = args[i];
          }
        }
        if (a == ACTION.INSTALL_BOARD) {
          i++;
          if (i >= args.length) {
            BaseNoGui.showError(null, I18n.format(tr("Argument required for {0}"), a.value), 3);
          }
          boardToInstall = args[i];
        }
        if (a == ACTION.INSTALL_LIBRARY) {
          i++;
          if (i >= args.length) {
            BaseNoGui.showError(null, I18n.format(tr("Argument required for {0}"), a.value), 3);
          }
          libraryToInstall = args[i];
        }
        action = a;
        continue;
      }
      if (args[i].startsWith("-psn")) {
        //discard
        continue;
      }
      if (args[i].equals("--verbose") || args[i].equals("-v")) {
        doVerboseBuild = true;
        doVerboseUpload = true;
        if (action == ACTION.GUI)
          action = ACTION.NOOP;
        continue;
      }
      if (args[i].equals("--preserve-temp-files")) {
        preserveTempFiles = true;
        if (action == ACTION.GUI)
          action = ACTION.NOOP;
        continue;
      }
      if (args[i].equals("--verbose-build")) {
        doVerboseBuild = true;
        if (action == ACTION.GUI)
          action = ACTION.NOOP;
        continue;
      }
      if (args[i].equals("--verbose-upload")) {
        doVerboseUpload = true;
        if (action == ACTION.GUI)
          action = ACTION.NOOP;
        continue;
      }
      if (args[i].equals("--useprogrammer")) {
        doUseProgrammer = true;
        if (action == ACTION.GUI)
          action = ACTION.NOOP;
        continue;
      }
      if (args[i].equals("--nouploadport")) {
        noUploadPort = true;
        if (action == ACTION.GUI)
          action = ACTION.NOOP;
        continue;
      }
      if (args[i].equals("--board")) {
        i++;
        if (i >= args.length)
          BaseNoGui.showError(null, tr("Argument required for --board"), 3);
        if (action == ACTION.GUI)
          action = ACTION.NOOP;
        continue;
      }
      if (args[i].equals("--port")) {
        i++;
        if (i >= args.length)
          BaseNoGui.showError(null, tr("Argument required for --port"), 3);
        uploadPort = Optional.of(args[i]);
        if (action == ACTION.GUI)
          action = ACTION.NOOP;
        continue;
      }
      if (args[i].equals("--curdir")) {
        BaseNoGui.showError(null, tr("--curdir no longer supported"), 3);
        return;
      }
      if (args[i].equals("--buildpath")) {
        i++;
        if (i >= args.length) {
          BaseNoGui.showError(null, "Argument required for --buildpath", 3);
        }
        File buildFolder = new File(args[i]);
        if (!buildFolder.exists()) {
          BaseNoGui.showError(null, "The build path doesn't exist", 3);
        }
        if (!buildFolder.isDirectory()) {
          BaseNoGui.showError(null, "The build path is not a folder", 3);
        }
        PreferencesData.set("build.path", buildFolder.getAbsolutePath());
        continue;
      }
      if (args[i].equals("--pref")) {
        i++;
        if (i >= args.length)
          BaseNoGui.showError(null, tr("Argument required for --pref"), 3);
        processPrefArgument(args[i]);
        if (action == ACTION.GUI)
          action = ACTION.NOOP;
        continue;
      }
      if (args[i].equals("--save-prefs")) {
        forceSavePrefs = true;
        continue;
      }
      if (args[i].equals("--preferences-file")) {
        i++;
        if (i >= args.length)
          BaseNoGui.showError(null, tr("Argument required for --preferences-file"), 3);
        // Argument should be already processed by Base.main(...)
        continue;
      }
      if (args[i].startsWith("--"))
        BaseNoGui.showError(null, I18n.format(tr("unknown option: {0}"), args[i]), 3);

      filenames.add(args[i]);
    }

    checkAction();
  }

  public void parseArgumentsPhase2() {
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--board")) {
        i++;
        if (i >= args.length) {
          BaseNoGui.showError(null, tr("Argument required for --board"), 3);
        }
        processBoardArgument(args[i]);
        if (action == ACTION.GUI) {
          action = ACTION.NOOP;
        }
      }
    }
  }

  private void checkAction() {
    if ((action == ACTION.UPLOAD || action == ACTION.VERIFY) && filenames.size() != 1)
      BaseNoGui.showError(null, tr("Must specify exactly one sketch file"), 3);

    if ((action == ACTION.NOOP || action == ACTION.GET_PREF) && filenames.size() != 0)
      BaseNoGui.showError(null, tr("Cannot specify any sketch files"), 3);

    if ((action != ACTION.UPLOAD && action != ACTION.VERIFY) && (doVerboseBuild || doVerboseUpload))
      BaseNoGui.showError(null, tr("--verbose, --verbose-upload and --verbose-build can only be used together with --verify or --upload"), 3);
  }

  private void processBoardArgument(String selectBoard) {
    // No board selected? Nothing to do
    if (selectBoard == null)
      return;

    String[] split = selectBoard.split(":", 4);

    if (split.length < 3) {
      BaseNoGui.showError(null, I18n.format(tr("{0}: Invalid board name, it should be of the form \"package:arch:board\" or \"package:arch:board:options\""), selectBoard), 3);
    }

    TargetPackage targetPackage = BaseNoGui.getTargetPackage(split[0]);
    if (targetPackage == null) {
      BaseNoGui.showError(null, I18n.format(tr("{0}: Unknown package"), split[0]), 3);
      return;
    }

    TargetPlatform targetPlatform = targetPackage.get(split[1]);
    if (targetPlatform == null) {
      BaseNoGui.showError(null, I18n.format(tr("{0}: Unknown architecture"), split[1]), 3);
      return;
    }

    TargetBoard targetBoard = targetPlatform.getBoard(split[2]);
    if (targetBoard == null || !targetBoard.getId().equals(split[2])) {
      BaseNoGui.showError(null, I18n.format(tr("{0}: Unknown board"), split[2]), 3);
      return;
    }

    BaseNoGui.selectBoard(targetBoard);
    BaseNoGui.onBoardOrPortChange();

    if (split.length > 3) {
      String[] options = split[3].split(",");
      for (String option : options) {
        String[] keyValue = option.split("=", 2);

        if (keyValue.length != 2)
          BaseNoGui.showError(null, I18n.format(tr("{0}: Invalid option, should be of the form \"name=value\""), option, targetBoard.getId()), 3);
        String key = keyValue[0].trim();
        String value = keyValue[1].trim();

        if (!targetBoard.hasMenu(key))
          BaseNoGui.showError(null, I18n.format(tr("{0}: Invalid option for board \"{1}\""), key, targetBoard.getId()), 3);
        if (targetBoard.getMenuLabel(key, value) == null)
          BaseNoGui.showError(null, I18n.format(tr("{0}: Invalid value for option \"{1}\" for board \"{2}\""), value, key, targetBoard.getId()), 3);

        PreferencesData.set("custom_" + key, targetBoard.getId() + "_" + value);
      }
    }
  }

  private void processPrefArgument(String arg) {
    String[] split = arg.split("=", 2);
    if (split.length != 2 || split[0].isEmpty())
      BaseNoGui.showError(null, I18n.format(tr("{0}: Invalid argument to --pref, should be of the form \"pref=value\""), arg), 3);

    PreferencesData.set(split[0], split[1]);
    PreferencesData.set("runtime.build_properties_custom." + split[0], split[1]);
  }

  public boolean isDoVerboseBuild() {
    return doVerboseBuild;
  }

  public boolean isDoVerboseUpload() {
    return doVerboseUpload;
  }

  public boolean isForceSavePrefs() {
    return forceSavePrefs;
  }

  public String getGetPref() {
    return getPref;
  }

  public List<String> getFilenames() {
    return filenames;
  }

  public boolean isGetPrefMode() {
    return action == ACTION.GET_PREF;
  }

  public boolean isGuiMode() {
    return action == ACTION.GUI;
  }

  public boolean isNoOpMode() {
    return action == ACTION.NOOP;
  }

  public boolean isUploadMode() {
    return action == ACTION.UPLOAD;
  }

  private boolean isVerifyMode() {
    return action == ACTION.VERIFY;
  }

  public boolean isVerifyOrUploadMode() {
    return isVerifyMode() || isUploadMode();
  }

  public boolean isDoUseProgrammer() {
    return doUseProgrammer;
  }

  public boolean isNoUploadPort() {
    return noUploadPort;
  }

  public boolean isInstallBoard() {
    return action == ACTION.INSTALL_BOARD;
  }

  public boolean isInstallLibrary() {
    return action == ACTION.INSTALL_LIBRARY;
  }

  public boolean isVersionMode() {
    return action == ACTION.VERSION;
  }

  public String getBoardToInstall() {
    return this.boardToInstall;
  }

  public String getLibraryToInstall() {
    return libraryToInstall;
  }

  public boolean isPreserveTempFiles() {
    return preserveTempFiles;
  }

  public Optional<String> getUploadPort() {
    return uploadPort;
  }
}
