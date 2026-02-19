package gui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * UserPanel - Complete user management interface
 * 
 * This panel works exactly like ProductPanel but manages users instead of products.
 * It provides full CRUD operations: Create, Read, Update, Delete.
 * 
 * Key differences from ProductPanel:
 * - No search feature (typically not needed for user management)
 * - Password field instead of price field
 * - Passwords shown as "********" in table for security
 * - When selecting a user, actual password is loaded (for editing)
 * 
 * Layout structure:
 * ┌─────────────────────────────────────────┐
 * │ User Information (Form)                 │
 * │  Username: [_______________]            │
 * │  Password: [_______________]            │
 * │  [Add] [Update] [Delete] [Clear]        │  ← Form section (NORTH)
 * ├─────────────────────────────────────────┤
 * │ User List (Table)               [Refresh]│
 * │  ┌───┬──────────┬──────────┐            │
 * │  │ID │ Username │ Password │            │  ← Table section (CENTER)
 * │  ├───┼──────────┼──────────┤            │
 * │  │ 1 │ admin    │ ********  │            │
 * │  └───┴──────────┴──────────┘            │
 * └─────────────────────────────────────────┘
 * 
 * User workflow:
 * 1. View all users in table (loaded automatically)
 * 2. Add new user: fill form, click "Add"
 * 3. Update user: click row in table, modify form, click "Update"
 * 4. Delete user: click row in table, click "Delete", confirm
 * 5. Clear form: click "Clear" to reset everything
 * 6. Refresh list: click "Refresh" to reload from database
 */
public class UserPanel extends JPanel {
    
    // ================================================================
    // COMPONENT DECLARATIONS
    // ================================================================
    
    private UserDAO userDAO;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextField usernameField;      // Username input (plain text)
    private JPasswordField passwordField;  // Password input (text is masked)
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;
    private JButton refreshButton;
    
    /**
     * Tracks which user is currently selected in the table
     * -1 means no user is selected
     */
    private int selectedUserId = -1;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================
    
    /**
     * Constructor - Creates the user management panel
     * Same initialization sequence as ProductPanel
     */
    public UserPanel() {
        userDAO = new UserDAO();
        initComponents();
        loadUsers();
    }

    // ================================================================
    // UI CONSTRUCTION METHODS
    // ================================================================
    
    /**
     * makeButton() - Creates a styled button with custom colors
     * Identical to ProductPanel.makeButton()
     * See ProductPanel for detailed explanation
     */
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

