package gui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.awt.event.*;

/**
 * LoginFrame - The application's entry point and login screen
 * 
 * This is the first window users see when launching the application.
 * It handles user authentication and opens the main application window
 * upon successful login.
 * 
 * Key responsibilities:
 * - Display username and password input fields
 * - Validate user credentials against the database
 * - Open MainFrame if login succeeds
 * - Show error messages if login fails
 * 
 * This class contains the main() method, making it the application entry point.
 */
public class LoginFrame extends JFrame {
    
    // ================================================================
    // COMPONENT DECLARATIONS
    // ================================================================
    
    /**
     * Text field where user enters their username
     */
    private JTextField usernameField;
    
    /**
     * Password field where user enters their password
     * Text is automatically masked with dots or asterisks for security
     */
    private JPasswordField passwordField;
    
    /**
     * Green "Login" button - triggers authentication
     */
    private JButton loginButton;
    
    /**
     * Red "Cancel" button - exits the application
     */
    private JButton cancelButton;
    
    /**
     * Data Access Object for User operations
     * Used to check login credentials against the database
     */
    private UserDAO userDAO;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================
    
    /**
     * Constructor - Creates the login window
     * 
     * When called, this:
     * 1. Creates a UserDAO instance for database access
     * 2. Calls initComponents() to build the entire UI
     * 
     * The window is created but not visible yet - main() method
     * will call setVisible(true) to show it.
     */
    public LoginFrame() {
        userDAO = new UserDAO();  // Create DAO for database operations
        initComponents();          // Build the UI
    }

    // ================================================================
    // UI CONSTRUCTION METHODS
    // ================================================================
    
