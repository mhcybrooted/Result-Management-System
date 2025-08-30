package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.entity.Teacher;
import mh.cyb.root.rms.entity.Subject;
import mh.cyb.root.rms.entity.Session;
import mh.cyb.root.rms.service.TeacherService;
import mh.cyb.root.rms.service.TeacherAssignmentService;
import mh.cyb.root.rms.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/teachers")
public class TeacherController {
    
    @Autowired
    private TeacherService teacherService;
    
    @Autowired
    private TeacherAssignmentService teacherAssignmentService;
    
    @Autowired
    private ExamService examService;
    
    @GetMapping
    public String listTeachers(Model model) {
        List<Teacher> teachers = teacherService.getAllActiveTeachers();
        model.addAttribute("teachers", teachers);
        return "teachers";
    }
    
    @GetMapping("/add")
    public String addTeacherForm(Model model) {
        model.addAttribute("teacher", new Teacher());
        return "add-teacher";
    }
    
    @PostMapping("/add")
    public String saveTeacher(@Valid @ModelAttribute Teacher teacher, 
                             BindingResult result, 
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "add-teacher";
        }
        
        try {
            teacherService.saveTeacher(teacher);
            redirectAttributes.addFlashAttribute("success", "Teacher added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding teacher: " + e.getMessage());
        }
        
        return "redirect:/teachers";
    }
    
    @GetMapping("/{id}/assign")
    public String assignSubjectsForm(@PathVariable Long id, Model model) {
        Teacher teacher = teacherService.findById(id);
        if (teacher == null) {
            return "redirect:/teachers";
        }
        
        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            return "redirect:/teachers";
        }
        
        List<Subject> allSubjects = examService.getAllSubjects();
        List<Subject> assignedSubjects = teacherAssignmentService.getAssignedSubjects(id, activeSession.get().getId());
        
        model.addAttribute("teacher", teacher);
        model.addAttribute("allSubjects", allSubjects);
        model.addAttribute("assignedSubjects", assignedSubjects);
        model.addAttribute("activeSession", activeSession.get());
        
        return "assign-subjects";
    }
    
    @PostMapping("/{id}/assign")
    public String assignSubjects(@PathVariable Long id, 
                                @RequestParam(required = false) List<Long> subjectIds,
                                RedirectAttributes redirectAttributes) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "No active session found!");
            return "redirect:/teachers";
        }
        
        try {
            if (subjectIds == null) subjectIds = List.of(); // Handle no selections
            teacherAssignmentService.assignSubjectsToTeacher(id, subjectIds, activeSession.get().getId());
            redirectAttributes.addFlashAttribute("success", "Subjects assigned successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error assigning subjects: " + e.getMessage());
        }
        
        return "redirect:/teachers";
    }
}
