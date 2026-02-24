#!C:/Strawberry/perl/bin/perl.exe
use strict;
use warnings;
use CGI;
use CGI::Cookie;
use DBI;
use Crypt::Bcrypt qw(bcrypt_check);
use Digest::SHA  qw(sha256_hex);

my $cgi = CGI->new;

# ✅ change server if needed
my $dsn = "DBI:ODBC:Driver={SQL Server};Server=ACER-NITROV15-F\\SQLEXPRESS;Database=SampleDB;Trusted_Connection=Yes;";
my $dbh = DBI->connect($dsn, '', '', { RaiseError => 0, AutoCommit => 1 });

# ✅ IMPORTANT: change this to a LONG random secret string
my $COOKIE_SECRET = "CHANGE_ME_TO_A_LONG_RANDOM_SECRET";

my $username = $cgi->param('username') // '';
my $password = $cgi->param('password') // '';

sub redirect_to {
    my ($url) = @_;
    print $cgi->header(-location => $url, -status => '302 Found');
    exit;
}

unless ($username && $password) {
    redirect_to('/product-system/login.html?error=1');
}

# Matches your schema
my $sth = $dbh->prepare("SELECT Id, Password FROM [Users] WHERE Username = ?");
$sth->execute($username);
my $user = $sth->fetchrow_hashref;

unless ($user) {
    redirect_to('/product-system/login.html?error=1');
}

my $ok = eval { bcrypt_check($password, $user->{Password}) };
unless ($ok) {
    redirect_to('/product-system/login.html?error=1');
}

# ---- Signed cookie session (no Sessions table needed) ----
my $expires_epoch = time() + (8 * 60 * 60); # 8 hours
my $payload = $username . "|" . $expires_epoch;
my $sig     = sha256_hex($payload . "|" . $COOKIE_SECRET);
my $token   = $payload . "|" . $sig;

my $cookie = CGI::Cookie->new(
    -name     => 'pms_session',
    -value    => $token,
    -expires  => '+8h',
    -path     => '/',
    -httponly => 1,
);

print $cgi->header(
    -status   => '302 Found',
    -location => '/cgi-bin/product.cgi?action=view_all',
    -cookie   => $cookie
);
exit;