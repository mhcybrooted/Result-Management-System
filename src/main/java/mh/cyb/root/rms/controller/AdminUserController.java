package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.entity.AdminUser;
import mh.cyb.root.rms.entity.Teacher;
import mh.cyb.root.rms.service.AdminUserService;
import mh.cyb.root.rms.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private TeacherService teacherService;

    @GetMapping
    public String listUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        org.springframework.data.domain.Page<AdminUser> userPage = adminUserService.getAllAdminUsers(
                org.springframework.data.domain.PageRequest.of(page, size));
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        return "admin-users";
    }

    @GetMapping("/add")
    public String addUserForm(Model model) {
        List<Teacher> teachers = teacherService.getAllActiveTeachers();
        model.addAttribute("teachers", teachers);
        model.addAttribute("adminUser", new AdminUser());
        return "add-admin-user";
    }

    @PostMapping("/add")
    public String addUser(@ModelAttribute AdminUser adminUser, @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        if (!adminUser.getPassword().equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/admin/users/add";
        }

        if (adminUserService.existsByUsername(adminUser.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "Username already exists");
            return "redirect:/admin/users/add";
        }

        try {
            adminUserService.createAdmin(adminUser.getUsername(), adminUser.getPassword(), adminUser.getRole(),
                    adminUser.getTeacherId());
            redirectAttributes.addFlashAttribute("success", "User created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating user: " + e.getMessage());
            return "redirect:/admin/users/add";
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminUserService.deleteAdminUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user");
        }
        return "redirect:/admin/users";
    }
}
