package processing.app.forms;

import processing.app.Base;
import processing.app.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import static processing.app.I18n.tr;

public class PasswordAuthorizationDialog extends JDialog {

  private final JPasswordField passwordField;

  private boolean cancelled;
  private String password;

  public PasswordAuthorizationDialog(Frame parent, String dialogText) {
    super(parent, true);

    this.cancelled = false;
    this.password = null;

    JLabel typePasswordLabel = new JLabel();
    JLabel icon = new JLabel();
    JLabel passwordLabel = new JLabel();
    passwordField = new JPasswordField();
    JButton uploadButton = new JButton();
    JButton cancelButton = new JButton();

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    typePasswordLabel.setText(dialogText);

    icon.setIcon(new ImageIcon(Theme.getThemeResource("theme/lock.png").getUrl()));

    passwordLabel.setText(tr("Password:"));

    passwordField.setText("");
    passwordField.addActionListener(PasswordAuthorizationDialog.this::uploadButtonPressed);

    uploadButton.setText(tr("Upload"));
    uploadButton.addActionListener(PasswordAuthorizationDialog.this::uploadButtonPressed);

    cancelButton.setText(tr("Cancel"));
    cancelButton.addActionListener(PasswordAuthorizationDialog.this::cancelButtonPressed);

    Base.registerWindowCloseKeys(getRootPane(), this::cancelButtonPressed);

    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
          .addContainerGap()
          .addComponent(icon, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(typePasswordLabel)
            .addGroup(layout.createSequentialGroup()
              .addComponent(passwordLabel)
              .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
              .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, 300, GroupLayout.PREFERRED_SIZE)))
          .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
          .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(cancelButton)
          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
          .addComponent(uploadButton)
          .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
          .addContainerGap()
          .addComponent(typePasswordLabel)
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
              .addGap(53, 53, 53)
              .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(passwordLabel)
                .addComponent(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
              .addGap(18, 18, 18))
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
              .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
              .addComponent(icon)
              .addGap(9, 9, 9)))
          .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(uploadButton)
            .addComponent(cancelButton))
          .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    pack();
  }

  private void cancelButtonPressed(ActionEvent event) {
    this.cancelled = true;
    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
  }

  private void uploadButtonPressed(ActionEvent event) {
    this.password = new String(passwordField.getPassword());
    dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
  }

  public String getPassword() {
    return this.password;
  }

  public boolean isCancelled() {
    return cancelled;
  }
}
