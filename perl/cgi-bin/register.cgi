#!C:/Strawberry/perl/bin/perl.exe
use strict;
use warnings;
use CGI;
use DBI;
use Crypt::Bcrypt qw(bcrypt);
use Crypt::URandom qw(urandom);

my $cgi = CGI->new;

sub redirect_to {
    my ($url) = @_;
    print $cgi->header(-location => $url, -status => '302 Found');
    exit;
}

my $dsn = "DBI:ODBC:Driver={SQL Server};Server=ACER-NITROV15-F\\SQLEXPRESS;Database=SampleDB;Trusted_Connection=Yes;";
my $dbh = DBI->connect($dsn, '', '', { RaiseError => 0, AutoCommit => 1 });

my $username = $cgi->param('username') // '';
my $password = $cgi->param('password') // '';

unless ($username && $password) {
    redirect_to('/product-system/register.html?error=1');
}

my $check = $dbh->prepare("SELECT COUNT(*) FROM [Users] WHERE Username = ?");
$check->execute($username);
my ($exists) = $check->fetchrow_array;

if ($exists && $exists > 0) {
    redirect_to('/product-system/register.html?error=2');
}

my $salt   = urandom(16);
my $hashed = bcrypt($password, '2b', 10, $salt);

my $ins = $dbh->prepare("INSERT INTO [Users] (Username, Password) VALUES (?, ?)");
$ins->execute($username, $hashed);

redirect_to('/product-system/login.html?registered=1');