package mh.cyb.root.rms.service;

import mh.cyb.root.rms.entity.Student;
import mh.cyb.root.rms.entity.Marks;
import mh.cyb.root.rms.repository.StudentRepository;
import mh.cyb.root.rms.repository.MarksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private MarksRepository marksRepository;
    
    public List<Student> searchStudents(String query, Long sessionId) {
        if (query == null || query.trim().isEmpty()) {
            return studentRepository.findBySessionId(sessionId);
        }
        
        String searchTerm = query.trim().toLowerCase();
        
        return studentRepository.findBySessionId(sessionId).stream()
                .filter(student -> 
                    student.getName().toLowerCase().contains(searchTerm) ||
                    student.getRollNumber().toLowerCase().contains(searchTerm) ||
                    student.getClassName().toLowerCase().contains(searchTerm)
                )
                .collect(Collectors.toList());
    }
    
    public List<Student> filterByClass(String className, Long sessionId) {
        if (className == null || className.trim().isEmpty()) {
            return studentRepository.findBySessionId(sessionId);
        }
        
        return studentRepository.findBySessionId(sessionId).stream()
                .filter(student -> student.getClassName().equals(className))
                .collect(Collectors.toList());
    }
    
    public List<String> getAvailableClasses(Long sessionId) {
        return studentRepository.findBySessionId(sessionId).stream()
                .map(Student::getClassName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
