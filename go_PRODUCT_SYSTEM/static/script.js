// ── Helpers ──────────────────────────────────────────────────────────────────

async function apiFetch(url, options = {}) {
    const res = await fetch(url, options);
    if (res.status === 401) {
        // Session expired or not logged in → go back to login
        window.location.href = '/login';
        return null;
    }
    return res;
}

async function logout() {
    await fetch('/api/logout');
    window.location.href = '/login';
}

// ── Boot ─────────────────────────────────────────────────────────────────────

loadProducts();

// ── Add product ───────────────────────────────────────────────────────────────

document.getElementById('addForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const product = {
        name:     document.getElementById('productName').value,
        price:    parseFloat(document.getElementById('productPrice').value),
        quantity: parseInt(document.getElementById('productQuantity').value)
    };

    const res = await apiFetch('/api/products/add', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(product)
    });
    if (!res) return;

    const result = await res.json();
    if (result.success) {
        showMessage('Product added successfully!', 'success');
        document.getElementById('addForm').reset();
        loadProducts();
    } else {
        showMessage('Error adding product: ' + result.error, 'error');
    }
});

// ── Search ────────────────────────────────────────────────────────────────────

document.getElementById('searchInput').addEventListener('input', (e) => {
    const term = e.target.value;
    if (term.length > 0) {
        searchProducts(term);
    } else {
        loadProducts();
    }
});

// ── Load / Search ─────────────────────────────────────────────────────────────

async function loadProducts() {
    const res = await apiFetch('/api/products');
    if (!res) return;
    const products = await res.json();
    displayProducts(products);
    updateStats(products);
}

async function searchProducts(term) {
    const res = await apiFetch('/api/products/search?q=' + encodeURIComponent(term));
    if (!res) return;
    const products = await res.json();
    displayProducts(products);
}

// ── Display ───────────────────────────────────────────────────────────────────

function displayProducts(products) {
    const tbody = document.getElementById('productsTable');

    if (!products || products.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;">No products found</td></tr>';
        return;
    }

    tbody.innerHTML = products.map(p => {
        const totalValue = (p.price * p.quantity).toFixed(2);
        return `
            <tr>
                <td>${p.id}</td>
                <td>${p.name}</td>
                <td>$${p.price.toFixed(2)}</td>
                <td>${p.quantity}</td>
                <td>$${totalValue}</td>
                <td class="actions">
                    <button class="btn-edit"
                        onclick="editProduct(${p.id}, '${p.name.replace(/'/g, "\\'")}', ${p.price}, ${p.quantity})">
                        Edit
                    </button>
                    <button class="btn-delete"
                        onclick="deleteProduct(${p.id}, '${p.name.replace(/'/g, "\\'")}')">
                        Delete
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function updateStats(products) {
    if (!products) products = [];
    const totalValue = products.reduce((sum, p) => sum + (p.price * p.quantity), 0);
    document.getElementById('totalProducts').textContent = products.length;
    document.getElementById('totalValue').textContent    = '$' + totalValue.toFixed(2);
}

// ── Edit / Delete ─────────────────────────────────────────────────────────────

async function editProduct(id, name, price, quantity) {
    const newName     = prompt('Enter new name:', name);
    if (newName === null) return;
    const newPrice    = parseFloat(prompt('Enter new price:', price));
    if (isNaN(newPrice)) return;
    const newQuantity = parseInt(prompt('Enter new quantity:', quantity));
    if (isNaN(newQuantity)) return;

    const res = await apiFetch('/api/products/update', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ id, name: newName, price: newPrice, quantity: newQuantity })
    });
    if (!res) return;

    const result = await res.json();
    if (result.success) {
        showMessage('Product updated successfully!', 'success');
        loadProducts();
    } else {
        showMessage('Error updating product: ' + result.error, 'error');
    }
}

async function deleteProduct(id, name) {
    if (!confirm(`Are you sure you want to delete "${name}"?`)) return;

    const res = await apiFetch('/api/products/delete', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ id })
    });
    if (!res) return;

    const result = await res.json();
    if (result.success) {
        showMessage('Product deleted successfully!', 'success');
        loadProducts();
    } else {
        showMessage('Error deleting product: ' + result.error, 'error');
    }
}

// ── Message ───────────────────────────────────────────────────────────────────

function showMessage(text, type) {
    const msg = document.getElementById('message');
    msg.textContent  = text;
    msg.className    = 'message ' + type;
    msg.style.display = 'block';
    setTimeout(() => { msg.style.display = 'none'; }, 3000);
}