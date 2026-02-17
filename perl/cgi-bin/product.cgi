#!C:/Strawberry/perl/bin/perl.exe
use strict;
use warnings;
use CGI;
use CGI::Cookie;
use DBI;

my $cgi    = CGI->new;
my $action = $cgi->param('action') || 'view_all';

my $dsn = "DBI:ODBC:Driver={SQL Server};Server=XEVO\\SQLEXPRESS01;Database=ProductDB;Trusted_Connection=Yes;";
my $dbh = DBI->connect($dsn, '', '', { RaiseError => 1, AutoCommit => 1 })
    or do {
        print $cgi->header('text/html');
        print "<h2>Database connection failed: $DBI::errstr</h2>";
        exit;
    };

# ── Session check ──────────────────────────────────────────────────────────────
my %cookies = CGI::Cookie->fetch;
unless ($cookies{pms_session}) {
    print $cgi->header(-status => '302 Found', -location => '/product-system/login.html?error=2');
    exit;
}
my $token = $cookies{pms_session}->value;
my $sess  = $dbh->prepare("SELECT username FROM sessions WHERE token = ? AND expires_at > GETDATE()");
$sess->execute($token);
my $sess_row = $sess->fetchrow_hashref;
unless ($sess_row) {
    print $cgi->header(-status => '302 Found', -location => '/product-system/login.html?error=2');
    exit;
}
my $logged_in_user = $sess_row->{username};

# ── HTML wrappers ──────────────────────────────────────────────────────────────
print $cgi->header('text/html; charset=UTF-8');

