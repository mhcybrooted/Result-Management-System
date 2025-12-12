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
    private DashboardService dashboardService; // Injected DashboardService

    @Autowired
    private TeacherAssignmentService teacherAssignmentService;

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
    public String adminDashboard(Model model) {
        // Get active session
        Optional<Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
        }

        // Add statistics
        model.addAttribute("totalStudents", examService.getAllStudents().size());
        model.addAttribute("totalTeachers", teacherService.getAllActiveTeachers().size());
        model.addAttribute("totalSubjects", examService.getAllSubjects().size());
        model.addAttribute("totalExams", examService.getAllActiveExams().size());

        // Add Pass/Fail & GPA Statistics from DashboardService
        Map<String, Object> stats = dashboardService.getDashboardStats();
        model.addAttribute("totalPass", stats.get("totalPass"));
        model.addAttribute("totalFail", stats.get("totalFail"));
        model.addAttribute("averageGpa", stats.get("averageGpa"));
        model.addAttribute("studentsWithResults", stats.get("studentsWithResults"));
        model.addAttribute("gradeDistribution", stats.get("gradeDistribution"));

        // Add Subject Performance Stats
        Map<String, Double> subjectPerformance = dashboardService.getSubjectPerformance();
        model.addAttribute("subjectPerformance", subjectPerformance);

        return "index";
    }

    // Developer page (public access)
    @GetMapping("/developer")
    public String developerPage() {
        return "developer";
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
            @RequestParam Long sessionId,
            @RequestParam(required = false) Long classId,
            RedirectAttributes redirectAttributes) {
        try {
            // Validate that the subject belongs to the selected class if classId is
            // provided
            if (classId != null) {
                Optional<Subject> subjectOpt = examService.getSubjectById(subjectId);
                if (subjectOpt.isPresent()) {
                    Subject subject = subjectOpt.get();
                    if (subject.getClassEntity() != null &&
                            !subject.getClassEntity().getId().equals(classId)) {
                        redirectAttributes.addFlashAttribute("error",
                                "Selected subject does not belong to the selected class!");
                        return "redirect:/assign-subjects";
                    }
                }
            }

            // Use TeacherAssignmentService to save the assignment
            boolean assignmentCreated = teacherAssignmentService.assignSubjectToTeacher(teacherId, subjectId,
                    sessionId);

            if (assignmentCreated) {
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
    public String listSessions(Model model) {
        List<Session> sessions = examService.getAllSessions();
        model.addAttribute("sessions", sessions);
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
        redirectAttributes.addFlashAttribute("success", "Session added successfully!");
        return "redirect:/sessions";
    }

    @PostMapping("/sessions/{id}/activate")
    public String activateSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (examService.activateSession(id)) {
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
            redirectAttributes.addFlashAttribute("success", "Students promoted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to promote students");
        }
        return "redirect:/students/promote";
    }

    // Exam management pages
    @GetMapping("/exams")
    public String listExams(Model model) {
        List<Exam> exams = examService.getAllExams();
        model.addAttribute("exams", exams);
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
            redirectAttributes.addFlashAttribute("success", "Exam deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete exam");
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

        model.addAttribute("students", students);
        model.addAttribute("subjects", subjects);
        model.addAttribute("exams", exams);
        model.addAttribute("teachers", teachers);
        model.addAttribute("availableClasses", availableClasses);
        model.addAttribute("selectedClass", classFilter);
        return "add-marks";
    }

    // Process add marks form (updated)
    @PostMapping("/add-marks")
    public String addMarks(@RequestParam Long studentId,
            @RequestParam Long subjectId,
            @RequestParam Long examId,
            @RequestParam Integer obtainedMarks,
            @RequestParam Long teacherId,
            RedirectAttributes redirectAttributes) {

        // Validate input
        if (studentId == null || subjectId == null || examId == null || obtainedMarks == null || obtainedMarks < 0
                || teacherId == null) {
            redirectAttributes.addFlashAttribute("error", "Please fill all fields with valid values");
            return "redirect:/add-marks";
        }

        boolean success = examService.addMarks(studentId, subjectId, examId, obtainedMarks, teacherId);

        if (success) {
            redirectAttributes.addFlashAttribute("success", "Marks added successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to add marks. Check if marks exceed maximum allowed or duplicate entry.");
        }

        return "redirect:/add-marks";
    }

    // Student management pages
    @GetMapping("/students")
    public String listStudents(Model model) {
        List<Student> students = examService.getAllStudents();
        model.addAttribute("students", students);
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
            redirectAttributes.addFlashAttribute("success", "Student deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete student");
        }
        return "redirect:/students";
    }

    // Class management pages
    @GetMapping("/classes")
    public String listClasses(Model model) {
        List<mh.cyb.root.rms.entity.Class> classes = examService.getAllClasses();
        model.addAttribute("classes", classes);
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
            redirectAttributes.addFlashAttribute("success", "Class deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete class");
        }
        return "redirect:/classes";
    }

    // Subject management pages
    @GetMapping("/subjects")
    public String listSubjects(Model model) {
        List<Subject> subjects = examService.getAllSubjects();
        model.addAttribute("subjects", subjects);

        // Calculate dynamic stats
        long classesCovered = subjects.stream()
                .map(Subject::getClassName)
                .distinct()
                .count();

        int maxMarks = subjects.stream()
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
            redirectAttributes.addFlashAttribute("success", "Subject deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to delete subject");
        }
        return "redirect:/subjects";
    }

    // Search results
    @PostMapping("/search-results")
    public String searchResults(@RequestParam String rollNumber, Model model) {

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
}