    /**
     * makeButton() - Creates a custom-styled button with proper colors
     * 
     * This method creates buttons that bypass Windows Look & Feel issues.
     * Windows themes override button colors, making text invisible. We fix
     * this by overriding paintComponent() to draw our own background.
     * 
     * @param text The button label (e.g., "Login", "Cancel")
     * @param bg   The background color for the button
     * @return Fully configured JButton with custom painting
     * 
     * How the custom painting works:
     * 1. Override paintComponent() to intercept button rendering
     * 2. Use Graphics2D to draw a rounded rectangle with our chosen color
     * 3. Call super.paintComponent() to draw the text on top
     * 4. Result: Button has our color, text is visible
     * 
     * Also adds:
     * - Hover effect (button darkens when mouse enters)
     * - Hand cursor to indicate it's clickable
     * - Proper styling (font, size, no border)
     */
    private JButton makeButton(String text, Color bg) {
        // Create JButton with overridden paintComponent method
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                // Step 1: Get Graphics2D for advanced drawing
                Graphics2D g2 = (Graphics2D) g.create();
                
                // Step 2: Enable anti-aliasing for smooth edges
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Step 3: Draw rounded rectangle with our background color
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Step 4: Clean up Graphics2D object
                g2.dispose();
                
                // Step 5: Let Swing draw the button text on top
                super.paintComponent(g);
            }
        };
        
        // Configure button appearance
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(bg);           // Background color
        btn.setForeground(Color.WHITE);  // Text color (always white)
        btn.setFocusPainted(false);      // Remove focus border
        btn.setBorderPainted(false);     // Remove button border
        btn.setContentAreaFilled(false); // Disable default background painting
        btn.setOpaque(false);            // Make background transparent (we draw it ourselves)
        btn.setPreferredSize(new Dimension(120, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Hand pointer on hover
        
        // Add mouse hover effect
        btn.addMouseListener(new MouseAdapter() {
            /**
             * Mouse enters button - darken the background
             */
            public void mouseEntered(MouseEvent e) { 
                btn.setBackground(bg.darker());    // Make color darker
                btn.setForeground(Color.WHITE);    // Keep text white
            }
            
            /**
             * Mouse leaves button - restore original background
             */
            public void mouseExited(MouseEvent e) { 
                btn.setBackground(bg);             // Restore original color
                btn.setForeground(Color.WHITE);    // Keep text white
            }
        });
        
        return btn;
    }

    /**
     * initComponents() - Builds the entire login window UI
     * 
     * This method constructs every visual element of the login screen:
     * - Window properties (size, title, close behavior)
     * - Main panel with border layout
     * - Title label at the top
     * - Form panel with username and password fields
     * - Button panel at the bottom
     * - Event listeners for buttons and Enter key
     * 
     * Layout structure:
     * ┌───────────────────────────────┐
     * │      System Login             │ ← Title (NORTH)
     * ├───────────────────────────────┤
     * │  Username: [____________]     │
     * │  Password: [____________]     │ ← Form (CENTER)
     * ├───────────────────────────────┤
     * │    [Login]  [Cancel]          │ ← Buttons (SOUTH)
     * └───────────────────────────────┘
     */
    private void initComponents() {
        // ── Window Properties ───────────────────────────────────────
        setTitle("Login - CRUD System");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // Close app when X is clicked
        setLocationRelativeTo(null);  // Center window on screen
        setResizable(false);          // Prevent resizing

        // ── Main Panel ──────────────────────────────────────────────
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));  // Padding
        mainPanel.setBackground(new Color(240, 240, 245));  // Light gray background

        // ── Title Label ─────────────────────────────────────────────
        JLabel titleLabel = new JLabel("System Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(50, 50, 50));  // Dark gray text
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // ── Form Panel ──────────────────────────────────────────────
        // Uses GridBagLayout for flexible positioning of labels and fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(240, 240, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);  // Spacing between components
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username label and field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(usernameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 16));
        usernameField.setPreferredSize(new Dimension(250, 35));
        formPanel.add(usernameField, gbc);

        // Password label and field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
        passwordField.setPreferredSize(new Dimension(250, 35));
        formPanel.add(passwordField, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // ── Button Panel ────────────────────────────────────────────
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        buttonPanel.setBackground(new Color(240, 240, 245));

        // Create buttons with custom styling
        loginButton = makeButton("Login",  new Color(25, 135, 84));   // Green
        cancelButton = makeButton("Cancel", new Color(220, 53, 69));  // Red

        // Attach event listeners
        loginButton.addActionListener(e -> login());       // Call login() when clicked
        cancelButton.addActionListener(e -> System.exit(0)); // Exit app when clicked

        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // ── Keyboard Shortcuts ──────────────────────────────────────
        
        /**
         * Pressing Enter in password field triggers login
         * This provides a keyboard-only login flow:
         * 1. Tab to username field
         * 2. Type username
         * 3. Press Enter (focus moves to password field)
         * 4. Type password
         * 5. Press Enter (triggers login)
         */
        passwordField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) login();
            }
        });
        
        /**
         * Pressing Enter in username field moves focus to password field
         * Allows user to proceed without using mouse
         */
        usernameField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) passwordField.requestFocus();
            }
        });
    }

    // ================================================================
    // BUSINESS LOGIC METHODS
    // ================================================================
    
    /**
     * login() - Handles the login process
     * 
     * This is the core authentication method. It's called when:
     * - User clicks the "Login" button
     * - User presses Enter in the password field
     * 
     * Process flow:
     * 1. Get username and password from input fields
     * 2. Validate that both fields are filled
     * 3. Query database to check if credentials are valid
     * 4. If valid: show success message, open main application, close login window
     * 5. If invalid: show error message, clear password field, refocus
     * 
     * Security considerations:
     * - Never reveals which part is wrong (username vs password)
     * - Always shows generic "Invalid username or password" message
     * - Clears password field after failed attempt
     * - Trims whitespace to prevent confusion
     */
    private void login() {
        // Step 1: Get input from fields and remove leading/trailing spaces
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Step 2: Validate input - check if either field is empty
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both username and password", 
                "Input Error", 
                JOptionPane.ERROR_MESSAGE);
            return;  // Stop here if validation fails
        }

        // Step 3: Attempt authentication against database
        // userDAO.authenticate() returns User object if valid, null if invalid
        User user = userDAO.authenticate(username, password);

        // Step 4: Check authentication result
        if (user != null) {
            // ── SUCCESS PATH ──
            // Valid credentials - user object was returned
            
            // Show success message
            JOptionPane.showMessageDialog(this, 
                "Login successful! Welcome, " + username, 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            
            // Create and show the main application window
            // Pass the User object so MainFrame knows who's logged in
            SwingUtilities.invokeLater(() -> new MainFrame(user).setVisible(true));
            
            // Close the login window (we're done with it)
            dispose();
            
        } else {
            // ── FAILURE PATH ──
            // Invalid credentials - null was returned
            
            // Show error message (intentionally vague for security)
            JOptionPane.showMessageDialog(this, 
                "Invalid username or password", 
                "Login Failed", 
                JOptionPane.ERROR_MESSAGE);
            
            // Clear password field (security best practice)
            passwordField.setText("");
            
            // Set focus back to password field for retry
            passwordField.requestFocus();
        }
    }

    // ================================================================
    // MAIN METHOD - APPLICATION ENTRY POINT
    // ================================================================
    
    /**
     * main() - Application entry point
     * 
     * This is the first method called when you run:
     * java -jar JavaCRUDSystem.jar
     * 
     * What it does:
     * 1. Sets the Look & Feel to cross-platform (Java Metal theme)
     * 2. Creates the LoginFrame on the Event Dispatch Thread (EDT)
     * 3. Makes the login window visible
     * 
     * Why cross-platform Look & Feel?
     * - Windows Look & Feel overrides button colors, making text invisible
     * - Cross-platform L&F lets our custom button painting work correctly
     * - Ensures consistent appearance on all operating systems
     * 
     * Why SwingUtilities.invokeLater()?
     * - All Swing components must be created and modified on the EDT
     * - invokeLater() schedules the code to run on the EDT
     * - Prevents threading issues and ensures GUI responsiveness
     * 
     * The EDT (Event Dispatch Thread):
     * - Special thread that handles all GUI events (clicks, repaints, etc.)
     * - All GUI code must run on this thread to avoid race conditions
     * - SwingUtilities.invokeLater() moves our code to the EDT
     * 
     * @param args Command-line arguments (not used in this application)
     */
    public static void main(String[] args) {
        try {
            // Set Look & Feel to cross-platform (Java Metal theme)
            // This prevents Windows from overriding our button colors
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            
            // Force all buttons to use white text
            UIManager.put("Button.foreground", new ColorUIResource(Color.WHITE));
            
        } catch (Exception e) {
            // If Look & Feel setting fails, just use the default
            // Application will still work, buttons might look different
            e.printStackTrace();
        }
        
        // Create and show the login window on the Event Dispatch Thread
        // Lambda expression is shorthand for: new Runnable() { public void run() { ... } }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}