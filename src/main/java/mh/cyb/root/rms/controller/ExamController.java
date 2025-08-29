package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.dto.Result;
import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class ExamController {
    
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
    
    // Home page
    @GetMapping("/")
    public String home() {
        return "index";
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
    public String addSession(@ModelAttribute("academicSession") Session session, RedirectAttributes redirectAttributes) {
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
    
    // Add marks page (updated with class filter)
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
        
        List<Subject> subjects = examService.getAllSubjects();
        List<Exam> exams = examService.getAllActiveExams();
        
        // Get available classes for filter dropdown
        List<String> availableClasses = examService.getAllStudents().stream()
                .map(Student::getClassName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        model.addAttribute("students", students);
        model.addAttribute("subjects", subjects);
        model.addAttribute("exams", exams);
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
                          RedirectAttributes redirectAttributes) {
        
        // Validate input
        if (studentId == null || subjectId == null || examId == null || obtainedMarks == null || obtainedMarks < 0) {
            redirectAttributes.addFlashAttribute("error", "Please fill all fields with valid values");
            return "redirect:/add-marks";
        }
        
        boolean success = examService.addMarks(studentId, subjectId, examId, obtainedMarks);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Marks added successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to add marks. Check if marks exceed maximum allowed or duplicate entry.");
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
        
        // Get available classes from Class entity and existing student classes
        List<String> availableClasses = new ArrayList<>();
        
        // Add classes from Class entity
        List<mh.cyb.root.rms.entity.Class> classEntities = examService.getAllActiveClasses();
        availableClasses.addAll(classEntities.stream()
                .map(mh.cyb.root.rms.entity.Class::getClassName)
                .collect(Collectors.toList()));
        
        // Add existing student classes
        availableClasses.addAll(examService.getAvailableClasses());
        
        // Add standard class options
        availableClasses.addAll(List.of("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", 
                                       "Class 6", "Class 7", "Class 8", "Class 9", "Class 10", 
                                       "Class 11", "Class 12"));
        
        model.addAttribute("availableClasses", availableClasses.stream().distinct().sorted().collect(Collectors.toList()));
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
            
            // Get available classes from Class entity and existing student classes
            List<String> availableClasses = new ArrayList<>();
            
            // Add classes from Class entity
            List<mh.cyb.root.rms.entity.Class> classEntities = examService.getAllActiveClasses();
            availableClasses.addAll(classEntities.stream()
                    .map(mh.cyb.root.rms.entity.Class::getClassName)
                    .collect(Collectors.toList()));
            
            // Add existing student classes
            availableClasses.addAll(examService.getAvailableClasses());
            
            // Add standard class options
            availableClasses.addAll(List.of("Class 1", "Class 2", "Class 3", "Class 4", "Class 5", 
                                           "Class 6", "Class 7", "Class 8", "Class 9", "Class 10", 
                                           "Class 11", "Class 12"));
            
            model.addAttribute("availableClasses", availableClasses.stream().distinct().sorted().collect(Collectors.toList()));
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
    public String addClass(@ModelAttribute("classEntity") mh.cyb.root.rms.entity.Class classEntity, RedirectAttributes redirectAttributes) {
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
    
    // View results page
    @GetMapping("/view-results")
    public String viewResultsPage() {
        return "view-results";
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
