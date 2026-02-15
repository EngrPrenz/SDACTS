<?php
session_start();
require 'config.php';

$username = $_POST['username'];
$password = $_POST['password'];

$sql = "SELECT * FROM users WHERE username = ? AND password = ?";
$params = [$username, $password];

$stmt = sqlsrv_query($conn, $sql, $params);

if ($stmt && sqlsrv_has_rows($stmt)) {
    $_SESSION['username'] = $username;
    header("Location: main.php");
    exit();
} else {
    echo "Invalid username or password";
}
?>
