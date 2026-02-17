#!C:/Strawberry/perl/bin/perl.exe
use strict;
use warnings;
use CGI;
use CGI::Cookie;
use DBI;

my $cgi = CGI->new;

my $dsn = "DBI:ODBC:Driver={SQL Server};Server=XEVO\\SQLEXPRESS01;Database=ProductDB;Trusted_Connection=Yes;";
my $dbh = DBI->connect($dsn, '', '', { RaiseError => 0, AutoCommit => 1 });

# Get session token from cookie
my %cookies = CGI::Cookie->fetch;
if ($cookies{pms_session}) {
    my $token = $cookies{pms_session}->value;
    $dbh->do("DELETE FROM sessions WHERE token = ?", undef, $token);
}

$dbh->disconnect;

# Expire the cookie
my $expired_cookie = CGI::Cookie->new(
    -name    => 'pms_session',
    -value   => '',
    -expires => '-1d',
    -path    => '/',
);

print $cgi->header(
    -type     => 'text/html',
    -status   => '302 Found',
    -location => '/product-system/login.html',
    -cookie   => $expired_cookie,
);
