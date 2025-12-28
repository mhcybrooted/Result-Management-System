package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.entity.Session;
import mh.cyb.root.rms.entity.Student;
import mh.cyb.root.rms.service.ExamService;
import mh.cyb.root.rms.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class SearchController {
    
    @Autowired
    private SearchService searchService;
    
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
    
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        Optional<Session> activeSession = examService.getActiveSession();
        if (!activeSession.isPresent()) {
            model.addAttribute("error", "No active session found");
            return "search-results";
        }
        
        List<Student> students;
        if (q != null && !q.trim().isEmpty()) {
            students = searchService.searchStudents(q, activeSession.get().getId());
            model.addAttribute("query", q);
        } else {
            students = examService.getAllStudents();
            model.addAttribute("query", "");
        }
        
        model.addAttribute("students", students);
        return "search-results";
    }
}
