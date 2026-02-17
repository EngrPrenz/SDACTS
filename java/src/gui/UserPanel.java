package gui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class UserPanel extends JPanel {
    private UserDAO userDAO;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;
    private JButton refreshButton;
    private int selectedUserId = -1;

    public UserPanel() {
        userDAO = new UserDAO();
        initComponents();
        loadUsers();
    }

    // Custom painted button - overrides Windows theme completely
    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(95, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg);         btn.setForeground(Color.WHITE); }
        });
        return btn;
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── Form Panel ──────────────────────────────────────────────
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
            "User Information", 0, 0,
            new Font("Arial", Font.BOLD, 14), new Color(0, 123, 255)));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(usernameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JPanel formBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        formBtnPanel.setBackground(Color.WHITE);

        addButton    = makeButton("Add",    new Color(40,  167,  69));
        updateButton = makeButton("Update", new Color(255, 193,   7));
        deleteButton = makeButton("Delete", new Color(220,  53,  69));
        clearButton  = makeButton("Clear",  new Color(108, 117, 125));

        // Give Update button dark text since it's yellow
        updateButton.setForeground(new Color(33, 33, 33));
        updateButton.addMouseListener(new MouseAdapter() {
            Color bg = new Color(255, 193, 7);
            public void mouseEntered(MouseEvent e) { updateButton.setBackground(bg.darker()); updateButton.setForeground(new Color(33,33,33)); }
            public void mouseExited(MouseEvent e)  { updateButton.setBackground(bg);          updateButton.setForeground(new Color(33,33,33)); }
        });

        addButton.addActionListener(e    -> addUser());
        updateButton.addActionListener(e -> updateUser());
        deleteButton.addActionListener(e -> deleteUser());
        clearButton.addActionListener(e  -> clearForm());

        formBtnPanel.add(addButton);
        formBtnPanel.add(updateButton);
        formBtnPanel.add(deleteButton);
        formBtnPanel.add(clearButton);
        formPanel.add(formBtnPanel, gbc);
        add(formPanel, BorderLayout.NORTH);

        // ── Table Panel ──────────────────────────────────────────────
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
            "User List", 0, 0,
            new Font("Arial", Font.BOLD, 14), new Color(0, 123, 255)));

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        refreshPanel.setBackground(Color.WHITE);
        refreshButton = makeButton("Refresh", new Color(108, 117, 125));
        refreshButton.addActionListener(e -> loadUsers());
        refreshPanel.add(refreshButton);
        tablePanel.add(refreshPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Username", "Password"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(tableModel);
        userTable.setFont(new Font("Arial", Font.PLAIN, 12));
        userTable.setRowHeight(25);
        userTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        userTable.getTableHeader().setBackground(new Color(0, 123, 255));
        userTable.getTableHeader().setForeground(Color.WHITE);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { selectUser(); }
        });

        tablePanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    private void loadUsers() {
        tableModel.setRowCount(0);
        for (User u : userDAO.getAllUsers()) {
            tableModel.addRow(new Object[]{u.getId(), u.getUsername(), "********"});
        }
        clearForm();
    }

    private void addUser() {
        if (!validateInput()) return;
        User u = new User(usernameField.getText().trim(), new String(passwordField.getPassword()));
        if (userDAO.createUser(u)) {
            JOptionPane.showMessageDialog(this, "User added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add user! Username might already exist.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to update!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!validateInput()) return;
        User u = new User(selectedUserId, usernameField.getText().trim(), new String(passwordField.getPassword()));
        if (userDAO.updateUser(u)) {
            JOptionPane.showMessageDialog(this, "User updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update user!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (userDAO.deleteUser(selectedUserId)) {
                JOptionPane.showMessageDialog(this, "User deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete user!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void selectUser() {
        int row = userTable.getSelectedRow();
        if (row != -1) {
            selectedUserId = (int) tableModel.getValueAt(row, 0);
            usernameField.setText((String) tableModel.getValueAt(row, 1));
            User u = userDAO.getUserById(selectedUserId);
            if (u != null) passwordField.setText(u.getPassword());
        }
    }

    private void clearForm() {
        usernameField.setText("");
        passwordField.setText("");
        selectedUserId = -1;
        userTable.clearSelection();
    }

    private boolean validateInput() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty()) { JOptionPane.showMessageDialog(this, "Please enter username!", "Validation Error", JOptionPane.ERROR_MESSAGE); usernameField.requestFocus(); return false; }
        if (password.isEmpty()) { JOptionPane.showMessageDialog(this, "Please enter password!", "Validation Error", JOptionPane.ERROR_MESSAGE); passwordField.requestFocus(); return false; }
        if (username.length() > 50) { JOptionPane.showMessageDialog(this, "Username must be 50 characters or less!", "Validation Error", JOptionPane.ERROR_MESSAGE); usernameField.requestFocus(); return false; }
        return true;
    }
}