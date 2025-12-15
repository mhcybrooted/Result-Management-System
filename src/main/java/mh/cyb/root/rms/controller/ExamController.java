package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.dto.Result;
import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.repository.TeacherRepository;
import mh.cyb.root.rms.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private TeacherService teacherService; // Kept as it's used in adminDashboard

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ActivityLogService activityLogService; // Injected for logging

    @Autowired
    private TeacherAssignmentService teacherAssignmentService;

    @Autowired
    private mh.cyb.root.rms.repository.AdminUserRepository adminUserRepository;

    @org.springframework.beans.factory.annotation.Value("${dashboard.top.performers.limit:50}")
    private int topPerformersLimit;

    // Public home page
    @GetMapping("/")
    public String homePage(Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
        }
        return "public-home";
    }

    // Public view results (no authentication required)
    @GetMapping("/view-results")
    public String publicViewResults(Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
        }
        return "public-view-results";
    }

    // Admin view results (admin authentication required)
    @GetMapping("/admin/view-results")
    public String adminViewResults(Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
        }
        return "view-results";
    }

    // Admin dashboard (admin authentication required)
    @GetMapping("/admin/dashboard")
    public String adminDashboard(@RequestParam(defaultValue = "0") int performersPage,
            @RequestParam(defaultValue = "0") int atRiskPage, Model model) {
        // Get active session
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
        }

        // RBAC: Check user role
        String username = securityServiceGetUsername();
        Optional<AdminUser> adminUserOpt = adminUserRepository.findByUsername(username);
        boolean isTeacher = adminUserOpt.isPresent() && "TEACHER".equals(adminUserOpt.get().getRole());
        model.addAttribute("isTeacher", isTeacher);

        if (isTeacher) {
            Long teacherId = adminUserOpt.get().getTeacherId();

            if (activeSession.isPresent() && teacherId != null) {
                Map<String, Object> teacherStats = dashboardService.getTeacherStats(teacherId,
                        activeSession.get().getId());
                model.addAttribute("totalStudents", teacherStats.get("totalStudents")); // My Students
                model.addAttribute("totalSubjects", teacherStats.get("totalSubjects")); // My Subjects
                model.addAttribute("totalPass", teacherStats.get("totalPass")); // Subject-level Pass
                model.addAttribute("totalFail", teacherStats.get("totalFail")); // Subject-level Fail
                model.addAttribute("averageGpa", teacherStats.get("averageGpa")); // Average GPA
                model.addAttribute("subjectPerformance", teacherStats.get("subjectPerformance"));
                model.addAttribute("gradeDistribution", teacherStats.get("gradeDistribution")); // Grade Distribution
                                                                                                // Chart

                // Teachers don't manage other teachers/exams
                model.addAttribute("totalTeachers", 0);
                model.addAttribute("totalExams", 0);
            }
        } else {
            // Super Admin (Global Stats)
            model.addAttribute("totalStudents", examService.getAllStudents().size());
            model.addAttribute("totalTeachers", teacherService.getAllActiveTeachers().size());
            model.addAttribute("totalSubjects", examService.getAllSubjects().size());
            model.addAttribute("totalExams", examService.getAllActiveExams().size());

            Map<String, Object> stats = dashboardService.getDashboardStats();
            model.addAttribute("totalPass", stats.get("totalPass"));
            model.addAttribute("totalFail", stats.get("totalFail"));
            model.addAttribute("averageGpa", stats.get("averageGpa"));
            model.addAttribute("subjectPerformance", dashboardService.getSubjectPerformance());
            model.addAttribute("studentsWithResults", stats.get("studentsWithResults"));
            model.addAttribute("gradeDistribution", stats.get("gradeDistribution"));
        }

        // Common Data
        int performersSize = topPerformersLimit;
        List<Result> topPerformers = dashboardService.getTopPerformers(performersPage, performersSize);
        int totalPerformers = dashboardService.getTopPerformersCount();
        int totalPages = (int) Math.ceil((double) totalPerformers / performersSize);

        model.addAttribute("topPerformers", topPerformers);
        model.addAttribute("topPerformersLimit", topPerformersLimit);
        model.addAttribute("performersPage", performersPage);
        model.addAttribute("performersTotalPages", totalPages);

        // At-Risk Students Data
        int atRiskSize = 5;
        List<Result> atRiskStudents = dashboardService.getAtRiskStudents(atRiskPage, atRiskSize);
        int totalAtRisk = dashboardService.getAtRiskStudentsCount();
        int totalAtRiskPages = (int) Math.ceil((double) totalAtRisk / atRiskSize);

        model.addAttribute("atRiskStudents", atRiskStudents);
        model.addAttribute("atRiskPage", atRiskPage);
        model.addAttribute("atRiskTotalPages", totalAtRiskPages);

        return "index";
    }

    // Developer page (public access)
    @GetMapping("/developer")
    public String developerPage() {
        return "developer";
    }

    // Activity Logs (Admin only)
    @GetMapping("/admin/activity-logs")
    public String viewActivityLogs(Model model) {
        model.addAttribute("logs", activityLogService.getAllLogs());
        return "activity-logs";
    }

    // Assign subjects page
    @GetMapping("/assign-subjects")
    public String assignSubjects(Model model, @RequestParam(required = false) Long teacherId) {
        model.addAttribute("teachers", teacherService.getAllActiveTeachers());
        model.addAttribute("subjects", examService.getAllSubjects());
        model.addAttribute("classes", examService.getAllClasses());

        // Get real assignment data from database
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
            // Get all active assignments for current session
            List<TeacherAssignment> assignments = teacherAssignmentService
                    .getAllActiveAssignments(activeSession.get().getId());
            model.addAttribute("assignments", assignments);
        } else {
            model.addAttribute("assignments", java.util.Collections.emptyList());
        }

        if (teacherId != null) {
            model.addAttribute("selectedTeacherId", teacherId);
        }
        return "assign-subjects";
    }

    // Process subject assignment
    @PostMapping("/assign-subjects")
    public String processAssignSubjects(@RequestParam Long teacherId,
            @RequestParam Long subjectId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long classId,
            RedirectAttributes redirectAttributes) {

        if (sessionId == null) {
            redirectAttributes.addFlashAttribute("error", "No active session found! Cannot assign subjects.");
            return "redirect:/assign-subjects";
        }
        try {
            // Validate Subject and Class consistency
            Optional<Subject> subjectOpt = examService.getSubjectById(subjectId);
            if (!subjectOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Invalid subject selected");
                return "redirect:/assign-subjects";
            }

            Subject subject = subjectOpt.get();

            // If classId is provided, ensure it matches subject's class
            if (classId != null) {
                if (subject.getClassEntity() != null && !subject.getClassEntity().getId().equals(classId)) {
                    redirectAttributes.addFlashAttribute("error",
                            "Selected subject does not belong to the selected class!");
                    return "redirect:/assign-subjects";
                }
            } else {
                // If classId NOT provided, but subject has one, strictly enforce strict
                // assignment if needed?
                // For now, if subject has a class, we implicitly accept it.
                // But we could warn if the subject is "Class 10 Math" and we didn't specify
                // class.
                // Actually, let's just ensure we don't assign inactive class subjects.
                if (subject.getClassEntity() != null && !subject.getClassEntity().getActive()) {
                    redirectAttributes.addFlashAttribute("error", "Cannot assign subject from an inactive class!");
                    return "redirect:/assign-subjects";
                }
            }

            // Use TeacherAssignmentService to save the assignment
            boolean assignmentCreated = teacherAssignmentService.assignSubjectToTeacher(teacherId, subjectId,
                    sessionId);

            if (assignmentCreated) {
                // Log Action
                String username = securityServiceGetUsername();
                activityLogService.logAction("ASSIGN_SUBJECT",
                        "Assigned Subject ID: " + subjectId + " to Teacher ID: " + teacherId, username, null);

                redirectAttributes.addFlashAttribute("success", "Subject assigned successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "This teacher is already assigned to this subject!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to assign subject: " + e.getMessage());
        }
        return "redirect:/assign-subjects";
    }

    // Edit teacher page
    @GetMapping("/teachers/edit/{id}")
    public String editTeacher(@PathVariable Long id, Model model) {
        // Redirect to add-teacher page with teacher data for editing
        return "redirect:/add-teacher?id=" + id;
    }

    // Remove assignment
    @PostMapping("/assign-subjects/remove")
    public String removeAssignment(@RequestParam Long assignmentId, RedirectAttributes redirectAttributes) {
        try {
            teacherAssignmentService.removeAssignment(assignmentId);

            // Log Action
            String username = securityServiceGetUsername();
            activityLogService.logAction("REMOVE_ASSIGNMENT", "Removed Assignment ID: " + assignmentId, username, null);

            redirectAttributes.addFlashAttribute("success", "Assignment removed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to remove assignment: " + e.getMessage());
        }
        return "redirect:/assign-subjects";
    }

    // Add active session to all pages
    @ModelAttribute
    public void addActiveSession(Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
        }
        model.addAttribute("allSessions", examService.getAllSessions());
    }

    // Session management pages
    @GetMapping("/sessions")
    public String listSessions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        org.springframework.data.domain.Page<Session> sessionPage = examService.getAllSessions(
                org.springframework.data.domain.PageRequest.of(page, size));
        model.addAttribute("sessions", sessionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", sessionPage.getTotalPages());
        model.addAttribute("totalItems", sessionPage.getTotalElements());
        return "sessions";
    }

    @GetMapping("/sessions/add")
    public String addSessionPage(Model model) {
        model.addAttribute("academicSession", new Session());
        return "add-session";
    }

    @PostMapping("/sessions/add")
    public String addSession(@ModelAttribute("academicSession") Session session,
            RedirectAttributes redirectAttributes) {
        if (session.getSessionName() == null || session.getSessionName().trim().isEmpty() ||
                session.getStartDate() == null || session.getEndDate() == null) {
            redirectAttributes.addFlashAttribute("error", "Please fill all fields");
            return "redirect:/sessions/add";
        }

        examService.saveSession(session);

        // Log Action
        String username = securityServiceGetUsername();
        activityLogService.logAction("ADD_SESSION", "Created session: " + session.getSessionName(), username, null);

        redirectAttributes.addFlashAttribute("success", "Session added successfully!");
        return "redirect:/sessions";
    }

    @PostMapping("/sessions/{id}/activate")
    public String activateSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (examService.activateSession(id)) {
            // Log Action
            String username = securityServiceGetUsername();
            activityLogService.logAction("ACTIVATE_SESSION", "Activated Session ID: " + id, username, null);

            redirectAttributes.addFlashAttribute("success", "Session activated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to activate session");
        }
        return "redirect:/sessions";
    }

    // Student promotion
    @GetMapping("/students/promote")
    public String promoteStudentsPage(Model model) {
        List<Student> students = examService.getAllStudents();
        List<Session> sessions = examService.getAllSessions();
        model.addAttribute("students", students);
        model.addAttribute("sessions", sessions);
        return "promote-students";
    }

    @PostMapping("/students/promote")
    public String promoteStudents(@RequestParam List<Long> studentIds,
            @RequestParam Long targetSessionId,
            RedirectAttributes redirectAttributes) {

        if (examService.promoteStudents(studentIds, targetSessionId)) {
            // Log Action
            String username = securityServiceGetUsername();
            activityLogService.logAction("PROMOTE_STUDENTS",
                    "Promoted " + studentIds.size() + " students to new session", username, null);

            redirectAttributes.addFlashAttribute("success", "Students promoted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to promote students");
        }
        return "redirect:/students/promote";
    }

    // Exam management pages
    @GetMapping("/exams")
    public String listExams(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        org.springframework.data.domain.Page<Exam> examPage = examService.getAllExams(
                org.springframework.data.domain.PageRequest.of(page, size));
        model.addAttribute("exams", examPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", examPage.getTotalPages());
        model.addAttribute("totalItems", examPage.getTotalElements());
        return "exams";
    }

    @GetMapping("/exams/add")
    public String addExamPage(Model model) {
        model.addAttribute("exam", new Exam());
        return "add-exam";
    }

    @PostMapping("/exams/add")
    public String addExam(@ModelAttribute Exam exam, RedirectAttributes redirectAttributes) {
        if (exam.getExamName() == null || exam.getExamName().trim().isEmpty() || exam.getExamDate() == null) {
            redirectAttributes.addFlashAttribute("error", "Please fill all fields");
            return "redirect:/exams/add";
        }

        examService.saveExam(exam);

        // Log Action
        String username = securityServiceGetUsername();
        activityLogService.logAction("ADD_EXAM", "Scheduled exam: " + exam.getExamName(), username, null);

        redirectAttributes.addFlashAttribute("success", "Exam added successfully!");
        return "redirect:/exams";
    }

    @GetMapping("/exams/edit/{id}")
    public String editExamPage(@PathVariable Long id, Model model) {
        Optional<Exam> exam = examService.getExamById(id);
        if (exam.isPresent()) {
            model.addAttribute("exam", exam.get());
            return "add-exam";
        }
        return "redirect:/exams";
    }

    @PostMapping("/exams/delete/{id}")
    public String deleteExam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (examService.deleteExam(id)) {
            // Log Action
            String username = securityServiceGetUsername();
            activityLogService.logAction("DEACTIVATE_EXAM", "Deactivated Exam ID: " + id, username, null);

            redirectAttributes.addFlashAttribute("success", "Exam deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete exam");
        }
        return "redirect:/exams";
    }

    @PostMapping("/exams/activate/{id}")
    public String activateExam(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (examService.activateExam(id)) {
            // Log Action
            String username = securityServiceGetUsername();
            activityLogService.logAction("ACTIVATE_EXAM", "Activated Exam ID: " + id, username, null);

            redirectAttributes.addFlashAttribute("success", "Exam activated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to activate exam");
        }
        return "redirect:/exams";
    }

    // Add marks page (updated with class filter and managed subjects)
    @GetMapping("/add-marks")
    public String addMarksPage(@RequestParam(required = false) String classFilter, Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            model.addAttribute("error", "No active session found");
            return "add-marks";
        }

        List<Student> students;
        if (classFilter != null && !classFilter.trim().isEmpty()) {
            students = examService.getAllStudents().stream()
                    .filter(s -> s.getClassName().equals(classFilter))
                    .collect(Collectors.toList());
        } else {
            students = examService.getAllStudents();
        }

        // Get subjects - filter by class if class filter is applied
        List<Subject> subjects;
        if (classFilter != null && !classFilter.trim().isEmpty()) {
            subjects = examService.getAllSubjects().stream()
                    .filter(s -> s.getClassName().equals(classFilter))
                    .collect(Collectors.toList());
            // If no subjects for the filtered class, show all subjects
            if (subjects.isEmpty()) {
                subjects = examService.getAllSubjects();
            }
        } else {
            subjects = examService.getAllSubjects();
        }

        List<Exam> exams = examService.getAllActiveExams();
        List<Teacher> teachers = examService.getAllActiveTeachers();

        // Get available classes for filter dropdown
        List<String> availableClasses = examService.getAllStudents().stream()
                .map(Student::getClassName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // RBAC: Filter filtering logic for Teachers
        String username = securityServiceGetUsername();
        Optional<AdminUser> adminUserOpt = adminUserRepository.findByUsername(username);

        boolean isTeacher = false;
        if (adminUserOpt.isPresent() && "TEACHER".equals(adminUserOpt.get().getRole())) {
            isTeacher = true;
            Long teacherId = adminUserOpt.get().getTeacherId();
            if (activeSession.isPresent() && teacherId != null) {
                // Get ONLY assigned subjects
                List<Subject> assignedSubjects = teacherAssignmentService.getAssignedSubjects(teacherId,
                        activeSession.get().getId());

                // Intersect with existing subjects filter if present
                if (classFilter != null && !classFilter.trim().isEmpty()) {
                    subjects = subjects.stream()
                            .filter(s -> assignedSubjects.stream().anyMatch(as -> as.getId().equals(s.getId())))
                            .collect(Collectors.toList());
                } else {
                    subjects = assignedSubjects;
                }
            }
        }

        model.addAttribute("students", students);
        model.addAttribute("subjects", subjects);
        model.addAttribute("exams", exams);
        model.addAttribute("teachers", teachers);
        model.addAttribute("availableClasses", availableClasses);
        model.addAttribute("selectedClass", classFilter);
        model.addAttribute("isTeacher", isTeacher);
        if (isTeacher && adminUserOpt.get().getTeacherId() != null) {
            model.addAttribute("currentTeacherId", adminUserOpt.get().getTeacherId());
        }
        return "add-marks";
    }

    // Process add marks form (updated)
    @PostMapping("/add-marks")
    public String addMarks(@RequestParam Long studentId,
            @RequestParam Long subjectId,
            @RequestParam Long examId,
            @RequestParam Integer obtainedMarks,
            @RequestParam(required = false) Long teacherId, // Optional for Teacher role
            RedirectAttributes redirectAttributes) {

        // Validate basic input
        if (studentId == null || subjectId == null || examId == null || obtainedMarks == null || obtainedMarks < 0) {
            redirectAttributes.addFlashAttribute("error", "Please fill all fields with valid values");
            return "redirect:/add-marks";
        }

        // RBAC: Determine real Teacher ID
        String username = securityServiceGetUsername();
        Optional<AdminUser> adminUserOpt = adminUserRepository.findByUsername(username);

        Long validTeacherId = teacherId;

        if (adminUserOpt.isPresent()) {
            AdminUser adminUser = adminUserOpt.get();
            // If user is a TEACHER, force override teacherId and validate
            if ("TEACHER".equals(adminUser.getRole())) {
                validTeacherId = adminUser.getTeacherId();
                if (validTeacherId == null) {
                    redirectAttributes.addFlashAttribute("error",
                            "Your account is not linked to a Teacher profile. Contact Super Admin.");
                    return "redirect:/add-marks";
                }

                // Validate if this teacher is assigned to this subject
                // Ideally this should also check Session ID, but obtaining it might require
                // Extra DB call or Exam lookup.
                // For now, we trust TeacherAssignmentService to check active assignments.
                Optional<Session> activeSession = examService.getActiveSession();
                if (activeSession.isPresent()) {
                    List<mh.cyb.root.rms.entity.Subject> assignedSubjects = teacherAssignmentService
                            .getAssignedSubjects(validTeacherId, activeSession.get().getId());
                    boolean isAssigned = assignedSubjects.stream().anyMatch(s -> s.getId().equals(subjectId));
                    if (!isAssigned) {
                        redirectAttributes.addFlashAttribute("error",
                                "Security Alert: You are not authorized to add marks for this subject.");
                        return "redirect:/add-marks";
                    }
                }
            } else {
                // For SUPER_ADMIN, teacherId field is required
                if (teacherId == null) {
                    redirectAttributes.addFlashAttribute("error", "Please select a teacher");
                    return "redirect:/add-marks";
                }
            }
        }

        boolean success = examService.addMarks(studentId, subjectId, examId, obtainedMarks, validTeacherId);

        if (success) {
            // Log Action
            activityLogService.logAction("ADD_MARKS",
                    "Added marks: " + obtainedMarks + " for Student ID: " + studentId + ", Subject ID: " + subjectId,
                    username, null);

            redirectAttributes.addFlashAttribute("success", "Marks added successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to add marks. Check if marks exceed maximum allowed or duplicate entry.");
        }

        return "redirect:/add-marks";
    }

    // Student management pages
    @GetMapping("/students")
    public String listStudents(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        String username = securityServiceGetUsername();
        Optional<AdminUser> adminUserOpt = adminUserRepository.findByUsername(username);
        boolean isTeacher = adminUserOpt.isPresent() && "TEACHER".equals(adminUserOpt.get().getRole());
        model.addAttribute("isTeacher", isTeacher);

        if (isTeacher) {
            Long teacherId = adminUserOpt.get().getTeacherId();
            Optional<Session> activeSession = examService.getActiveSession();

            if (activeSession.isPresent() && teacherId != null) {
                // Get all assigned students
                List<Student> allStudents = teacherAssignmentService.getStudentsForTeacher(teacherId,
                        activeSession.get().getId());

                // Manual in-memory pagination for teachers
                int start = Math.min(page * size, allStudents.size());
                int end = Math.min(start + size, allStudents.size());
                List<Student> pageContent = allStudents.subList(start, end);

                model.addAttribute("students", pageContent);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", (int) Math.ceil((double) allStudents.size() / size));
                model.addAttribute("totalItems", allStudents.size());
            } else {
                model.addAttribute("students", new ArrayList<>());
                model.addAttribute("currentPage", 0);
                model.addAttribute("totalPages", 0);
                model.addAttribute("totalItems", 0);
            }
        } else {
            // Super Admin (All Students) - Server-side Pagination
            org.springframework.data.domain.Page<Student> studentPage = examService
                    .getAllStudents(org.springframework.data.domain.PageRequest.of(page, size));
            model.addAttribute("students", studentPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", studentPage.getTotalPages());
            model.addAttribute("totalItems", studentPage.getTotalElements());
        }
        return "students";
    }

    @GetMapping("/students/add")
    public String addStudentPage(Model model) {
        model.addAttribute("student", new Student());

        // Get available classes - prioritize managed classes
        List<String> availableClasses = new ArrayList<>();

        // First, add active classes from Class entity (managed classes)
        List<mh.cyb.root.rms.entity.Class> classEntities = examService.getAllActiveClasses();
        availableClasses.addAll(classEntities.stream()
                .map(mh.cyb.root.rms.entity.Class::getClassName)
                .collect(Collectors.toList()));

        // Only add standard classes if no managed classes exist
        if (availableClasses.isEmpty()) {
            availableClasses.addAll(List.of("Class 1", "Class 2", "Class 3", "Class 4", "Class 5",
                    "Class 6", "Class 7", "Class 8", "Class 9", "Class 10",
                    "Class 11", "Class 12"));
        }

        model.addAttribute("availableClasses",
                availableClasses.stream().distinct().sorted().collect(Collectors.toList()));
        return "add-student";
    }

    @PostMapping("/students/add")
    public String addStudent(@ModelAttribute Student student, RedirectAttributes redirectAttributes) {
        if (student.getName() == null || student.getName().trim().isEmpty() ||
                student.getRollNumber() == null || student.getRollNumber().trim().isEmpty() ||
                student.getClassName() == null || student.getClassName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please fill all fields");
            return "redirect:/students/add";
        }

        examService.saveStudent(student);

        // Log Action
        String username = securityServiceGetUsername();
        activityLogService.logAction("ADD_STUDENT",
                "Added student: " + student.getName() + " (Roll: " + student.getRollNumber() + ")", username, null);

        redirectAttributes.addFlashAttribute("success", "Student added successfully!");
        return "redirect:/students";
    }

    @GetMapping("/students/edit/{id}")
    public String editStudentPage(@PathVariable Long id, Model model) {
        Optional<Student> student = examService.getStudentById(id);
        if (student.isPresent()) {
            model.addAttribute("student", student.get());

            // Get available classes - prioritize managed classes
            List<String> availableClasses = new ArrayList<>();

            // First, add active classes from Class entity (managed classes)
            List<mh.cyb.root.rms.entity.Class> classEntities = examService.getAllActiveClasses();
            availableClasses.addAll(classEntities.stream()
                    .map(mh.cyb.root.rms.entity.Class::getClassName)
                    .collect(Collectors.toList()));

            // Add current student's class if not in managed classes (for backward
            // compatibility)
            String currentClass = student.get().getClassName();
            if (!availableClasses.contains(currentClass)) {
                availableClasses.add(currentClass);
            }

            // Only add standard classes if no managed classes exist
            if (classEntities.isEmpty()) {
                availableClasses.addAll(List.of("Class 1", "Class 2", "Class 3", "Class 4", "Class 5",
                        "Class 6", "Class 7", "Class 8", "Class 9", "Class 10",
                        "Class 11", "Class 12"));
            }

            model.addAttribute("availableClasses",
                    availableClasses.stream().distinct().sorted().collect(Collectors.toList()));
            return "add-student";
        }
        return "redirect:/students";
    }

    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (examService.deleteStudent(id)) {
            // Log Action
            String username = securityServiceGetUsername();
            activityLogService.logAction("DELETE_STUDENT", "Deleted Student ID: " + id, username, null);

            redirectAttributes.addFlashAttribute("success", "Student deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete student");
        }
        return "redirect:/students";
    }

    // Class management pages
    @GetMapping("/classes")
    public String listClasses(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        org.springframework.data.domain.Page<mh.cyb.root.rms.entity.Class> classPage = examService.getAllClasses(
                org.springframework.data.domain.PageRequest.of(page, size));
        model.addAttribute("classes", classPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", classPage.getTotalPages());
        model.addAttribute("totalItems", classPage.getTotalElements());
        return "classes";
    }

    @GetMapping("/classes/add")
    public String addClassPage(Model model) {
        model.addAttribute("classEntity", new mh.cyb.root.rms.entity.Class());
        return "add-class";
    }

    @PostMapping("/classes/add")
    public String addClass(@ModelAttribute("classEntity") mh.cyb.root.rms.entity.Class classEntity,
            RedirectAttributes redirectAttributes) {
        if (classEntity.getClassName() == null || classEntity.getClassName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please fill class name");
            return "redirect:/classes/add";
        }

        examService.saveClass(classEntity);

        // Log Action
        String username = securityServiceGetUsername();
        activityLogService.logAction("ADD_CLASS", "Added Class: " + classEntity.getClassName(), username, null);

        redirectAttributes.addFlashAttribute("success", "Class added successfully!");
        return "redirect:/classes";
    }

    @GetMapping("/classes/edit/{id}")
    public String editClassPage(@PathVariable Long id, Model model) {
        Optional<mh.cyb.root.rms.entity.Class> classEntity = examService.getClassById(id);
        if (classEntity.isPresent()) {
            model.addAttribute("classEntity", classEntity.get());
            return "add-class";
        }
        return "redirect:/classes";
    }

    @PostMapping("/classes/delete/{id}")
    public String deleteClass(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (examService.deleteClass(id)) {
            // Log Action
            String username = securityServiceGetUsername();
            activityLogService.logAction("DELETE_CLASS", "Deleted Class ID: " + id, username, null);

            redirectAttributes.addFlashAttribute("success", "Class deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete class");
        }
        return "redirect:/classes";
    }

    // Subject management pages
    @GetMapping("/subjects")
    public String listSubjects(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        String username = securityServiceGetUsername();
        Optional<AdminUser> adminUserOpt = adminUserRepository.findByUsername(username);
        boolean isTeacher = adminUserOpt.isPresent() && "TEACHER".equals(adminUserOpt.get().getRole());
        model.addAttribute("isTeacher", isTeacher);

        List<Subject> allSubjectsForStats;
        List<Subject> pageContent;
        int currentPage = page;
        int totalPages;
        long totalItems;

        if (isTeacher) {
            Long teacherId = adminUserOpt.get().getTeacherId();
            Optional<Session> activeSession = examService.getActiveSession();

            if (activeSession.isPresent() && teacherId != null) {
                allSubjectsForStats = teacherAssignmentService.getAssignedSubjects(teacherId,
                        activeSession.get().getId());
            } else {
                allSubjectsForStats = new ArrayList<>();
            }

            // Manual Pagination
            int start = Math.min(page * size, allSubjectsForStats.size());
            int end = Math.min(start + size, allSubjectsForStats.size());
            pageContent = allSubjectsForStats.subList(start, end);
            totalPages = (int) Math.ceil((double) allSubjectsForStats.size() / size);
            totalItems = allSubjectsForStats.size();
        } else {
            // Admin (All Subjects)
            // fetch all for stats (optimization: could count in DB, but this is simple for
            // now)
            allSubjectsForStats = examService.getAllSubjects();

            // Server-side Pagination
            org.springframework.data.domain.Page<Subject> subjectPage = examService
                    .getAllSubjects(org.springframework.data.domain.PageRequest.of(page, size));
            pageContent = subjectPage.getContent();
            totalPages = subjectPage.getTotalPages();
            totalItems = subjectPage.getTotalElements();
        }

        model.addAttribute("subjects", pageContent);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);

        // Calculate dynamic stats from FULL list
        long classesCovered = allSubjectsForStats.stream()
                .map(Subject::getClassName)
                .distinct()
                .count();

        int maxMarks = allSubjectsForStats.stream()
                .mapToInt(Subject::getMaxMarks)
                .max()
                .orElse(0);

        model.addAttribute("classesCovered", classesCovered);
        model.addAttribute("maxMarks", maxMarks);

        return "subjects";
    }

    @GetMapping("/subjects/add")
    public String addSubjectPage(Model model) {
        model.addAttribute("subject", new Subject());
        // Get available classes for subject assignment
        List<mh.cyb.root.rms.entity.Class> availableClasses = examService.getAllActiveClasses();
        model.addAttribute("availableClasses", availableClasses);
        return "add-subject";
    }

    @PostMapping("/subjects/add")
    public String addSubject(@ModelAttribute Subject subject,
            @RequestParam Long classId,
            RedirectAttributes redirectAttributes) {
        if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please fill all required fields");
            return "redirect:/subjects/add";
        }

        if (subject.getMaxMarks() == null || subject.getMaxMarks() <= 0) {
            subject.setMaxMarks(100); // Default max marks
        }

        // Set class entity
        Optional<mh.cyb.root.rms.entity.Class> classEntity = examService.getClassById(classId);
        if (classEntity.isPresent()) {
            subject.setClassEntity(classEntity.get());
            examService.saveSubject(subject);

            // Log Action
            String username = securityServiceGetUsername();
            String action = subject.getId() != null ? "UPDATE_SUBJECT" : "ADD_SUBJECT";
            activityLogService.logAction(action,
                    "Saved subject: " + subject.getSubjectName(),
                    username, null);

            String message = subject.getId() != null ? "Subject updated successfully!" : "Subject added successfully!";
            redirectAttributes.addFlashAttribute("success", message);
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid class selected");
        }

        return "redirect:/subjects";
    }

    @GetMapping("/subjects/edit/{id}")
    public String editSubjectPage(@PathVariable Long id, Model model) {
        Optional<Subject> subject = examService.getSubjectById(id);
        if (subject.isPresent()) {
            model.addAttribute("subject", subject.get());

            // Get available classes
            List<mh.cyb.root.rms.entity.Class> availableClasses = examService.getAllActiveClasses();
            model.addAttribute("availableClasses", availableClasses);
            return "add-subject";
        }
        return "redirect:/subjects";
    }

    @PostMapping("/subjects/delete/{id}")
    public String deleteSubject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (examService.deleteSubject(id)) {
            // Log Action
            String username = securityServiceGetUsername();
            activityLogService.logAction("DELETE_SUBJECT", "Deleted Subject ID: " + id, username, null);

            redirectAttributes.addFlashAttribute("success", "Subject deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete subject");
        }
        return "redirect:/subjects";
    }

    // Search results
    @PostMapping("/search-results")
    public String searchResults(@RequestParam String rollNumber, Model model) {
        // RBAC Security Check
        String username = securityServiceGetUsername();
        Optional<AdminUser> adminUserOpt = adminUserRepository.findByUsername(username);

        if (adminUserOpt.isPresent() && "TEACHER".equals(adminUserOpt.get().getRole())) {
            Long teacherId = adminUserOpt.get().getTeacherId();
            Optional<Session> activeSession = examService.getActiveSession();
            if (activeSession.isPresent()) {
                List<Student> allowedStudents = teacherAssignmentService.getStudentsForTeacher(teacherId,
                        activeSession.get().getId());
                boolean isAllowed = allowedStudents.stream().anyMatch(s -> s.getRollNumber().equals(rollNumber));
                if (!isAllowed) {
                    model.addAttribute("error",
                            "Access Denied: You can only view results for students in your classes.");
                    return "view-results";
                }
            }
        }

        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            model.addAttribute("error", "Please enter a roll number");
            return "view-results";
        }

        Optional<Result> result = examService.getResultByRollNumber(rollNumber.trim());

        if (result.isPresent()) {
            model.addAttribute("result", result.get());
        } else {
            model.addAttribute("error", "No results found for roll number: " + rollNumber);
        }

        return "view-results";
    }

    // Helper to get username safely
    private String securityServiceGetUsername() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return "Unknown";
    }
}
