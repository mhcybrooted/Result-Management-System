package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.dto.BulkMarksRequest;
import mh.cyb.root.rms.dto.BulkResult;
import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.service.ExamService;
import mh.cyb.root.rms.service.BulkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/bulk")
public class BulkMarksController {
    
    @Autowired
    private ExamService examService;
    
    @Autowired
    private BulkService bulkService;
    
    @GetMapping("/marks")
    public String bulkMarksForm(@RequestParam(required = false) String classFilter, Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            model.addAttribute("error", "No active session found");
            return "bulk-marks";
        }
        
        List<Student> students;
        if (classFilter != null && !classFilter.trim().isEmpty()) {
            students = examService.getAllStudents().stream()
                    .filter(s -> s.getClassName().equals(classFilter))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            students = examService.getAllStudents();
        }
        
        List<Subject> subjects = examService.getAllSubjects();
        List<Exam> exams = examService.getAllActiveExams();
        List<Teacher> teachers = examService.getAllActiveTeachers();
        
        List<String> availableClasses = examService.getAllStudents().stream()
                .map(Student::getClassName)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("students", students);
        model.addAttribute("subjects", subjects);
        model.addAttribute("exams", exams);
        model.addAttribute("teachers", teachers);
        model.addAttribute("availableClasses", availableClasses);
        model.addAttribute("selectedClass", classFilter);
        model.addAttribute("activeSession", activeSession.get());
        
        return "bulk-marks";
    }
    
    @PostMapping("/marks")
    public String saveBulkMarks(@ModelAttribute BulkMarksRequest request, 
                               RedirectAttributes redirectAttributes) {
        
        BulkResult result = bulkService.saveBulkMarks(request);
        
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", 
                "Bulk save failed: " + String.join(", ", result.getErrors()));
        } else {
            String message = "Bulk marks saved! " + result.getSuccessCount() + " successful";
            if (result.getErrorCount() > 0) {
                message += ", " + result.getErrorCount() + " errors";
            }
            if (result.hasWarnings()) {
                message += " (with warnings)";
            }
            redirectAttributes.addFlashAttribute("success", message);
            
            if (result.hasWarnings()) {
                redirectAttributes.addFlashAttribute("warnings", result.getWarnings());
            }
        }
        
        return "redirect:/bulk/marks";
    }
}
