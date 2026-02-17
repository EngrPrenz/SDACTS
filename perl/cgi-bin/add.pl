sub add_product {
    my ($name, $price, $qty) = @_;

    my $dbh = DBI->connect("DBI:ODBC:SQLSERVER_DSN","sa","yourpassword",
        { RaiseError => 1, AutoCommit => 1 });

    my $sth = $dbh->prepare(
        "INSERT INTO products (name, price, quantity) VALUES (?, ?, ?)"
    );

    $sth->execute($name, $price, $qty);

    print "Product added successfully!\n";

    $dbh->disconnect;
}

add_product("Keyboard", 1200, 10);
