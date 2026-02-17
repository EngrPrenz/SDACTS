use strict;
use warnings;
use DBI;

my $dsn = "DBI:ODBC:SQLSERVER_DSN";
my $username = "sa";
my $password = "yourpassword";

my $dbh = DBI->connect($dsn, $username, $password,
    { RaiseError => 1, AutoCommit => 1 })
    or die "Connection failed: $DBI::errstr";

print "Connected successfully!\n";

$dbh->disconnect;
