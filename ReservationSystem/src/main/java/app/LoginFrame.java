package app;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField userField = new JTextField(15);
    private final JPasswordField passField = new JPasswordField(15);

    public LoginFrame() {
        super("Online Reservation System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 10, 25));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6); g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Username:"), g);
        g.gridx = 1; form.add(userField, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Password:"), g);
        g.gridx = 1; form.add(passField, g);

        JButton login = new JButton("Login");
        login.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(login);
        JPanel south = new JPanel(); south.add(login);

        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack(); setLocationRelativeTo(null); setResizable(false);
    }

    private void doLogin() {
        String u = userField.getText().trim();
        String p = new String(passField.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and password are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (Db.login(u, p)) {
                dispose();
                new MainFrame(u).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Access denied: invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                passField.setText("");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try { Db.init(); }
        catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to the database:\n" + e.getMessage()
                    + "\n\nCheck DB_URL / DB_USER / DB_PASS.", "Database Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
