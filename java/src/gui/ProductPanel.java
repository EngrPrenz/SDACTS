package gui;

import dao.ProductDAO;
import model.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.List;

public class ProductPanel extends JPanel {
    private ProductDAO productDAO;
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField nameField;
    private JTextField priceField;
    private JTextField searchField;
    private JButton addButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JButton clearButton;
    private JButton refreshButton;
    private JButton searchButton;
    private int selectedProductId = -1;

    public ProductPanel() {
        productDAO = new ProductDAO();
        initComponents();
        loadProducts();
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
            "Product Information", 0, 0,
            new Font("Arial", Font.BOLD, 14), new Color(0, 123, 255)));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        nameField = new JTextField(20);
        nameField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(priceLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        priceField = new JTextField(20);
        priceField.setFont(new Font("Arial", Font.PLAIN, 13));
        formPanel.add(priceField, gbc);

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

        // ── Table Panel ──────────────────────────────────────────────
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
            "Product List", 0, 0,
            new Font("Arial", Font.BOLD, 14), new Color(0, 123, 255)));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.setBackground(Color.WHITE);

        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        searchPanel.add(searchLabel);

        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 13));
        searchPanel.add(searchField);

        searchButton  = makeButton("Search",  new Color(23, 162, 184));
        refreshButton = makeButton("Refresh", new Color(108, 117, 125));
        searchButton.addActionListener(e  -> searchProducts());
        refreshButton.addActionListener(e -> loadProducts());

        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);
        tablePanel.add(searchPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Product Name", "Price"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        productTable = new JTable(tableModel);
        productTable.setFont(new Font("Arial", Font.PLAIN, 12));
        productTable.setRowHeight(25);
        productTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        productTable.getTableHeader().setBackground(new Color(0, 123, 255));
        productTable.getTableHeader().setForeground(Color.WHITE);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { selectProduct(); }
        });

        tablePanel.add(new JScrollPane(productTable), BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    private void loadProducts() {
        tableModel.setRowCount(0);
        for (Product p : productDAO.getAllProducts()) {
            tableModel.addRow(new Object[]{p.getId(), p.getName(), String.format("$%.2f", p.getPrice())});
        }
        clearForm();
    }

    private void addProduct() {
        if (!validateInput()) return;
        Product p = new Product(nameField.getText().trim(), new BigDecimal(priceField.getText().trim()));
        if (productDAO.createProduct(p)) {
            JOptionPane.showMessageDialog(this, "Product added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadProducts();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add product!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to update!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!validateInput()) return;
        Product p = new Product(selectedProductId, nameField.getText().trim(), new BigDecimal(priceField.getText().trim()));
        if (productDAO.updateProduct(p)) {
            JOptionPane.showMessageDialog(this, "Product updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadProducts();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update product!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProduct() {
        if (selectedProductId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this product?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (productDAO.deleteProduct(selectedProductId)) {
                JOptionPane.showMessageDialog(this, "Product deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadProducts();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete product!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void searchProducts() {
        String term = searchField.getText().trim();
        if (term.isEmpty()) { loadProducts(); return; }
        tableModel.setRowCount(0);
        List<Product> results = productDAO.searchProductsByName(term);
        for (Product p : results) {
            tableModel.addRow(new Object[]{p.getId(), p.getName(), String.format("$%.2f", p.getPrice())});
        }
        if (results.isEmpty())
            JOptionPane.showMessageDialog(this, "No products found!", "Search Result", JOptionPane.INFORMATION_MESSAGE);
    }

    private void selectProduct() {
        int row = productTable.getSelectedRow();
        if (row != -1) {
            selectedProductId = (int) tableModel.getValueAt(row, 0);
            nameField.setText((String) tableModel.getValueAt(row, 1));
            priceField.setText(((String) tableModel.getValueAt(row, 2)).replace("$", ""));
        }
    }

    private void clearForm() {
        nameField.setText("");
        priceField.setText("");
        searchField.setText("");
        selectedProductId = -1;
        productTable.clearSelection();
    }

    private boolean validateInput() {
        String name  = nameField.getText().trim();
        String price = priceField.getText().trim();
        if (name.isEmpty())  { JOptionPane.showMessageDialog(this, "Please enter product name!", "Validation Error", JOptionPane.ERROR_MESSAGE); nameField.requestFocus(); return false; }
        if (price.isEmpty()) { JOptionPane.showMessageDialog(this, "Please enter price!",         "Validation Error", JOptionPane.ERROR_MESSAGE); priceField.requestFocus(); return false; }
        try {
            if (new BigDecimal(price).compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "Price must be greater than 0!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                priceField.requestFocus(); return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid price!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            priceField.requestFocus(); return false;
        }
        return true;
    }
}