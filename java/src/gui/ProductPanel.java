package gui;

import dao.ProductDAO;
import model.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * ProductPanel - Complete product management interface
 * 
 * This panel provides full CRUD (Create, Read, Update, Delete) operations
 * for products, plus a search feature. It's embedded in MainFrame as a tab.
 * 
 * Layout structure:
 * ┌─────────────────────────────────────────┐
 * │ Product Information (Form)              │
 * │  Name:  [_______________]               │
 * │  Price: [_______________]               │
 * │  [Add] [Update] [Delete] [Clear]        │  ← Form section (NORTH)
 * ├─────────────────────────────────────────┤
 * │ Product List (Table)                    │
 * │  Search: [________] [Search] [Refresh]  │
 * │  ┌───┬──────────────┬────────┐          │
 * │  │ID │ Product Name │ Price  │          │  ← Table section (CENTER)
 * │  ├───┼──────────────┼────────┤          │
 * │  │ 1 │ Laptop       │$999.99 │          │
 * │  └───┴──────────────┴────────┘          │
 * └─────────────────────────────────────────┘
 * 
 * User workflow:
 * 1. View all products in table (loaded automatically)
 * 2. Add new product: fill form, click "Add"
 * 3. Update product: click row in table, modify form, click "Update"
 * 4. Delete product: click row in table, click "Delete", confirm
 * 5. Search products: type in search box, click "Search"
 * 6. Clear form: click "Clear" to reset everything
 * 7. Refresh list: click "Refresh" to reload from database
 */
public class ProductPanel extends JPanel {
    
    // ================================================================
    // COMPONENT DECLARATIONS
    // ================================================================
    
    /**
     * DAO for all product database operations
     */
    private ProductDAO productDAO;
    
    /**
     * Table displaying the list of products
     * Shows ID, Name, and formatted Price ($XX.XX) for each product
     */
    private JTable productTable;
    
    /**
     * Table model - manages the data in productTable
     * Allows us to add/remove/modify rows programmatically
     */
    private DefaultTableModel tableModel;
    
    /**
     * Form input fields
     */
    private JTextField nameField;   // Product name input
    private JTextField priceField;  // Product price input
    private JTextField searchField; // Search box (filters products by name)
    
    /**
     * Action buttons
     */
    private JButton addButton;     // Creates new product
    private JButton updateButton;  // Modifies selected product
    private JButton deleteButton;  // Removes selected product
    private JButton clearButton;   // Resets form and selection
    private JButton refreshButton; // Reloads data from database
    private JButton searchButton;  // Filters products by search term
    
    /**
     * Tracks which product is currently selected in the table
     * - Value of -1 means no product is selected
     * - Any other value is the ID of the selected product
     * 
     * Used by Update and Delete operations to know which product to modify/remove.
     */
    private int selectedProductId = -1;

    // ================================================================
    // CONSTRUCTOR
    // ================================================================
    
    /**
     * Constructor - Creates the product management panel
     * 
     * Initialization sequence:
     * 1. Create ProductDAO instance
     * 2. Build the entire UI (initComponents)
     * 3. Load all products from database (loadProducts)
     * 
     * When this panel is created (embedded in MainFrame's tab),
     * the table automatically shows all existing products.
     */
    public ProductPanel() {
        productDAO = new ProductDAO();  // Create DAO for database operations
        initComponents();                // Build the UI
        loadProducts();                  // Populate table with database data
    }

    // ================================================================
    // UI CONSTRUCTION METHODS
    // ================================================================
    
