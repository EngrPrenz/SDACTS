#!C:/Strawberry/perl/bin/perl.exe
use strict;
use warnings;
use CGI;
use DBI;
use Crypt::Bcrypt qw(bcrypt);
use Crypt::URandom qw(urandom);

my $cgi = CGI->new;
print $cgi->header('text/html; charset=UTF-8');

my $dsn = "DBI:ODBC:Driver={SQL Server};Server=XEVO\\SQLEXPRESS01;Database=ProductDB;Trusted_Connection=Yes;";
my $dbh = DBI->connect($dsn, '', '', { RaiseError => 0, AutoCommit => 1 });

my $username         = $cgi->param('username')         // '';
my $password         = $cgi->param('password')         // '';
my $confirm_password = $cgi->param('confirm_password') // '';

# ── Redirect helper ────────────────────────────────────────────────────────────
sub redirect_to {
    my ($url) = @_;
    print $cgi->redirect($url);
    exit;
}

# ── Validation ─────────────────────────────────────────────────────────────────
$username =~ s/^\s+|\s+$//g;

if (length($username) < 3) {
    redirect_to('/product-system/register.html?error=short_u');
}
if (length($password) < 4) {
    redirect_to('/product-system/register.html?error=short_p');
}
if ($password ne $confirm_password) {
    redirect_to('/product-system/register.html?error=mismatch');
}

# ── Check if username already taken ───────────────────────────────────────────
my $check = $dbh->prepare("SELECT COUNT(*) FROM users WHERE username = ?");
$check->execute($username);
my ($count) = $check->fetchrow_array;

if ($count > 0) {
    redirect_to('/product-system/register.html?error=taken');
}

# ── Hash password with bcrypt (same as Go system) ─────────────────────────────
my $salt   = urandom(16);
my $hashed = bcrypt($password, '2b', 10, $salt);

# ── Insert new user ────────────────────────────────────────────────────────────
my $insert = $dbh->prepare("INSERT INTO users (username, password) VALUES (?, ?)");
my $ok = $insert->execute($username, $hashed);

unless ($ok) {
    redirect_to('/product-system/register.html?error=fail');
}

$dbh->disconnect;

redirect_to('/product-system/login.html?registered=1');
