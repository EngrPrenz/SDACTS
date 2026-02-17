sub edit_product {
    my ($id, $name, $price, $qty) = @_;

    my $dbh = DBI->connect("DBI:ODBC:SQLSERVER_DSN","sa","yourpassword",
        { RaiseError => 1, AutoCommit => 1 });

    my $sth = $dbh->prepare(
        "UPDATE products SET name=?, price=?, quantity=? WHERE id=?"
    );

    $sth->execute($name, $price, $qty, $id);

    print "Product updated successfully!\n";

    $dbh->disconnect;
}

edit_product(1, "Gaming Keyboard", 1500, 15);
