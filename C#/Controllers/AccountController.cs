using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SampleCRUD.Data;
using SampleCRUD.Models;
using System.Security.Claims;

namespace SampleCRUD.Controllers;

public class AccountController : Controller
{
    private readonly AppDbContext _context;

    public AccountController(AppDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public IActionResult Login()
    {
        if (User.Identity?.IsAuthenticated == true)
            return RedirectToAction("Index", "Products");
        return View();
    }

    [HttpPost]
[ValidateAntiForgeryToken]
public async Task<IActionResult> Login(LoginViewModel model)
{
    if (!ModelState.IsValid)
    {
        foreach (var err in ModelState.Values.SelectMany(v => v.Errors))
            ModelState.AddModelError("", $"Validation: {err.ErrorMessage}");
        return View(model);
    }

    try
    {
        var allUsers = await _context.Users.ToListAsync();

        if (!allUsers.Any())
        {
            ModelState.AddModelError("", "❌ Database connected but Users table is EMPTY. Please insert the admin user.");
            return View(model);
        }

        var user = allUsers.FirstOrDefault(u => u.Username == model.Username && u.Password == model.Password);

        if (user == null)
        {
            var byName = allUsers.FirstOrDefault(u => u.Username == model.Username);
            if (byName == null)
                ModelState.AddModelError("", $"❌ No user '{model.Username}' found. Users in DB: {string.Join(", ", allUsers.Select(u => $"'{u.Username}'"))}");
            else
                ModelState.AddModelError("", $"❌ Username matches but password is wrong. DB password length is {byName.Password.Length} chars.");
            return View(model);
        }

        var claims = new List<Claim>
        {
            new Claim(ClaimTypes.Name, user.Username),
            new Claim(ClaimTypes.NameIdentifier, user.Id.ToString())
        };
        var identity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
        await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme, new ClaimsPrincipal(identity));
        return RedirectToAction("Index", "Products");
    }
    catch (Exception ex)
    {
        ModelState.AddModelError("", $"❌ DB Error: {ex.Message}");
        return View(model);
    }
}

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Logout()
    {
        await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
        return RedirectToAction("Login");
    }
}