using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using SampleCRUD.Data;
using SampleCRUD.Models;

namespace SampleCRUD.Controllers;

[Authorize]
public class UsersController : Controller
{
    private readonly AppDbContext _context;

    public UsersController(AppDbContext context)
    {
        _context = context;
    }

    public async Task<IActionResult> Index()
    {
        return View(await _context.Users.ToListAsync());
    }

    public IActionResult Create()
    {
        return View();
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create([Bind("Username,Password")] User user)
    {
        if (ModelState.IsValid)
        {
            bool exists = await _context.Users.AnyAsync(u => u.Username == user.Username);
            if (exists)
            {
                ModelState.AddModelError("Username", "Username already exists.");
                return View(user);
            }
            _context.Add(user);
            await _context.SaveChangesAsync();
            TempData["Success"] = "User created successfully!";
            return RedirectToAction(nameof(Index));
        }
        return View(user);
    }

    public async Task<IActionResult> Edit(int? id)
    {
        if (id == null) return NotFound();
        var user = await _context.Users.FindAsync(id);
        if (user == null) return NotFound();
        return View(user);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(int id, [Bind("Id,Username,Password")] User user)
    {
        if (id != user.Id) return NotFound();

        if (ModelState.IsValid)
        {
            try
            {
                bool exists = await _context.Users.AnyAsync(u => u.Username == user.Username && u.Id != user.Id);
                if (exists)
                {
                    ModelState.AddModelError("Username", "Username already taken.");
                    return View(user);
                }
                _context.Update(user);
                await _context.SaveChangesAsync();
                TempData["Success"] = "User updated successfully!";
            }
            catch (DbUpdateConcurrencyException)
            {
                if (!_context.Users.Any(e => e.Id == user.Id)) return NotFound();
                throw;
            }
            return RedirectToAction(nameof(Index));
        }
        return View(user);
    }

    public async Task<IActionResult> Delete(int? id)
    {
        if (id == null) return NotFound();
        var user = await _context.Users.FirstOrDefaultAsync(m => m.Id == id);
        if (user == null) return NotFound();
        return View(user);
    }

    [HttpPost, ActionName("Delete")]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var user = await _context.Users.FindAsync(id);
        if (user != null)
        {
            _context.Users.Remove(user);
            await _context.SaveChangesAsync();
            TempData["Success"] = "User deleted successfully!";
        }
        return RedirectToAction(nameof(Index));
    }
}