    /**
     * makeButton() - Creates a styled button with custom colors
     * 
     * This method works the same as LoginFrame.makeButton() - it overrides
     * paintComponent() to bypass Windows Look & Feel and ensure our button
     * colors are always visible.
     * 
     * @param text Button label
     * @param bg   Background color
     * @return Fully configured button with hover effects
     * 
     * Special handling for yellow (Update) button:
     * - Yellow background needs DARK text (not white) for readability
     * - This is handled in initComponents() after button creation
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
        
        // Add hover effect
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.darker()); btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(bg);         btn.setForeground(Color.WHITE); }
        });
        return btn;
    }

    /**
     * initComponents() - Builds the complete product management UI
     * 
     * Creates two main sections:
     * 1. Form Panel (NORTH) - Input fields and CRUD buttons
     * 2. Table Panel (CENTER) - Product list with search
     * 
     * See class-level comment for visual layout diagram.
     */
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ══ FORM PANEL (NORTH) ══════════════════════════════════════
        // Contains input fields and action buttons
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
            "Product Information", 0, 0,
            new Font("Arial", Font.BOLD, 14), new Color(0, 123, 255)));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Product Name label and field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        nameField = new JTextField(20);
        nameField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(nameField, gbc);

        // Price label and field
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(priceLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        priceField = new JTextField(20);
        priceField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(priceField, gbc);

        // Button panel
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JPanel formBtnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        formBtnPanel.setBackground(Color.WHITE);

        // Create buttons with appropriate colors
        addButton    = makeButton("Add",    new Color(40,  167,  69));  // Green
        updateButton = makeButton("Update", new Color(255, 193,   7));  // Yellow
        deleteButton = makeButton("Delete", new Color(220,  53,  69));  // Red
        clearButton  = makeButton("Clear",  new Color(108, 117, 125));  // Gray

        // Special handling for yellow Update button - needs dark text
        updateButton.setForeground(new Color(33, 33, 33));  // Dark gray text
        updateButton.addMouseListener(new MouseAdapter() {
            Color bg = new Color(255, 193, 7);
            // Keep text dark even on hover
            public void mouseEntered(MouseEvent e) { updateButton.setBackground(bg.darker()); updateButton.setForeground(new Color(33,33,33)); }
            public void mouseExited(MouseEvent e)  { updateButton.setBackground(bg);          updateButton.setForeground(new Color(33,33,33)); }
        });

        // Attach event listeners - use lambda syntax for cleaner code
        // Lambda (e -> method()) is shorthand for: new ActionListener() { public void actionPerformed(ActionEvent e) { method(); } }
        addButton.addActionListener(e    -> addProduct());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());
        clearButton.addActionListener(e  -> clearForm());

        formBtnPanel.add(addButton);
        formBtnPanel.add(updateButton);
        formBtnPanel.add(deleteButton);
        formBtnPanel.add(clearButton);
        
        formPanel.add(formBtnPanel, gbc);
        add(formPanel, BorderLayout.NORTH);

        // ══ TABLE PANEL (CENTER) ════════════════════════════════════
        // Contains search bar and product table
        
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
            "Product List", 0, 0,
            new Font("Arial", Font.BOLD, 14), new Color(0, 123, 255)));

        // Search panel at top of table section
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBackground(Color.WHITE);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        searchPanel.add(searchLabel);

        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 13));
        searchPanel.add(searchField);

        searchButton  = makeButton("Search",  new Color(23, 162, 184));  // Teal
        refreshButton = makeButton("Refresh", new Color(108, 117, 125)); // Gray
        
        searchButton.addActionListener(e  -> searchProducts());
        refreshButton.addActionListener(e -> loadProducts());

        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);
        tablePanel.add(searchPanel, BorderLayout.NORTH);

        // ── Product Table ───────────────────────────────────────────
        
        // Define column names
        String[] cols = {"ID", "Product Name", "Price"};
        
        // Create table model with custom behavior
        tableModel = new DefaultTableModel(cols, 0) {
            /**
             * Override isCellEditable to make table read-only
             * Users cannot edit cells directly - they must use the form
             * This prevents data corruption and ensures validation
             */
            public boolean isCellEditable(int r, int c) { 
                return false;  // All cells are non-editable
            }
        };
        
        // Create table with the model
        productTable = new JTable(tableModel);
        productTable.setFont(new Font("Arial", Font.PLAIN, 12));
        productTable.setRowHeight(25);
        productTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        productTable.getTableHeader().setBackground(new Color(0, 123, 255));
        productTable.getTableHeader().setForeground(Color.WHITE);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // Only one row selectable at a time
        
        /**
         * Table click listener
         * When user clicks a row, load that product's data into the form
         * This allows them to view/edit/delete the selected product
         */
        productTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { 
                selectProduct();  // Load selected product into form
            }
        });

        // Wrap table in scroll pane (adds scrollbars if needed)
        tablePanel.add(new JScrollPane(productTable), BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    // ════════════════════════════════════════════════════════════════
    // DATA MANAGEMENT METHODS
    // ════════════════════════════════════════════════════════════════
    
    /**
     * loadProducts() - Refreshes the table with all products from database
     * 
     * This method is called:
     * - When panel is first created (constructor)
     * - After adding a product
     * - After updating a product
     * - After deleting a product
     * - When user clicks "Refresh" button
     * 
     * Process:
     * 1. Clear all rows from table
     * 2. Get fresh product list from database
     * 3. Add each product as a row in the table
     * 4. Format price as currency ($XX.XX)
     * 5. Clear form fields and selection
     * 
     * Why clear the form?
     * - After any operation, user likely wants to start fresh
     * - Prevents confusion about what's in the form
     * - Deselects any selected product
     */
    private void loadProducts() {
        // Step 1: Clear existing table data
        tableModel.setRowCount(0);  // Remove all rows
        
        // Step 2: Get all products from database
        List<Product> products = productDAO.getAllProducts();
        
        // Step 3: Add each product as a table row
        for (Product p : products) {
            // Create row array: [ID, Name, FormattedPrice]
            Object[] row = {
                p.getId(),                              // Column 0: ID
                p.getName(),                            // Column 1: Name
                String.format("$%.2f", p.getPrice())    // Column 2: Price (formatted as $XX.XX)
            };
            tableModel.addRow(row);  // Add row to table
        }
        
        // Step 4: Clear form and selection
        clearForm();
    }

    /**
     * addProduct() - Creates a new product in the database
     * 
     * Called when user clicks the "Add" button.
     * 
     * Process:
     * 1. Validate form input (name and price filled, price valid)
     * 2. If invalid, show error and stop
     * 3. If valid, create Product object from form data
     * 4. Call productDAO.createProduct() to insert into database
     * 5. If successful, show success message and refresh table
     * 6. If failed, show error message
     * 
     * Validation performed by validateInput():
     * - Name field is not empty
     * - Price field is not empty
     * - Price is a valid number
     * - Price is greater than 0
     */
    private void addProduct() {
        // Step 1: Validate input
        if (!validateInput()) return;  // Stop if validation fails
        
        // Step 2: Get values from form
        String name = nameField.getText().trim();
        BigDecimal price = new BigDecimal(priceField.getText().trim());
        
        // Step 3: Create Product object
        // Note: We don't set ID - database will auto-generate it
        Product p = new Product(name, price);
        
        // Step 4: Insert into database
        if (productDAO.createProduct(p)) {
            // ── SUCCESS ──
            JOptionPane.showMessageDialog(this, 
                "Product added successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            loadProducts();  // Refresh table to show new product
        } else {
            // ── FAILURE ──
            JOptionPane.showMessageDialog(this, 
                "Failed to add product!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * updateProduct() - Modifies an existing product
     * 
     * Called when user clicks the "Update" button.
     * 
     * Prerequisites:
     * - A product must be selected (clicked in table)
     * - selectedProductId must not be -1
     * 
     * Process:
     * 1. Check if a product is selected
     * 2. Validate new form input
     * 3. Create Product object with selected ID and new data
     * 4. Call productDAO.updateProduct() to modify in database
     * 5. Show success/error message
     * 6. Refresh table if successful
     */
    private void updateProduct() {
        // Step 1: Check if a product is selected
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a product to update!", 
                "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;  // Stop here - can't update without selection
        }
        
        // Step 2: Validate input
        if (!validateInput()) return;
        
        // Step 3: Get new values from form
        String name = nameField.getText().trim();
        BigDecimal price = new BigDecimal(priceField.getText().trim());
        
        // Step 4: Create Product object with selected ID and new data
        Product p = new Product(selectedProductId, name, price);
        
        // Step 5: Update in database
        if (productDAO.updateProduct(p)) {
            // ── SUCCESS ──
            JOptionPane.showMessageDialog(this, 
                "Product updated successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
            loadProducts();  // Refresh table to show updated data
        } else {
            // ── FAILURE ──
            JOptionPane.showMessageDialog(this, 
                "Failed to update product!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * deleteProduct() - Removes a product from the database
     * 
     * Called when user clicks the "Delete" button.
     * 
     * Safety feature:
     * Shows confirmation dialog before deletion to prevent accidents.
     * User must explicitly click "Yes" to proceed.
     * 
     * Process:
     * 1. Check if a product is selected
     * 2. Show confirmation dialog
     * 3. If user confirms, call productDAO.deleteProduct()
     * 4. Show success/error message
     * 5. Refresh table if successful
     */
    private void deleteProduct() {
        // Step 1: Check if a product is selected
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a product to delete!", 
                "Warning", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Step 2: Show confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this product?", 
            "Confirm Delete", 
            JOptionPane.YES_NO_OPTION);
        
        // Step 3: Check user's response
        if (confirm == JOptionPane.YES_OPTION) {
            // User clicked "Yes" - proceed with deletion
            
            if (productDAO.deleteProduct(selectedProductId)) {
                // ── SUCCESS ──
                JOptionPane.showMessageDialog(this, 
                    "Product deleted successfully!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                loadProducts();  // Refresh table (deleted product is gone)
            } else {
                // ── FAILURE ──
                JOptionPane.showMessageDialog(this, 
                    "Failed to delete product!", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
        // If user clicked "No", do nothing (cancel deletion)
    }

    /**
     * searchProducts() - Filters products by name
     * 
     * Called when user clicks the "Search" button.
     * 
     * Search behavior:
     * - Case-insensitive
     * - Partial match (searching "lap" finds "Laptop")
     * - If search box is empty, shows all products (same as Refresh)
     * - If no matches found, shows "No products found!" message
     * 
     * Process:
     * 1. Get search term from search field
     * 2. If empty, call loadProducts() to show all
     * 3. If not empty, call productDAO.searchProductsByName()
     * 4. Display matching products in table
     * 5. If no matches, show info message
     */
    private void searchProducts() {
        // Step 1: Get search term
        String term = searchField.getText().trim();
        
        // Step 2: If empty, show all products
        if (term.isEmpty()) { 
            loadProducts();  // Load everything
            return; 
        }
        
        // Step 3: Clear table and search database
        tableModel.setRowCount(0);
        List<Product> results = productDAO.searchProductsByName(term);
        
        // Step 4: Display results
        for (Product p : results) {
            Object[] row = {
                p.getId(), 
                p.getName(), 
                String.format("$%.2f", p.getPrice())
            };
            tableModel.addRow(row);
        }
        
        // Step 5: If no results, notify user
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No products found!", 
                "Search Result", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * selectProduct() - Loads selected table row into the form
     * 
     * Called when user clicks a row in the product table.
     * 
     * Purpose:
     * - Allows user to view product details
     * - Prepares for Update operation (modify form, click Update)
     * - Prepares for Delete operation (click Delete)
     * 
     * Process:
     * 1. Get the index of the clicked row
     * 2. Extract ID, Name, and Price from that row
     * 3. Store ID in selectedProductId
     * 4. Populate form fields with Name and Price
     * 
     * Note on price:
     * - Table shows price as "$999.99"
     * - Form needs just "999.99" (no $ symbol)
     * - We remove the $ before putting it in the field
     */
    private void selectProduct() {
        // Step 1: Get selected row index
        int row = productTable.getSelectedRow();
        
        // Step 2: Check if a row is actually selected
        if (row != -1) {
            // Step 3: Extract data from the selected row
            selectedProductId = (int) tableModel.getValueAt(row, 0);      // Column 0: ID
            String name = (String) tableModel.getValueAt(row, 1);         // Column 1: Name
            String priceStr = (String) tableModel.getValueAt(row, 2);     // Column 2: Price (with $)
            
            // Step 4: Populate form fields
            nameField.setText(name);
            priceField.setText(priceStr.replace("$", ""));  // Remove $ for form input
        }
    }

    /**
     * clearForm() - Resets all form fields and clears selection
     * 
     * Called:
     * - After loadProducts() (to start fresh)
     * - When user clicks "Clear" button
     * - After any successful add/update/delete operation
     * 
     * What it clears:
     * - Name field
     * - Price field
     * - Search field
     * - Selected product (selectedProductId = -1)
     * - Table row selection (visual highlighting removed)
     * 
     * Why clear selectedProductId?
     * - Prevents accidental Update/Delete on wrong product
     * - Update/Delete buttons will show "Please select" message if clicked
     */
    private void clearForm() {
        nameField.setText("");           // Clear name input
        priceField.setText("");          // Clear price input
        searchField.setText("");         // Clear search box
        selectedProductId = -1;          // No product selected
        productTable.clearSelection();   // Remove table highlighting
    }

    /**
     * validateInput() - Checks if form input is valid
     * 
     * Called by addProduct() and updateProduct() before saving to database.
     * 
     * Validation rules:
     * 1. Name field cannot be empty
     * 2. Price field cannot be empty
     * 3. Price must be a valid number (can parse to BigDecimal)
     * 4. Price must be greater than 0
     * 
     * If any rule fails:
     * - Shows appropriate error message
     * - Sets focus to the problematic field
     * - Returns false (caller should stop operation)
     * 
     * @return true if all validation passes, false if any fails
     * 
     * Why validate here instead of in DAO?
     * - Provides immediate feedback to user
     * - Prevents unnecessary database calls
     * - Shows field-specific error messages
     * - DAO should assume data is already validated
     */
    private boolean validateInput() {
        String name  = nameField.getText().trim();
        String price = priceField.getText().trim();
        
        // Rule 1: Name cannot be empty
        if (name.isEmpty())  { 
            JOptionPane.showMessageDialog(this, 
                "Please enter product name!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE); 
            nameField.requestFocus();  // Put cursor in name field
            return false; 
        }
        
        // Rule 2: Price cannot be empty
        if (price.isEmpty()) { 
            JOptionPane.showMessageDialog(this, 
                "Please enter price!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE); 
            priceField.requestFocus();  // Put cursor in price field
            return false; 
        }
        
        // Rules 3 & 4: Price must be valid number and > 0
        try {
            BigDecimal priceValue = new BigDecimal(price);
            
            // Check if price is positive
            if (priceValue.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, 
                    "Price must be greater than 0!", 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
                priceField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            // Price is not a valid number
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid price!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            priceField.requestFocus();
            return false;
        }
        
        // All validations passed
        return true;
    }
}