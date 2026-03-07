#!C:/Strawberry/perl/bin/perl.exe
use strict;
use warnings;

use CGI qw(:standard escapeHTML);
use CGI::Cookie;
use CGI::Carp qw(fatalsToBrowser);   # shows Perl errors in browser instead of only 500
use DBI;
use Digest::SHA qw(sha256_hex);

my $cgi    = CGI->new;
my $action = $cgi->param('action') || 'view_all';

# ✅ MUST MATCH login.cgi
my $COOKIE_SECRET = "CHANGE_ME_TO_A_LONG_RANDOM_SECRET";

# ✅ your DB
my $dsn = "DBI:ODBC:Driver={SQL Server};Server=ACER-NITROV15-F\\SQLEXPRESS;Database=SampleDB;Trusted_Connection=Yes;";
my $dbh = DBI->connect($dsn, '', '', { RaiseError => 1, AutoCommit => 1 });

# ── Session check: signed cookie session ───────────────────────────────────────
my %cookies = CGI::Cookie->fetch;

unless ($cookies{pms_session}) {
    print $cgi->redirect('/product-system/login.html?error=2');
    exit;
}

my $raw = $cookies{pms_session}->value // '';
my ($u, $exp, $sig) = split(/\|/, $raw, 3);

unless ($u && $exp && $sig) {
    print $cgi->redirect('/product-system/login.html?error=2');
    exit;
}

if ($exp !~ /^\d+$/ || $exp <= time()) {
    print $cgi->redirect('/product-system/login.html?error=2');
    exit;
}

my $expected = sha256_hex($u . "|" . $exp . "|" . $COOKIE_SECRET);
unless ($sig eq $expected) {
    print $cgi->redirect('/product-system/login.html?error=2');
    exit;
}

my $logged_in_user = $u;

# ── HTML wrappers ──────────────────────────────────────────────────────────────
print $cgi->header('text/html; charset=UTF-8');

