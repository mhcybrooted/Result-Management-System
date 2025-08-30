package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.service.ExamService;
import mh.cyb.root.rms.service.TeacherService;
import mh.cyb.root.rms.service.TeacherAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;

@Controller
public class TeacherController {
    
    @Autowired
    private TeacherService teacherService;
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private TeacherAssignmentService teacherAssignmentService;
    
    @GetMapping("/teachers")
    public String listTeachers(Model model) {
        List<Teacher> teachers = teacherService.getAllActiveTeachers();
        model.addAttribute("teachers", teachers);
        model.addAttribute("subjects", examService.getAllSubjects());
        
        // Calculate subjects covered (subjects that have at least one teacher assigned)
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
            List<TeacherAssignment> assignments = teacherAssignmentService.getAllActiveAssignments(activeSession.get().getId());
            long subjectsCovered = assignments.stream()
                    .map(assignment -> assignment.getSubject().getId())
                    .distinct()
                    .count();
            model.addAttribute("subjectsCovered", subjectsCovered);
            
            // Calculate assignment rate (percentage of teachers who have assignments)
            long teachersWithAssignments = assignments.stream()
                    .map(assignment -> assignment.getTeacher().getId())
                    .distinct()
                    .count();
            double assignmentRate = teachers.size() > 0 ? (teachersWithAssignments * 100.0 / teachers.size()) : 0.0;
            model.addAttribute("assignmentRate", Math.round(assignmentRate));
        } else {
            model.addAttribute("subjectsCovered", 0);
            model.addAttribute("assignmentRate", 0);
        }
        
        return "teachers";
    }
    
    @GetMapping("/add-teacher")
    public String addTeacherForm(Model model, @RequestParam(required = false) Long id) {
        if (id != null) {
            Teacher teacher = teacherService.findById(id);
            if (teacher != null) {
                model.addAttribute("teacher", teacher);
                model.addAttribute("isEdit", true);
            }
        }
        if (!model.containsAttribute("teacher")) {
            model.addAttribute("teacher", new Teacher());
            model.addAttribute("isEdit", false);
        }
        return "add-teacher";
    }
    
    @GetMapping("/teachers/add")
    public String addTeacherAlias(Model model) {
        return addTeacherForm(model, null);
    }
    
    @PostMapping("/add-teacher")
    public String saveTeacher(@ModelAttribute Teacher teacher, RedirectAttributes redirectAttributes) {
        try {
            teacherService.saveTeacher(teacher);
            String message = teacher.getId() != null ? "Teacher updated successfully!" : "Teacher added successfully!";
            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving teacher: " + e.getMessage());
        }
        return "redirect:/teachers";
    }
    
    @PostMapping("/teachers/add")
    public String saveTeacherAlias(@ModelAttribute Teacher teacher, RedirectAttributes redirectAttributes) {
        return saveTeacher(teacher, redirectAttributes);
    }
}
