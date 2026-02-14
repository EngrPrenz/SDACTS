let selectedId = null;

window.onload = () => {
    if (!localStorage.getItem("user")) {
        window.location.href = "login.html";
    } else {
        loadProducts();
    }
};

async function loadProducts() {
    const res = await fetch("/products");
    const data = await res.json();

    const tbody = document.querySelector("#productTable tbody");
    tbody.innerHTML = "";
    data.forEach(p => {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${p.Id}</td><td>${p.Name}</td><td>${p.Price}</td>`;
        tr.onclick = () => selectProduct(p);
        tbody.appendChild(tr);
    });
}

function selectProduct(product) {
    selectedId = product.Id;
    document.getElementById("name").value = product.Name;
    document.getElementById("price").value = product.Price;
}

function clearForm() {
    selectedId = null;
    document.getElementById("name").value = "";
    document.getElementById("price").value = "";
}

async function addProduct() {
    const name = document.getElementById("name").value;
    const price = document.getElementById("price").value;
    if (!name || !price) return alert("Fill all fields");
    await fetch("/add", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, price })
    });
    clearForm();
    loadProducts();
}

async function updateProduct() {
    if (!selectedId) return alert("Select a product first");
    const name = document.getElementById("name").value;
    const price = document.getElementById("price").value;
    await fetch(`/update/${selectedId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, price })
    });
    clearForm();
    loadProducts();
}

async function deleteProduct() {
    if (!selectedId) return alert("Select a product first");
    await fetch(`/delete/${selectedId}`, { method: "DELETE" });
    clearForm();
    loadProducts();
}

function logout() {
    localStorage.removeItem("user");
    window.location.href = "login.html";
}

async function searchProduct() {
    const searchText = document.getElementById("searchText").value.toLowerCase();
    const res = await fetch("/products");
    const data = await res.json();
    const filtered = data.filter(p => p.Name.toLowerCase().includes(searchText));

    const tbody = document.querySelector("#productTable tbody");
    tbody.innerHTML = "";
    filtered.forEach(p => {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${p.Id}</td><td>${p.Name}</td><td>${p.Price}</td>`;
        tr.onclick = () => selectProduct(p);
        tbody.appendChild(tr);
    });
}
