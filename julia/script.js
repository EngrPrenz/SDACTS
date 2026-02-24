// ── Auth Helpers ──────────────────────────────────────────────────────────────

async function apiFetch(url, options = {}) {
    const res = await fetch(url, options);
    if (res.status === 401) {
        window.location.href = '/login';
        return null;
    }
    return res;
}

async function logout() {
    await fetch('/api/logout');
    window.location.href = '/login';
}

// ── Boot ──────────────────────────────────────────────────────────────────────

window.onload = function () {
    loadProducts();
};

// ── Add Product ───────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', function () {

    // Add product form
    document.getElementById('addForm').addEventListener('submit', async function (e) {
        e.preventDefault();

        const name  = document.getElementById('productName').value.trim();
        const price = parseFloat(document.getElementById('productPrice').value);

        if (!name) { showMessage('Product name is required', 'error'); return; }
        if (isNaN(price) || price < 0) { showMessage('Enter a valid price', 'error'); return; }

        const res = await apiFetch('/api/products/add', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ name, price })
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

    // Edit product form (modal)
    document.getElementById('editProductForm').addEventListener('submit', async function (e) {
        e.preventDefault();

        const id    = parseInt(document.getElementById('editProductId').value);
        const name  = document.getElementById('editProductName').value.trim();
        const price = parseFloat(document.getElementById('editProductPrice').value);

        if (!name) { showMessage('Product name is required', 'error'); return; }
        if (isNaN(price) || price < 0) { showMessage('Enter a valid price', 'error'); return; }

        const res = await apiFetch('/api/products/update', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ id, name, price })
        });
        if (!res) return;

        const result = await res.json();
        if (result.success) {
            showMessage('Product updated successfully!', 'success');
            closeEditModal();
            loadProducts();
        } else {
            showMessage('Error updating product: ' + result.error, 'error');
        }
    });

    // Search input — live search as you type
    document.getElementById('searchInput').addEventListener('input', function (e) {
        const term = e.target.value.trim();
        term.length > 0 ? searchProducts(term) : loadProducts();
    });

    // Allow Enter key in search box
    document.getElementById('searchInput').addEventListener('keypress', function (e) {
        if (e.key === 'Enter') searchProducts(document.getElementById('searchInput').value.trim());
    });

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
    if (!term) { loadProducts(); return; }
    const res = await apiFetch('/api/products/search?q=' + encodeURIComponent(term));
    if (!res) return;
    const products = await res.json();
    displayProducts(products);
    updateStats(products);
    if (products.length === 0) showMessage(`No products found for "${term}"`, 'error');
}

// ── Display ───────────────────────────────────────────────────────────────────

function displayProducts(products) {
    const tbody = document.getElementById('productsTable');

    if (!products || products.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:30px;">No products available</td></tr>';
        return;
    }

    tbody.innerHTML = products.map(p => {
        const safeName = p.name.replace(/'/g, "\\'");
        return `
            <tr>
                <td>${p.id}</td>
                <td>${p.name}</td>
                <td>$${parseFloat(p.price).toFixed(2)}</td>
                <td>
                    <div class="actions">
                        <button class="btn-edit"
                            onclick="openEditModal(${p.id}, '${safeName}', ${p.price})">
                            Edit
                        </button>
                        <button class="btn-delete"
                            onclick="deleteProduct(${p.id}, '${safeName}')">
                            Delete
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function updateStats(products) {
    document.getElementById('totalProducts').textContent = products ? products.length : 0;
}

// ── Edit Modal ────────────────────────────────────────────────────────────────

function openEditModal(id, name, price) {
    document.getElementById('editProductId').value     = id;
    document.getElementById('editProductName').value   = name;
    document.getElementById('editProductPrice').value  = price;
    document.getElementById('editModal').style.display = 'block';
}

function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
}

// Close modal when clicking outside it
window.onclick = function (event) {
    const modal = document.getElementById('editModal');
    if (event.target === modal) closeEditModal();
};

// ── Delete ────────────────────────────────────────────────────────────────────

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
    msg.textContent   = text;
    msg.className     = 'message ' + type;
    msg.style.display = 'block';
    setTimeout(() => { msg.style.display = 'none'; }, 3000);
}