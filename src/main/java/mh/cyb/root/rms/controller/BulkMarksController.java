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

    @Autowired
    private mh.cyb.root.rms.repository.AdminUserRepository adminUserRepository;

    @Autowired
    private mh.cyb.root.rms.service.TeacherAssignmentService teacherAssignmentService;

    @Autowired
    private mh.cyb.root.rms.service.ActivityLogService activityLogService;

    private String securityServiceGetUsername() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        return authentication.getName();
    }

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

        // RBAC: Filter for Teachers
        String username = securityServiceGetUsername();
        Optional<AdminUser> adminUserOpt = adminUserRepository.findByUsername(username);
        boolean isTeacher = false;

        if (adminUserOpt.isPresent() && "TEACHER".equals(adminUserOpt.get().getRole())) {
            isTeacher = true;
            Long teacherId = adminUserOpt.get().getTeacherId();
            if (activeSession.isPresent() && teacherId != null) {
                // Filter subjects
                List<Subject> assignedSubjects = teacherAssignmentService.getAssignedSubjects(teacherId,
                        activeSession.get().getId());
                subjects = assignedSubjects; // Override with only assigned subjects
            }
        }

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
        model.addAttribute("selectedClass", classFilter);
        model.addAttribute("activeSession", activeSession.get());
        model.addAttribute("isTeacher", isTeacher);
        if (isTeacher && adminUserOpt.get().getTeacherId() != null) {
            model.addAttribute("currentTeacherId", adminUserOpt.get().getTeacherId());
        }

        return "bulk-marks";
    }

    @PostMapping("/marks")
    public String saveBulkMarks(@ModelAttribute BulkMarksRequest request,
            RedirectAttributes redirectAttributes) {

        // RBAC Security Validation
        String username = securityServiceGetUsername();
        Optional<AdminUser> adminUserOpt = adminUserRepository.findByUsername(username);

        if (adminUserOpt.isPresent() && "TEACHER".equals(adminUserOpt.get().getRole())) {
            Long validTeacherId = adminUserOpt.get().getTeacherId();
            if (validTeacherId == null) {
                redirectAttributes.addFlashAttribute("error", "Security Error: Teacher profile not linked.");
                return "redirect:/bulk/marks";
            }
            // Force override teacher ID
            request.setTeacherId(validTeacherId);

            // Validate Subject Assignment
            Optional<Session> activeSession = examService.getActiveSession();
            if (activeSession.isPresent()) {
                List<Subject> assignedSubjects = teacherAssignmentService.getAssignedSubjects(validTeacherId,
                        activeSession.get().getId());
                boolean isAssigned = assignedSubjects.stream().anyMatch(s -> s.getId().equals(request.getSubjectId()));
                if (!isAssigned) {
                    redirectAttributes.addFlashAttribute("error",
                            "Security Alert: You are not authorized to add marks for this subject.");
                    return "redirect:/bulk/marks";
                }
            }
        }

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