    /**
     * initComponents() - Builds the complete user management UI
     * Very similar to ProductPanel.initComponents()
     * Main differences: password field instead of price, no search panel
     */
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ══ FORM PANEL (NORTH) ══════════════════════════════════════
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
            "User Information", 0, 0,
            new Font("Arial", Font.BOLD, 14), new Color(0, 123, 255)));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username label and field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(usernameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(usernameField, gbc);

        // Password label and field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);  // Automatically masks input
        passwordField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(passwordField, gbc);

        // Button panel
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JPanel formBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        formBtnPanel.setBackground(Color.WHITE);

        addButton    = makeButton("Add",    new Color(40,  167,  69));
        updateButton = makeButton("Update", new Color(255, 193,   7));
        deleteButton = makeButton("Delete", new Color(220,  53,  69));
        clearButton  = makeButton("Clear",  new Color(108, 117, 125));

        // Yellow button needs dark text for readability
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

        // ══ TABLE PANEL (CENTER) ════════════════════════════════════
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
            "User List", 0, 0,
            new Font("Arial", Font.BOLD, 14), new Color(0, 123, 255)));

        // Refresh button panel (no search for users)
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        refreshPanel.setBackground(Color.WHITE);
        refreshButton = makeButton("Refresh", new Color(108, 117, 125));
        refreshButton.addActionListener(e -> loadUsers());
        refreshPanel.add(refreshButton);
        tablePanel.add(refreshPanel, BorderLayout.NORTH);

        // User table
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

    // ════════════════════════════════════════════════════════════════
    // DATA MANAGEMENT METHODS
    // ════════════════════════════════════════════════════════════════
    
    /**
     * loadUsers() - Refreshes the table with all users from database
     * 
     * Similar to ProductPanel.loadProducts() but:
     * - Uses UserDAO instead of ProductDAO
     * - Shows "********" instead of actual passwords (security)
     * 
     * Why hide passwords in table?
     * - Passwords are sensitive information
     * - Should not be visible to anyone glancing at the screen
     * - User can still view/edit by selecting a row (loads into form)
     */
    private void loadUsers() {
        tableModel.setRowCount(0);
        
        for (User u : userDAO.getAllUsers()) {
            Object[] row = {
                u.getId(), 
                u.getUsername(), 
                "********"  // Always show asterisks, never actual password
            };
            tableModel.addRow(row);
        }
        
        clearForm();
    }

    /**
     * addUser() - Creates a new user in the database
     * 
     * Called when user clicks the "Add" button.
     * 
     * May fail if:
     * - Username already exists (UNIQUE constraint in database)
     * - Validation fails (empty fields, username too long)
     * 
     * See ProductPanel.addProduct() for detailed process explanation.
     */
    private void addUser() {
        if (!validateInput()) return;
        
        User u = new User(
            usernameField.getText().trim(), 
            new String(passwordField.getPassword())
        );
        
        if (userDAO.createUser(u)) {
            JOptionPane.showMessageDialog(this, 
                "User added successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to add user! Username might already exist.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * updateUser() - Modifies an existing user
     * 
     * Called when user clicks the "Update" button.
     * Requires a user to be selected first.
     * 
     * See ProductPanel.updateProduct() for detailed process explanation.
     */
    private void updateUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a user to update!", 
                "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!validateInput()) return;
        
        User u = new User(
            selectedUserId, 
            usernameField.getText().trim(), 
            new String(passwordField.getPassword())
        );
        
        if (userDAO.updateUser(u)) {
            JOptionPane.showMessageDialog(this, 
                "User updated successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Failed to update user!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * deleteUser() - Removes a user from the database
     * 
     * Called when user clicks the "Delete" button.
     * Shows confirmation dialog before deletion.
     * 
     * WARNING: Deleting users is permanent and cannot be undone!
     * 
     * See ProductPanel.deleteProduct() for detailed process explanation.
     */
    private void deleteUser() {
        if (selectedUserId == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a user to delete!", 
                "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this user?", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (userDAO.deleteUser(selectedUserId)) {
                JOptionPane.showMessageDialog(this, 
                    "User deleted successfully!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to delete user!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * selectUser() - Loads selected table row into the form
     * 
     * Called when user clicks a row in the user table.
     * 
     * Important: Unlike the table, the form shows the ACTUAL password
     * - Table shows: ********
     * - Form shows: actualpassword123
     * 
     * Why?
     * - User needs to see current password to edit it
     * - Form is less visible than the table
     * - User is actively working on this specific user
     * 
     * Process:
     * 1. Get selected row index
     * 2. Extract ID and Username from that row
     * 3. Call userDAO.getUserById() to get full User object (with actual password)
     * 4. Populate form fields with username and password
     */
    private void selectUser() {
        int row = userTable.getSelectedRow();
        
        if (row != -1) {
            // Get ID and username from table
            selectedUserId = (int) tableModel.getValueAt(row, 0);
            String username = (String) tableModel.getValueAt(row, 1);
            
            // Load username into form
            usernameField.setText(username);
            
            // Get full User object from database to retrieve password
            // Table only shows "********", so we need to fetch real password
            User u = userDAO.getUserById(selectedUserId);
            if (u != null) {
                passwordField.setText(u.getPassword());  // Show actual password in form
            }
        }
    }

    /**
     * clearForm() - Resets all form fields and clears selection
     * Identical to ProductPanel.clearForm()
     * See ProductPanel for detailed explanation
     */
    private void clearForm() {
        usernameField.setText("");
        passwordField.setText("");
        selectedUserId = -1;
        userTable.clearSelection();
    }

    /**
     * validateInput() - Checks if form input is valid
     * 
     * Validation rules:
     * 1. Username cannot be empty
     * 2. Password cannot be empty
     * 3. Username must be 50 characters or less (database constraint)
     * 
     * @return true if all validation passes, false if any fails
     * 
     * Similar to ProductPanel.validateInput() but with user-specific rules.
     */
    private boolean validateInput() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // Rule 1: Username cannot be empty
        if (username.isEmpty()) { 
            JOptionPane.showMessageDialog(this, 
                "Please enter username!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE); 
            usernameField.requestFocus(); 
            return false; 
        }
        
        // Rule 2: Password cannot be empty
        if (password.isEmpty()) { 
            JOptionPane.showMessageDialog(this, 
                "Please enter password!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE); 
            passwordField.requestFocus(); 
            return false; 
        }
        
        // Rule 3: Username length limit (database constraint)
        if (username.length() > 50) { 
            JOptionPane.showMessageDialog(this, 
                "Username must be 50 characters or less!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE); 
            usernameField.requestFocus(); 
            return false; 
        }
        
        return true;
    }
}