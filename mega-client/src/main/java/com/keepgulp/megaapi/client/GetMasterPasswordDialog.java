package com.keepgulp.megaapi.client;

import static com.keepgulp.megaapi.client.MainPanel.*;
import static com.keepgulp.megaapi.client.MiscTools.*;
import java.awt.Dialog;
import java.awt.HeadlessException;
import java.awt.event.KeyEvent;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/**
 *
 * @author tonikelope
 */
public class GetMasterPasswordDialog extends javax.swing.JDialog {

    private boolean _pass_ok;

    private String _current_pass_hash;

    private final String _salt;

    private byte[] _pass;

    public JPasswordField getNew_pass_textfield() {
        return current_pass_textfield;
    }

    public JCheckBox getRemember_checkbox() {
        return remember_checkbox;
    }

    public boolean isPass_ok() {
        return _pass_ok;
    }

    public byte[] getPass() {
        return _pass;
    }

    public void deletePass() {

        if (_pass != null) {

            Arrays.fill(_pass, (byte) 0);
        }

        _pass = null;
    }

    /**
     * Creates new form MegaPassDialog
     */
    public GetMasterPasswordDialog(java.awt.Frame parent, boolean modal, String current_pass_hash, String salt, MainPanel main_panel) {
        super(parent, modal);

        initComponents();

        updateFonts(this, GUI_FONT, main_panel.getZoom_factor());

        translateLabels(this);

        _current_pass_hash = current_pass_hash;

        _pass_ok = false;

        _pass = null;

        _salt = salt;

        pack();

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        current_pass_textfield = new JPasswordField();
        cancel_button = new javax.swing.JButton();
        ok_button = new javax.swing.JButton();
        lock_label = new javax.swing.JLabel();
        please_label = new javax.swing.JLabel();
        status_label = new javax.swing.JLabel();
        remember_checkbox = new JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Master password unlock");
        setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);

        current_pass_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        current_pass_textfield.setDoubleBuffered(true);
        current_pass_textfield.setMargin(new java.awt.Insets(2, 2, 2, 2));
        current_pass_textfield.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                current_pass_textfieldKeyPressed(evt);
            }
        });

        cancel_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        cancel_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-cancel-30.png"))); // NOI18N
        cancel_button.setText("CANCEL");
        cancel_button.setDoubleBuffered(true);
        cancel_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel_buttonActionPerformed(evt);
            }
        });

        ok_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        ok_button.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons8-ok-30.png"))); // NOI18N
        ok_button.setText("OK");
        ok_button.setDoubleBuffered(true);
        ok_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok_buttonActionPerformed(evt);
            }
        });

        lock_label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/lock_medium.png"))); // NOI18N
        lock_label.setDoubleBuffered(true);

        please_label.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        please_label.setText("Please, enter your master password");
        please_label.setDoubleBuffered(true);

        status_label.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        status_label.setDoubleBuffered(true);

        remember_checkbox.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        remember_checkbox.setSelected(true);
        remember_checkbox.setText("Remember for this session");
        remember_checkbox.setDoubleBuffered(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(status_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ok_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancel_button))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lock_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(current_pass_textfield)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(remember_checkbox)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addComponent(please_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(please_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(current_pass_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(remember_checkbox))
                    .addComponent(lock_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, 16, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(ok_button)
                        .addComponent(cancel_button))
                    .addComponent(status_label, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancel_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_buttonActionPerformed

        setVisible(false);
    }//GEN-LAST:event_cancel_buttonActionPerformed

    private void ok_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ok_buttonActionPerformed

        status_label.setText(LabelTranslatorSingleton.getInstance().translate("Verifying your password, please wait..."));

        pack();

        final Dialog tthis = this;

        THREAD_POOL.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    byte[] pass = CryptTools.PBKDF2HMACSHA256(new String(current_pass_textfield.getPassword()), BASE642Bin(_salt), CryptTools.MASTER_PASSWORD_PBKDF2_ITERATIONS, CryptTools.MASTER_PASSWORD_PBKDF2_OUTPUT_BIT_LENGTH);

                    String pass_hash = Bin2BASE64(HashBin("SHA-1", pass));

                    swingInvoke(
                            new Runnable() {
                        @Override
                        public void run() {
                            if (!pass_hash.equals(_current_pass_hash)) {

                                JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("BAD PASSWORD!"), "Error", JOptionPane.ERROR_MESSAGE);

                                status_label.setText("");

                                pack();

                                current_pass_textfield.setText("");

                                current_pass_textfield.grabFocus();

                            } else {

                                _pass = pass;

                                _current_pass_hash = pass_hash;

                                _pass_ok = true;

                                tthis.setVisible(false);
                            }
                        }
                    });

                } catch (HeadlessException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }

            }
        });

    }//GEN-LAST:event_ok_buttonActionPerformed

    private void current_pass_textfieldKeyPressed(KeyEvent evt) {//GEN-FIRST:event_current_pass_textfieldKeyPressed

        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {

            ok_buttonActionPerformed(null);
        }
    }//GEN-LAST:event_current_pass_textfieldKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancel_button;
    private JPasswordField current_pass_textfield;
    private javax.swing.JLabel lock_label;
    private javax.swing.JButton ok_button;
    private javax.swing.JLabel please_label;
    private JCheckBox remember_checkbox;
    private javax.swing.JLabel status_label;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(GetMasterPasswordDialog.class.getName());
}