sub html_start {
    my $safe_user = escapeHTML($logged_in_user);
    print qq{<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Product Management System</title>
  <link rel="stylesheet" href="/product-system/style.css">
  <style>
    .top-bar {
      display:flex; justify-content:space-between; align-items:center;
      margin-bottom:20px; border-bottom:2px solid #000; padding-bottom:10px;
    }
    .top-bar h1 { border:none; margin:0; padding:0; }
    .btn-logout {
      background:#000; color:#fff; border:1px solid #000;
      padding:7px 18px; font-size:13px; font-weight:bold;
      cursor:pointer; text-decoration:none; letter-spacing:0.4px;
    }
    .btn-logout:hover { background:#333; }
    .user-label { font-size:13px; color:#666; margin-right:12px; }
  </style>
</head>
<body>
<div class="container">
  <div class="top-bar">
    <h1>Product Management System</h1>
    <div>
      <span class="user-label">Signed in as <strong>$safe_user</strong></span>
      <a href="/cgi-bin/logout.cgi" class="btn-logout">SIGN OUT</a>
    </div>
  </div>
};
}

sub html_end {
    print qq{</div></body></html>};
}

# ── Stats ─────────────────────────────────────────────────────────────────────
sub render_stats {
    my $s = $dbh->prepare("SELECT COUNT(*) AS cnt, ISNULL(SUM(Price),0) AS total_price FROM [Products]");
    $s->execute();
    my $r = $s->fetchrow_hashref;

    my $total = $r->{cnt} // 0;
    my $sum   = sprintf("%.2f", $r->{total_price} // 0);

    print qq{
    <div class="stats">
      <div class="stat-card"><h3>$total</h3><p>Total Products</p></div>
      <div class="stat-card"><h3>\$$sum</h3><p>Sum of Prices</p></div>
    </div>
    };
}

# ── Add form ───────────────────────────────────────────────────────────────────
sub render_add_form {
    print qq{
    <div class="form-section">
      <h2>Add New Product</h2>
      <form action="/cgi-bin/product.cgi" method="post">
        <input type="hidden" name="action" value="add">
        <div class="form-row">
          <input type="text"   name="name"  placeholder="Product Name" required>
          <input type="number" name="price" placeholder="Price" step="0.01" required>
          <button type="submit">Add Product</button>
        </div>
      </form>
    </div>
    };
}

# ── Search form ────────────────────────────────────────────────────────────────
sub render_search_form {
    my ($keyword) = @_;
    $keyword //= '';
    my $safe_kw = escapeHTML($keyword);
    print qq{
    <div class="search-box">
      <form action="/cgi-bin/product.cgi" method="post" style="display:flex;gap:10px;">
        <input type="hidden" name="action" value="search">
        <input type="text" name="keyword" placeholder="Search products by name or ID..."
               value="$safe_kw" style="flex:1;">
        <button type="submit">Search</button>
      </form>
    </div>
    };
}

# ── Table ─────────────────────────────────────────────────────────────────────
sub render_table {
    my ($sth_ref) = @_;
    my @rows;
    while (my $row = $sth_ref->fetchrow_hashref) {
        push @rows, $row;
    }

    if (@rows == 0) {
        print qq{<p style="text-align:center;padding:20px;">No products found.</p>};
        return;
    }

    print qq{
    <table>
      <thead>
        <tr><th>ID</th><th>Name</th><th>Price</th><th>Actions</th></tr>
      </thead>
      <tbody>
    };

    for my $r (@rows) {
        my $id    = escapeHTML($r->{Id});
        my $name  = escapeHTML($r->{Name} // '');
        my $price = sprintf("%.2f", $r->{Price} // 0);

        print qq{
        <tr>
          <td>$id</td>
          <td>$name</td>
          <td>\$$price</td>
          <td class="actions">
            <form method="post" style="display:inline;">
              <input type="hidden" name="action" value="edit_form">
              <input type="hidden" name="id" value="$id">
              <button type="submit" class="btn-edit">Edit</button>
            </form>
            <form method="post" style="display:inline;" onsubmit="return confirm('Delete $name?');">
              <input type="hidden" name="action" value="delete">
              <input type="hidden" name="id" value="$id">
              <button type="submit" class="btn-delete">Delete</button>
            </form>
          </td>
        </tr>
        };
    }

    print qq{</tbody></table>};
}

# ══════════════════════════════════════════════════════════════════════════════
# ACTIONS
# ══════════════════════════════════════════════════════════════════════════════

# View all
if ($action eq 'view_all' || $action eq '') {
    html_start();
    render_stats();
    render_add_form();
    render_search_form();

    my $sth = $dbh->prepare("SELECT Id, Name, Price FROM [Products] ORDER BY Id ASC");
    $sth->execute();
    render_table($sth);

    html_end();
    exit;
}

# Add
elsif ($action eq 'add') {
    my $name  = $cgi->param('name')  // '';
    my $price = $cgi->param('price') // '';

    my $sth = $dbh->prepare("INSERT INTO [Products] (Name, Price) VALUES (?, ?)");
    $sth->execute($name, $price);

    print $cgi->redirect('/cgi-bin/product.cgi?action=view_all');
    exit;
}

# Edit form
elsif ($action eq 'edit_form') {
    my $id = $cgi->param('id') // '';

    my $sth = $dbh->prepare("SELECT Id, Name, Price FROM [Products] WHERE Id = ?");
    $sth->execute($id);
    my $r = $sth->fetchrow_hashref;

    html_start();
    print qq{<h2>Edit Product</h2>};

    if (!$r) {
        print qq{<p>Product not found.</p>};
        html_end();
        exit;
    }

    my $sid    = escapeHTML($r->{Id});
    my $sname  = escapeHTML($r->{Name} // '');
    my $sprice = sprintf("%.2f", $r->{Price} // 0);

    print qq{
    <div class="form-section">
      <form action="/cgi-bin/product.cgi" method="post">
        <input type="hidden" name="action" value="edit">
        <input type="hidden" name="id" value="$sid">
        <div class="form-row">
          <input type="text" name="name" value="$sname" required>
          <input type="number" name="price" value="$sprice" step="0.01" required>
          <button type="submit">Save</button>
          <a href="/cgi-bin/product.cgi?action=view_all" class="btn-cancel">Cancel</a>
        </div>
      </form>
    </div>
    };

    html_end();
    exit;
}

# Edit
elsif ($action eq 'edit') {
    my $id    = $cgi->param('id')    // '';
    my $name  = $cgi->param('name')  // '';
    my $price = $cgi->param('price') // '';

    my $sth = $dbh->prepare("UPDATE [Products] SET Name = ?, Price = ? WHERE Id = ?");
    $sth->execute($name, $price, $id);

    print $cgi->redirect('/cgi-bin/product.cgi?action=view_all');
    exit;
}

# Delete
elsif ($action eq 'delete') {
    my $id = $cgi->param('id') // '';

    my $sth = $dbh->prepare("DELETE FROM [Products] WHERE Id = ?");
    $sth->execute($id);

    print $cgi->redirect('/cgi-bin/product.cgi?action=view_all');
    exit;
}

# Search (by Name LIKE or Id exact if number)
elsif ($action eq 'search') {
    my $keyword = $cgi->param('keyword') // '';

    html_start();
    render_stats();
    render_add_form();
    render_search_form($keyword);

    my $sth;
    if ($keyword =~ /^\d+$/) {
        $sth = $dbh->prepare("SELECT Id, Name, Price FROM [Products] WHERE Id = ? ORDER BY Id ASC");
        $sth->execute($keyword);
    } else {
        $sth = $dbh->prepare("SELECT Id, Name, Price FROM [Products] WHERE Name LIKE ? ORDER BY Id ASC");
        $sth->execute('%' . $keyword . '%');
    }

    render_table($sth);
    html_end();
    exit;
}

# Unknown action
else {
    html_start();
    print qq{<p>Unknown action.</p>};
    html_end();
    exit;
}