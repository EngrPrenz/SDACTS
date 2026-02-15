<?php
require 'config.php';

// ADD
if (isset($_POST['add'])) {
    $sql = "INSERT INTO Products (Name, Price) VALUES (?, ?)";
    $params = [$_POST['name'], $_POST['price']];
    sqlsrv_query($conn, $sql, $params);
    header("Location: main.php");
}

// UPDATE
if (isset($_POST['update'])) {
    $sql = "UPDATE Products SET Name=?, Price=? WHERE Id=?";
    $params = [$_POST['name'], $_POST['price'], $_POST['id']];
    sqlsrv_query($conn, $sql, $params);
    header("Location: main.php");
}

// DELETE
if (isset($_GET['delete'])) {
    $sql = "DELETE FROM Products WHERE Id=?";
    $params = [$_GET['delete']];
    sqlsrv_query($conn, $sql, $params);
    header("Location: main.php");
}
?>
