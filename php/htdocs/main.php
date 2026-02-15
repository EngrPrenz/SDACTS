<?php
session_start();
require 'config.php';

if (!isset($_SESSION['username'])) {
    header("Location: login.php");
    exit();
}

// Search feature
$search = "";
if (isset($_GET['search'])) {
    $search = $_GET['search'];
    $sql = "SELECT * FROM Products WHERE Name LIKE ?";
    $params = ["%$search%"];
    $stmt = sqlsrv_query($conn, $sql, $params);
} else {
    $sql = "SELECT * FROM Products";
    $stmt = sqlsrv_query($conn, $sql);
}
?>

<!DOCTYPE html>
<html>
<head>
    <title>Main Page - Products</title>
    <style>
        table, th, td { border:1px solid black; border-collapse: collapse; padding: 5px; }
        input { margin: 5px; }
    </style>
</head>
<body>

<h2>Welcome, <?php echo $_SESSION['username']; ?>!</h2>
<a href="logout.php">Logout</a>
<br><br>

<!-- Product Form -->
<form method="POST" action="process.php">
    <input type="hidden" name="id" id="id">
    <input type="text" name="name" id="name" placeholder="Product Name" required>
    <input type="number" step="0.01" name="price" id="price" placeholder="Price" required>
    <button type="submit" name="add">Add</button>
    <button type="submit" name="update">Update</button>
    <button type="reset" onclick="clearForm()">Clear</button>
</form>

<!-- Search -->
<form method="GET">
    <input type="text" name="search" placeholder="Search..." value="<?php echo $search; ?>">
    <button type="submit">Search</button>
</form>

<!-- Products Table -->
<table>
<tr>
    <th>ID</th>
    <th>Name</th>
    <th>Price</th>
    <th>Action</th>
</tr>

<?php while ($row = sqlsrv_fetch_array($stmt, SQLSRV_FETCH_ASSOC)) { ?>
<tr onclick="fillForm(<?php echo $row['Id']; ?>, '<?php echo addslashes($row['Name']); ?>', '<?php echo $row['Price']; ?>')">
    <td><?php echo $row['Id']; ?></td>
    <td><?php echo $row['Name']; ?></td>
    <td><?php echo $row['Price']; ?></td>
    <td>
        <a href="process.php?delete=<?php echo $row['Id']; ?>" onclick="return confirm('Delete this product?')">Delete</a>
    </td>
</tr>
<?php } ?>

</table>

<script>
// Fill form when clicking a row
function fillForm(id, name, price) {
    document.getElementById('id').value = id;
    document.getElementById('name').value = name;
    document.getElementById('price').value = price;
}

// Clear form
function clearForm() {
    document.getElementById('id').value = '';
    document.getElementById('name').value = '';
    document.getElementById('price').value = '';
}
</script>

</body>
</html>
