package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.dto.ReportCardData;
import mh.cyb.root.rms.entity.Session;
import mh.cyb.root.rms.entity.Student;
import mh.cyb.root.rms.service.ExamService;
import mh.cyb.root.rms.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ExamService examService;

    // Add active session to all pages
    @ModelAttribute
    public void addActiveSession(Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
        }
        model.addAttribute("allSessions", examService.getAllSessions());
    }

    // Reports home page
    @GetMapping
    public String reportsHome(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        // Get paginated students
        org.springframework.data.domain.Page<Student> studentPage = examService.getAllStudents(
                org.springframework.data.domain.PageRequest.of(page, size));

        // Get available classes using dedicated method (efficient)
        List<String> availableClasses = examService.getAvailableClasses();

        // Add managed classes that don't already exist (just in case)
        List<mh.cyb.root.rms.entity.Class> managedClasses = examService.getAllActiveClasses();
        managedClasses.stream()
                .map(mh.cyb.root.rms.entity.Class::getClassName)
                .filter(className -> !availableClasses.contains(className))
                .forEach(availableClasses::add);

        // Sort the final list
        List<String> finalClasses = availableClasses.stream()
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("students", studentPage.getContent());
        model.addAttribute("availableClasses", finalClasses);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", studentPage.getTotalPages());
        model.addAttribute("totalItems", studentPage.getTotalElements());

        return "reports";
    }

    // View individual report card
    @GetMapping("/{studentId}")
    public String viewReportCard(@PathVariable Long studentId, Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            model.addAttribute("error", "No active session found");
            return "reports";
        }

        Optional<ReportCardData> reportCard = reportService.generateReportCard(studentId, activeSession.get().getId());

        if (reportCard.isPresent()) {
            model.addAttribute("reportCard", reportCard.get());
            return "report-card";
        } else {
            model.addAttribute("error", "No report card data found for this student");
            return "reports";
        }
    }

    // Download PDF report card
    @GetMapping("/{studentId}/pdf")
    public ResponseEntity<byte[]> downloadReportCardPDF(@PathVariable Long studentId) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<ReportCardData> reportCard = reportService.generateReportCard(studentId, activeSession.get().getId());

        if (reportCard.isPresent()) {
            byte[] pdfBytes = reportService.generatePDF(reportCard.get());

            String filename = reportCard.get().getStudent().getName().toLowerCase().replace(" ", "-")
                    + "-report-" + activeSession.get().getSessionName() + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        }

        return ResponseEntity.notFound().build();
    }

    // Class reports page
    @GetMapping("/class/{className}")
    public String viewClassReports(@PathVariable String className, Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            model.addAttribute("error", "No active session found");
            return "reports";
        }

        List<ReportCardData> classReports = reportService.generateClassReports(className, activeSession.get().getId());

        model.addAttribute("classReports", classReports);
        model.addAttribute("className", className);
        return "class-reports";
    }
}
