package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.entity.AttendanceStatus;
import mh.cyb.root.rms.entity.Session;
import mh.cyb.root.rms.service.AttendanceService;
import mh.cyb.root.rms.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private ExamService examService;

    @GetMapping("/take")
    public String takeAttendancePage(@RequestParam(required = false) String className,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            model.addAttribute("error", "No active session found");
            return "attendance-take";
        }

        if (date == null)
            date = LocalDate.now();

        model.addAttribute("date", date);
        model.addAttribute("className", className);
        model.addAttribute("availableClasses", examService.getAvailableClasses());

        if (className != null && !className.isEmpty()) {
            List<AttendanceService.AttendanceDTO> sheet = attendanceService.getDailySheet(className, date);
            boolean isRecorded = sheet.stream().anyMatch(d -> d.getAttendanceId() != null);

            model.addAttribute("sheet", sheet);
            model.addAttribute("isRecorded", isRecorded);
            model.addAttribute("formWrapper", new AttendanceFormWrapper(sheet));

            // Navigation
            model.addAttribute("prevDate", date.minusDays(1));
            model.addAttribute("nextDate", date.plusDays(1));
        }

        return "attendance-take";
    }

    @PostMapping("/save")
    public String saveAttendance(@ModelAttribute AttendanceFormWrapper formWrapper,
            @RequestParam String className,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            RedirectAttributes redirectAttributes) {

        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            attendanceService.saveBulkAttendance(formWrapper.getSheet(), date, activeSession.get().getId());
            redirectAttributes.addFlashAttribute("success", "Attendance saved for " + className + " on " + date);
        }

        return "redirect:/attendance/take?className=" + className + "&date=" + date;
    }

    // Wrapper for List Binding
    public static class AttendanceFormWrapper {
        private List<AttendanceService.AttendanceDTO> sheet;

        public AttendanceFormWrapper() {
        }

        public AttendanceFormWrapper(List<AttendanceService.AttendanceDTO> sheet) {
            this.sheet = sheet;
        }

        public List<AttendanceService.AttendanceDTO> getSheet() {
            return sheet;
        }

        public void setSheet(List<AttendanceService.AttendanceDTO> sheet) {
            this.sheet = sheet;
        }
    }
}
