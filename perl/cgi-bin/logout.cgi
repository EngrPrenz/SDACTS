#!C:/Strawberry/perl/bin/perl.exe
use strict;
use warnings;
use CGI;
use CGI::Cookie;

my $cgi = CGI->new;

my $cookie = CGI::Cookie->new(
    -name    => 'pms_session',
    -value   => '',
    -expires => '-1d',
    -path    => '/',
);

print $cgi->header(
    -status   => '302 Found',
    -location => '/product-system/login.html',
    -cookie   => $cookie
);
exit;