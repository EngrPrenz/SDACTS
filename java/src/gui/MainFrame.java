package gui;

import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * MainFrame - The main application window
 * 
 * This window appears after successful login and contains the entire
 * application interface. It displays:
 * - Header with app title, username, and logout button
 * - Tabbed interface with Products and Users panels
 * 
 * The user stays in this window until they logout or close the application.
 * 
 * Key responsibilities:
 * - Display current user's name in header
 * - Host ProductPanel and UserPanel in tabs
 * - Handle logout process
 * - Confirm before closing application
 */
public class MainFrame extends JFrame {
    
    // ================================================================
    // COMPONENT DECLARATIONS
    // ================================================================
    
    /**
     * The User object representing who is currently logged in
     * Used to display their username in the header
     */
    private User currentUser;
    
    /**
     * Tabbed pane that holds ProductPanel and UserPanel
     * Allows user to switch between managing products and users
     */
    private JTabbedPane tabbedPane;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================
    
    /**
     * Constructor - Creates the main application window
     * 
     * @param user The User who just logged in successfully
     * 
     * This user object is:
     * - Passed from LoginFrame after successful authentication
     * - Stored in currentUser field
     * - Used to display the username in the header
     * 
     * After storing the user, calls initComponents() to build the UI.
     */
    public MainFrame(User user) {
        this.currentUser = user;  // Store the logged-in user
        initComponents();          // Build the UI
    }

    // ================================================================
    // UI CONSTRUCTION METHODS
    // ================================================================
    
    /**
     * makeButton() - Creates a custom-styled button
     * 
     * Same approach as LoginFrame - overrides paintComponent() to
     * draw the button background ourselves, bypassing Windows Look & Feel.
     * 
     * @param text Button label
     * @param bg   Background color
     * @return Fully configured button with custom painting and hover effects
     * 
     * See LoginFrame.makeButton() for detailed explanation of how
     * the custom painting works.
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
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg);         btn.setForeground(Color.WHITE); }
        });
        return btn;
    }

    /**
     * initComponents() - Builds the main application window UI
     * 
     * Creates the complete interface structure:
     * 
     * ┌──────────────────────────────────────────┐
     * │ CRUD System | Welcome, admin  [Logout]   │ ← Header (NORTH)
     * ├──────────────────────────────────────────┤
     * │ ┌──────────┬──────────┐                  │
     * │ │ Products │  Users   │ ← Tabs           │
     * │ └──────────┴──────────┘                  │
     * │                                           │
     * │  [Selected tab's panel content]          │ ← Tab content (CENTER)
     * │                                           │
     * └──────────────────────────────────────────┘
     * 
     * The header shows:
     * - Application title on the left
     * - Username and logout button on the right
     * 
     * The tabbed pane contains:
     * - ProductPanel - Full product management interface
     * - UserPanel    - Full user management interface
     */
    private void initComponents() {
        // ── Window Properties ───────────────────────────────────────
        setTitle("CRUD Management System - Logged in as: " + currentUser.getUsername());
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);  // Custom close handler (see below)
        setLocationRelativeTo(null);  // Center on screen

        // Main container panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // ── Header Panel ────────────────────────────────────────────
        // Blue bar at the top showing title, username, and logout button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 123, 255));  // Blue background
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Title on the left side of header
        JLabel titleLabel = new JLabel("CRUD Management System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // User info panel on the right side of header
        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        userInfoPanel.setBackground(new Color(0, 123, 255));

        // Display current user's name
        JLabel userLabel = new JLabel("Welcome, " + currentUser.getUsername());
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        userLabel.setForeground(Color.WHITE);
        userInfoPanel.add(userLabel);

        // Logout button
        JButton logoutButton = makeButton("Logout", new Color(220, 53, 69));  // Red
        logoutButton.addActionListener(e -> logout());  // Call logout() when clicked
        userInfoPanel.add(logoutButton);
        
        headerPanel.add(userInfoPanel, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ── Tabbed Pane ─────────────────────────────────────────────
        // Container that holds multiple panels, showing one at a time
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Add ProductPanel as first tab
        // ProductPanel constructor automatically loads all products from database
        tabbedPane.addTab("Products", new ProductPanel());
        
        // Add UserPanel as second tab
        // UserPanel constructor automatically loads all users from database
        tabbedPane.addTab("Users",    new UserPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);

        // ── Window Close Handler ────────────────────────────────────
        /**
         * Custom window close behavior
         * 
         * Instead of closing immediately when user clicks X, we:
         * 1. Show a confirmation dialog
         * 2. Only close if they click "Yes"
         * 3. Stay open if they click "No"
         * 
         * Why DO_NOTHING_ON_CLOSE?
         * - Prevents default close behavior
         * - Lets us show confirmation dialog
         * - Gives user a chance to cancel accidental close
         * 
         * The WindowAdapter.windowClosing() method is called
         * when the user clicks the X button on the window.
         */
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                // Show confirmation dialog
                int confirm = JOptionPane.showConfirmDialog(
                    MainFrame.this,                      // Parent window
                    "Are you sure you want to exit?",    // Message
                    "Exit",                              // Dialog title
                    JOptionPane.YES_NO_OPTION            // Show Yes and No buttons
                );
                
                // Check user's choice
                if (confirm == JOptionPane.YES_OPTION) {
                    System.exit(0);  // Close application
                }
                // If NO_OPTION, do nothing (window stays open)
            }
        });
    }

    // ================================================================
    // BUSINESS LOGIC METHODS
    // ================================================================
    
    /**
     * logout() - Handles the logout process
     * 
     * Called when user clicks the "Logout" button in the header.
     * 
     * Process:
     * 1. Show confirmation dialog ("Are you sure you want to logout?")
     * 2. If user clicks "Yes":
     *    - Close the main application window (dispose())
     *    - Create and show a new LoginFrame
     *    - User is back at login screen
     * 3. If user clicks "No":
     *    - Do nothing (stay in the application)
     * 
     * Why create a NEW LoginFrame?
     * - The old LoginFrame was disposed when login succeeded
     * - We need a fresh LoginFrame for the next login
     * - This also clears any residual data from the old login
     * 
     * Why use SwingUtilities.invokeLater()?
     * - Ensures the new LoginFrame is created on the EDT
     * - Prevents threading issues
     * - See LoginFrame.main() for detailed EDT explanation
     */
    private void logout() {
        // Show confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(
            this,                                 // Parent window
            "Are you sure you want to logout?",   // Message
            "Logout",                             // Dialog title
            JOptionPane.YES_NO_OPTION             // Yes/No buttons
        );
        
        // Check user's response
        if (confirm == JOptionPane.YES_OPTION) {
            // User confirmed logout
            
            // Close this window and release its resources
            dispose();
            
            // Create and show new login window on the EDT
            // Lambda creates new LoginFrame and makes it visible
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
        // If user clicked "No", do nothing (stay logged in)
    }
}