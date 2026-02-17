sub delete_product {
    my ($id) = @_;

    my $dbh = DBI->connect("DBI:ODBC:SQLSERVER_DSN","sa","yourpassword",
        { RaiseError => 1, AutoCommit => 1 });

    my $sth = $dbh->prepare("DELETE FROM products WHERE id=?");
    $sth->execute($id);

    print "Product deleted successfully!\n";

    $dbh->disconnect;
}

delete_product(1);