sub html_start {
    print qq{<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Product Management System</title>
    <link rel="stylesheet" href="/product-system/style.css">
    <style>
        .top-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
            border-bottom: 2px solid #000;
            padding-bottom: 10px;
        }
        .top-bar h1 { border: none; margin: 0; padding: 0; }
        .btn-logout {
            background: #000; color: #fff; border: 1px solid #000;
            padding: 7px 18px; font-size: 13px; font-weight: bold;
            cursor: pointer; text-decoration: none; letter-spacing: 0.4px;
        }
        .btn-logout:hover { background: #333; }
        .user-label { font-size: 13px; color: #666; margin-right: 12px; }
    </style>
</head>
<body>
<div class="container">
    <div class="top-bar">
        <h1>Product Management System</h1>
        <div>
            <span class="user-label">Signed in as <strong>$logged_in_user</strong></span>
            <a href="/cgi-bin/logout.cgi" class="btn-logout">SIGN OUT</a>
        </div>
    </div>
};
}

sub html_end {
    print qq{</div></body></html>};
}

# ── Stats helper ───────────────────────────────────────────────────────────────
sub render_stats {
    my $s = $dbh->prepare("SELECT COUNT(*) as cnt, ISNULL(SUM(price * quantity),0) as val FROM products");
    $s->execute();
    my $r = $s->fetchrow_hashref;
    my $total = $r->{cnt}  // 0;
    my $val   = sprintf("%.2f", $r->{val} // 0);
    print qq{
    <div class="stats">
        <div class="stat-card"><h3>$total</h3><p>Total Products</p></div>
        <div class="stat-card"><h3>\$$val</h3><p>Total Inventory Value</p></div>
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
                <input type="text"   name="name"     placeholder="Product Name"  required>
                <input type="number" name="price"    placeholder="Price" step="0.01" required>
                <input type="number" name="quantity" placeholder="Quantity"       required>
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
    print qq{
    <div class="search-box">
        <form action="/cgi-bin/product.cgi" method="post" style="display:flex;gap:10px;">
            <input type="hidden" name="action" value="search">
            <input type="text" name="keyword" placeholder="Search products by name or ID..."
                   value="$keyword" style="flex:1;">
            <button type="submit">Search</button>
        </form>
    </div>
    };
}

# ── Products table ─────────────────────────────────────────────────────────────
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

    my $total_val = 0;
    print qq{
    <table>
        <thead>
            <tr><th>ID</th><th>Name</th><th>Price</th><th>Quantity</th><th>Total Value</th><th>Actions</th></tr>
        </thead>
        <tbody>
    };

    for my $r (@rows) {
        my $line_val = sprintf("%.2f", ($r->{price} // 0) * ($r->{quantity} // 0));
        $total_val += $line_val;
        print qq{
            <tr>
                <td>$r->{id}</td>
                <td>$r->{name}</td>
                <td>\$$r->{price}</td>
                <td>$r->{quantity}</td>
                <td>\$$line_val</td>
                <td class="actions">
                    <form method="post" style="display:inline;">
                        <input type="hidden" name="action" value="edit_form">
                        <input type="hidden" name="id"     value="$r->{id}">
                        <button type="submit" class="btn-edit">Edit</button>
                    </form>
                    <form method="post" style="display:inline;"
                          onsubmit="return confirm('Delete $r->{name}?');">
                        <input type="hidden" name="action" value="delete">
                        <input type="hidden" name="id"     value="$r->{id}">
                        <button type="submit" class="btn-delete">Delete</button>
                    </form>
                </td>
            </tr>
        };
    }

    print qq{</tbody></table>};
}

# ══════════════════════════════════════════════════════════════════════════════
# ACTION HANDLERS
# ══════════════════════════════════════════════════════════════════════════════

# ── View all ───────────────────────────────────────────────────────────────────
if ($action eq 'view_all' || $action eq '') {
    html_start();
    render_stats();
    render_add_form();
    render_search_form();

    my $sth = $dbh->prepare("SELECT * FROM products ORDER BY id ASC");
    $sth->execute();
    render_table($sth);
    html_end();
}

# ── Add product ────────────────────────────────────────────────────────────────
elsif ($action eq 'add') {
    my $name  = $cgi->param('name');
    my $price = $cgi->param('price');
    my $qty   = $cgi->param('quantity');

    eval {
        $dbh->do(
            "INSERT INTO products (name, price, quantity) VALUES (?, ?, ?)",
            undef, $name, $price, $qty
        );
    };

    html_start();
    if ($@) {
        print qq{<div class="message error" style="display:block;">Error adding product: $@</div>};
    } else {
        print qq{<div class="message success" style="display:block;">Product "$name" added successfully!</div>};
    }
    render_stats();
    render_add_form();
    render_search_form();
    my $sth = $dbh->prepare("SELECT * FROM products ORDER BY id ASC");
    $sth->execute();
    render_table($sth);
    html_end();
}

# ── Search ─────────────────────────────────────────────────────────────────────
elsif ($action eq 'search') {
    my $keyword = $cgi->param('keyword') // '';
    html_start();
    render_stats();
    render_add_form();
    render_search_form($keyword);

    my $sth;
    # Try numeric ID first
    if ($keyword =~ /^\d+$/) {
        $sth = $dbh->prepare("SELECT * FROM products WHERE id = ?");
        $sth->execute($keyword);
    } else {
        $sth = $dbh->prepare("SELECT * FROM products WHERE name LIKE ?");
        $sth->execute("%$keyword%");
    }
    render_table($sth);
    html_end();
}

# ── Edit form ──────────────────────────────────────────────────────────────────
elsif ($action eq 'edit_form') {
    my $id  = $cgi->param('id');
    my $sth = $dbh->prepare("SELECT * FROM products WHERE id = ?");
    $sth->execute($id);
    my $p = $sth->fetchrow_hashref;

    html_start();
    if ($p) {
        print qq{
        <div class="form-section">
            <h2>Edit Product (ID: $id)</h2>
            <form action="/cgi-bin/product.cgi" method="post">
                <input type="hidden" name="action" value="edit">
                <input type="hidden" name="id"     value="$id">
                <div class="form-row">
                    <input type="text"   name="name"     value="$p->{name}"     required>
                    <input type="number" name="price"    value="$p->{price}"    step="0.01" required>
                    <input type="number" name="quantity" value="$p->{quantity}" required>
                    <button type="submit">Save Changes</button>
                </div>
            </form>
        </div>
        };
    } else {
        print qq{<div class="message error" style="display:block;">Product not found.</div>};
    }
    render_stats();
    render_search_form();
    my $all = $dbh->prepare("SELECT * FROM products ORDER BY id ASC");
    $all->execute();
    render_table($all);
    html_end();
}

# ── Edit submit ────────────────────────────────────────────────────────────────
elsif ($action eq 'edit') {
    my $id    = $cgi->param('id');
    my $name  = $cgi->param('name');
    my $price = $cgi->param('price');
    my $qty   = $cgi->param('quantity');

    eval {
        $dbh->do(
            "UPDATE products SET name=?, price=?, quantity=? WHERE id=?",
            undef, $name, $price, $qty, $id
        );
    };

    html_start();
    if ($@) {
        print qq{<div class="message error" style="display:block;">Error updating product: $@</div>};
    } else {
        print qq{<div class="message success" style="display:block;">Product updated successfully!</div>};
    }
    render_stats();
    render_add_form();
    render_search_form();
    my $sth = $dbh->prepare("SELECT * FROM products ORDER BY id ASC");
    $sth->execute();
    render_table($sth);
    html_end();
}

# ── Delete ─────────────────────────────────────────────────────────────────────
elsif ($action eq 'delete') {
    my $id = $cgi->param('id');

    my $name_sth = $dbh->prepare("SELECT name FROM products WHERE id=?");
    $name_sth->execute($id);
    my $prod = $name_sth->fetchrow_hashref;
    my $prod_name = $prod ? $prod->{name} : "ID $id";

    eval { $dbh->do("DELETE FROM products WHERE id=?", undef, $id); };

    html_start();
    if ($@) {
        print qq{<div class="message error" style="display:block;">Error deleting product: $@</div>};
    } else {
        print qq{<div class="message success" style="display:block;">Product "$prod_name" deleted successfully!</div>};
    }
    render_stats();
    render_add_form();
    render_search_form();
    my $sth = $dbh->prepare("SELECT * FROM products ORDER BY id ASC");
    $sth->execute();
    render_table($sth);
    html_end();
}

else {
    html_start();
    print "<p>Unknown action.</p>";
    html_end();
}

$dbh->disconnect;
