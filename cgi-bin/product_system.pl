use strict;
use warnings;
use DBI;

my $dsn = "DBI:ODBC:SQLSERVER_DSN";
my $db_user = "sa";
my $db_pass = "yourpassword";

sub connect_db {
    return DBI->connect($dsn, $db_user, $db_pass,
        { RaiseError => 1, AutoCommit => 1 });
}

sub add_product {
    print "Enter product name: ";
    chomp(my $name = <STDIN>);

    print "Enter price: ";
    chomp(my $price = <STDIN>);

    print "Enter quantity: ";
    chomp(my $qty = <STDIN>);

    my $dbh = connect_db();
    my $sth = $dbh->prepare(
        "INSERT INTO products (name, price, quantity) VALUES (?, ?, ?)"
    );
    $sth->execute($name, $price, $qty);

    print "Product added successfully!\n";
    $dbh->disconnect;
}

sub edit_product {
    print "Enter product ID to edit: ";
    chomp(my $id = <STDIN>);

    print "New name: ";
    chomp(my $name = <STDIN>);

    print "New price: ";
    chomp(my $price = <STDIN>);

    print "New quantity: ";
    chomp(my $qty = <STDIN>);

    my $dbh = connect_db();
    my $sth = $dbh->prepare(
        "UPDATE products SET name=?, price=?, quantity=? WHERE id=?"
    );
    $sth->execute($name, $price, $qty, $id);

    print "Product updated successfully!\n";
    $dbh->disconnect;
}

sub delete_product {
    print "Enter product ID to delete: ";
    chomp(my $id = <STDIN>);

    my $dbh = connect_db();
    my $sth = $dbh->prepare("DELETE FROM products WHERE id=?");
    $sth->execute($id);

    print "Product deleted successfully!\n";
    $dbh->disconnect;
}

sub search_product {
    print "Enter search keyword: ";
    chomp(my $keyword = <STDIN>);

    my $dbh = connect_db();
    my $sth = $dbh->prepare(
        "SELECT * FROM products WHERE name LIKE ?"
    );
    $sth->execute("%$keyword%");

    while (my $row = $sth->fetchrow_hashref) {
        print "-----------------\n";
        print "ID: $row->{id}\n";
        print "Name: $row->{name}\n";
        print "Price: $row->{price}\n";
        print "Quantity: $row->{quantity}\n";
    }

    $dbh->disconnect;
}

# MAIN MENU LOOP
while (1) {
    print "\n==== PRODUCT SYSTEM ====\n";
    print "1. Add Product\n";
    print "2. Edit Product\n";
    print "3. Delete Product\n";
    print "4. Search Product\n";
    print "5. Exit\n";
    print "Choose option: ";

    chomp(my $choice = <STDIN>);

    if ($choice == 1) {
        add_product();
    }
    elsif ($choice == 2) {
        edit_product();
    }
    elsif ($choice == 3) {
        delete_product();
    }
    elsif ($choice == 4) {
        search_product();
    }
    elsif ($choice == 5) {
        print "Exiting...\n";
        last;
    }
    else {
        print "Invalid option.\n";
    }
}
