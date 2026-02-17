sub search_product {
    my ($keyword) = @_;

    my $dbh = DBI->connect("DBI:ODBC:SQLSERVER_DSN","sa","yourpassword",
        { RaiseError => 1, AutoCommit => 1 });

    my $sth = $dbh->prepare(
        "SELECT * FROM products WHERE name LIKE ?"
    );

    $sth->execute("%$keyword%");

    while (my $row = $sth->fetchrow_hashref) {
        print "ID: $row->{id}\n";
        print "Name: $row->{name}\n";
        print "Price: $row->{price}\n";
        print "Quantity: $row->{quantity}\n";
        print "------------------\n";
    }

    $dbh->disconnect;
}

search_product("Keyboard");
