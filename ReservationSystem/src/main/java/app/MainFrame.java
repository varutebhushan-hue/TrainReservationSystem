package app;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class MainFrame extends JFrame {

    public MainFrame(String user) {
        super("Online Reservation System  |  Logged in as: " + user);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Book Ticket", bookingPanel());
        tabs.addTab("Cancel Ticket", cancelPanel());
        add(tabs);
        setSize(520, 470); setLocationRelativeTo(null);
    }

    // Res. form
    private JPanel bookingPanel() {
        JTextField name = new JTextField(20), trainNo = new JTextField(20), trainName = new JTextField(20),
                   date = new JTextField(20), src = new JTextField(20), dst = new JTextField(20);
        trainName.setEditable(false);
        JComboBox<String> cls = new JComboBox<>(new String[]{"Sleeper", "AC 3 Tier", "AC 2 Tier", "AC First Class", "Chair Car"});
        date.setToolTipText("Format: yyyy-MM-dd");

        // train name after the train number is typed
        trainNo.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                String t = trainNo.getText().trim();
                if (!t.matches("\\d{1,9}")) { trainName.setText(t.isEmpty() ? "" : "(enter numeric train number)"); return; }
                try {
                    Optional<String> n = Db.trainName(Integer.parseInt(t));
                    trainName.setText(n.orElse("(train not found)"));
                } catch (Exception ex) { trainName.setText("(database error)"); }
            }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        String[] labels = {"Passenger Name *", "Train Number *", "Train Name", "Class Type *", "Date of Journey * (yyyy-MM-dd)", "Source Station *", "Destination Station *"};
        JComponent[] fields = {name, trainNo, trainName, cls, date, src, dst};
        addRows(p, labels, fields);

        JButton book = new JButton("Book Ticket");
        book.addActionListener(e -> {
            String pn = name.getText().trim(), tn = trainNo.getText().trim(), d = date.getText().trim(),
                   s = src.getText().trim(), t = dst.getText().trim();
            if (pn.isEmpty() || tn.isEmpty() || d.isEmpty() || s.isEmpty() || t.isEmpty()) {
                warn("All fields marked * are required."); return;
            }
            if (!tn.matches("\\d{1,9}")) { warn("Train number must be numeric."); return; }
            LocalDate jd;
            try { jd = LocalDate.parse(d); } catch (DateTimeParseException ex) { warn("Invalid date. Use yyyy-MM-dd (e.g. 2026-12-25)."); return; }
            if (jd.isBefore(LocalDate.now())) { warn("Date of journey cannot be in the past."); return; }
            if (s.equalsIgnoreCase(t)) { warn("Source and destination must be different."); return; }
            try {
                Optional<String> tName = Db.trainName(Integer.parseInt(tn));
                if (tName.isEmpty()) { warn("Train number not found."); return; }
                String pnr = Db.book(pn, Integer.parseInt(tn), tName.get(), (String) cls.getSelectedItem(), jd.toString(), s, t);
                Db.Booking b = new Db.Booking(pnr, pn, Integer.parseInt(tn), tName.get(), (String) cls.getSelectedItem(), jd.toString(), s, t);
                JOptionPane.showMessageDialog(this, "Booking confirmed!\n\n" + b + "\n\nPlease note your PNR for cancellation.",
                        "Booking Confirmation", JOptionPane.INFORMATION_MESSAGE);
                name.setText(""); trainNo.setText(""); date.setText(""); src.setText(""); dst.setText("");
            } catch (Exception ex) { error(ex); }
        });
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = labels.length; g.gridwidth = 2; g.insets = new Insets(15, 0, 0, 0);
        p.add(book, g);
        return p;
    }

    // Cancel ur form
    private JPanel cancelPanel() {
        JTextField pnrField = new JTextField(14);
        JButton fetch = new JButton("Fetch");
        JTextArea details = new JTextArea(9, 34);
        details.setEditable(false); details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JButton cancel = new JButton("Confirm Cancellation");
        cancel.setEnabled(false);
        String[] fetched = {null};

        fetch.addActionListener(e -> {
            String pnr = pnrField.getText().trim();
            cancel.setEnabled(false); fetched[0] = null; details.setText("");
            if (pnr.isEmpty()) { warn("Please enter a PNR number."); return; }
            if (!pnr.matches("\\d{10}")) { warn("PNR must be a 10-digit number."); return; }
            try {
                Optional<Db.Booking> b = Db.find(pnr);
                if (b.isPresent()) { details.setText(b.get().toString()); fetched[0] = pnr; cancel.setEnabled(true); }
                else details.setText("No booking found for PNR " + pnr);
            } catch (Exception ex) { error(ex); }
        });

        cancel.addActionListener(e -> {
            if (fetched[0] == null) return;
            int ans = JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel PNR " + fetched[0] + "?",
                    "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ans != JOptionPane.YES_OPTION) return;
            try {
                if (Db.cancel(fetched[0])) {
                    JOptionPane.showMessageDialog(this, "Booking " + fetched[0] + " has been cancelled.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                    details.setText(""); pnrField.setText(""); fetched[0] = null; cancel.setEnabled(false);
                } else warn("Booking not found (it may already be cancelled).");
            } catch (Exception ex) { error(ex); }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 12));
        top.add(new JLabel("PNR Number:")); top.add(pnrField); top.add(fetch);
        JPanel bottom = new JPanel(); bottom.add(cancel);
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        p.add(top, BorderLayout.NORTH); p.add(new JScrollPane(details), BorderLayout.CENTER); p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    //helpers
    private static void addRows(JPanel p, String[] labels, JComponent[] fields) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4); g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; g.fill = GridBagConstraints.NONE; p.add(new JLabel(labels[i]), g);
            g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; p.add(fields[i], g);
        }
    }
    private void warn(String m)  { JOptionPane.showMessageDialog(this, m, "Validation", JOptionPane.WARNING_MESSAGE); }
    private void error(Exception ex) { JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
}
