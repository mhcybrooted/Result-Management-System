package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.entity.Session;
import mh.cyb.root.rms.entity.Student;
import mh.cyb.root.rms.repository.StudentRepository;
import mh.cyb.root.rms.service.AttendanceService;
import mh.cyb.root.rms.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class PublicAttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ExamService examService;

    @GetMapping("/attendance/public")
    public String showSearchPage(Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
        }
        return "attendance-public-search";
    }

    @PostMapping("/attendance/public/search")
    public String searchAttendance(@RequestParam String rollNumber, Model model,
            RedirectAttributes redirectAttributes) {
        // Find Student (Optimized: Find in Active Session)
        // Since we don't have separate method for findByRollAndSession in repo yet,
        // we use roll number and filter via active session logic if needed,
        // OR rely on unique roll number assumption per system which is safer for this
        // scope.
        // Ideally: studentRepository.findByRollNumberAndSessionId(...)

        Optional<Student> studentOpt = studentRepository.findByRollNumber(rollNumber);

        if (!studentOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Student with Roll Number " + rollNumber + " not found.");
            return "redirect:/attendance/public";
        }

        Student student = studentOpt.get();

        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "No active session found.");
            return "redirect:/attendance/public";
        }

        // Ensure student belongs to active session to avoid showing old records
        if (!student.getSession().getId().equals(activeSession.get().getId())) {
            redirectAttributes.addFlashAttribute("error", "Student is not in the current active session.");
            return "redirect:/attendance/public";
        }

        AttendanceService.AttendanceSummaryDTO summary = attendanceService.getStudentAttendanceSummary(student.getId(),
                activeSession.get().getId());

        if (summary == null) {
            redirectAttributes.addFlashAttribute("error", "Could not fetch attendance summary.");
            return "redirect:/attendance/public";
        }

        model.addAttribute("summary", summary);
        model.addAttribute("activeSession", activeSession.get());

        return "attendance-public-view";
    }
}
