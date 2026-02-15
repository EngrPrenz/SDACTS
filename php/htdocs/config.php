<?php
$serverName = "ACER-NITROV15-F\SQLEXPRESS"; 
$connectionOptions = [
    "Database" => "SampleDB",
    "Uid" => "",
    "PWD" => ""
];

$conn = sqlsrv_connect($serverName, $connectionOptions);

if ($conn === false) {
    die(print_r(sqlsrv_errors(), true));
}
?>
