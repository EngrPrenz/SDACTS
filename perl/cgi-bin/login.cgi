#!C:/Strawberry/perl/bin/perl.exe
use strict;
use warnings;
use CGI;
use CGI::Cookie;
use DBI;
use Crypt::Bcrypt qw(bcrypt_check);
use Digest::SHA  qw(sha256_hex);

my $cgi = CGI->new;

my $dsn = "DBI:ODBC:Driver={SQL Server};Server=XEVO\\SQLEXPRESS01;Database=ProductDB;Trusted_Connection=Yes;";
my $dbh = DBI->connect($dsn, '', '', { RaiseError => 0, AutoCommit => 1 });

my $username = $cgi->param('username') // '';
my $password = $cgi->param('password') // '';

# ── Redirect helper ────────────────────────────────────────────────────────────
sub redirect_to {
    my ($url) = @_;
    print $cgi->header(-location => $url, -status => '302 Found');
    exit;
}

# ── Validate input ─────────────────────────────────────────────────────────────
unless ($username && $password) {
    redirect_to('/product-system/login.html?error=1');
}

# ── Fetch user from DB ─────────────────────────────────────────────────────────
my $sth = $dbh->prepare("SELECT id, password FROM users WHERE username = ?");
$sth->execute($username);
my $user = $sth->fetchrow_hashref;

unless ($user) {
    redirect_to('/product-system/login.html?error=1');
}

# ── Verify password with bcrypt (same as Go system) ───────────────────────────
my $ok = eval { bcrypt_check($password, $user->{password}) };
unless ($ok) {
    redirect_to('/product-system/login.html?error=1');
}

# ── Create session token ───────────────────────────────────────────────────────
my $token = sha256_hex($username . time() . rand());

# Store session in DB
$dbh->do(
    "INSERT INTO sessions (token, username, expires_at) VALUES (?, ?, DATEADD(HOUR, 8, GETDATE()))",
    undef, $token, $username
);

# Set session cookie
my $cookie = CGI::Cookie->new(
    -name     => 'pms_session',
    -value    => $token,
    -expires  => '+8h',
    -path     => '/',
    -httponly => 1,
);

print $cgi->header(
    -type     => 'text/html',
    -status   => '302 Found',
    -location => '/cgi-bin/product.cgi?action=view_all',
    -cookie   => $cookie,
);

$dbh->disconnect;
