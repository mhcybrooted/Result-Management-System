package mh.cyb.root.rms.service;

import mh.cyb.root.rms.dto.Result;
import mh.cyb.root.rms.dto.ResultBuilder;
import mh.cyb.root.rms.entity.Marks;
import mh.cyb.root.rms.entity.Student;
import mh.cyb.root.rms.repository.MarksRepository;
import mh.cyb.root.rms.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private GradeCalculatorService gradeCalculatorService;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Student> allStudents = studentRepository.findAll();

        int totalPass = 0;
        int totalFail = 0;
        double totalGpa = 0.0;
        int studentsWithResults = 0;

        // Distribution of Grades (A+, A, etc.)
        Map<String, Integer> gradeDistribution = new HashMap<>();

        for (Student student : allStudents) {
            List<Marks> marksList = marksRepository.findByStudentRollNumber(student.getRollNumber());

            if (!marksList.isEmpty()) {
                Result result = ResultBuilder.buildResult(
                        student.getName(),
                        student.getRollNumber(),
                        student.getClassName(),
                        marksList,
                        gradeCalculatorService);

                if ("PASS".equalsIgnoreCase(result.getResult())) {
                    totalPass++;
                } else {
                    totalFail++;
                }

                totalGpa += result.getGpa();
                studentsWithResults++;

                // Count grades
                String grade = result.getGrade();
                gradeDistribution.put(grade, gradeDistribution.getOrDefault(grade, 0) + 1);
            }
        }

        double averageGpa = studentsWithResults > 0 ? (totalGpa / studentsWithResults) : 0.0;
        averageGpa = Math.round(averageGpa * 100.0) / 100.0; // Round to 2 decimals

        stats.put("totalPass", totalPass);
        stats.put("totalFail", totalFail);
        stats.put("averageGpa", averageGpa);
        stats.put("studentsWithResults", studentsWithResults);
        stats.put("gradeDistribution", gradeDistribution);

        return stats;
    }

    // Helper to get students count by class for the charts
    public Map<String, Integer> getStudentsByClass() {
        List<Student> students = studentRepository.findAll();
        return students.stream()
                .collect(Collectors.groupingBy(Student::getClassName,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    // Get average marks per subject for dashboard charts
    public Map<String, Double> getSubjectPerformance() {
        List<Marks> allMarks = marksRepository.findAll();

        // Group marks by subject name and calculate average
        Map<String, Double> averageMarksBySubject = allMarks.stream()
                .collect(Collectors.groupingBy(
                        marks -> marks.getSubject().getSubjectName(),
                        Collectors.averagingInt(Marks::getObtainedMarks)));

        // Round to 1 decimal place and return top subjects
        Map<String, Double> formattedStats = new HashMap<>();
        averageMarksBySubject.forEach((subject, avg) -> {
            formattedStats.put(subject, Math.round(avg * 10.0) / 10.0);
        });

        return formattedStats;
    }

    // Get top performing students
    public List<Result> getTopPerformers(int limit) {
        List<Student> allStudents = studentRepository.findAll();

        return allStudents.stream()
                .map(student -> {
                    List<Marks> marksList = marksRepository.findByStudentRollNumber(student.getRollNumber());
                    return marksList.isEmpty() ? null
                            : ResultBuilder.buildResult(
                                    student.getName(),
                                    student.getRollNumber(),
                                    student.getClassName(),
                                    marksList,
                                    gradeCalculatorService);
                })
                // Filter: Must be PASS (Compulsory subjects fail = Result FAIL, so this covers
                // it)
                .filter(result -> result != null && "PASS".equalsIgnoreCase(result.getResult()))
                .sorted((r1, r2) -> Double.compare(r2.getGpa(), r1.getGpa()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Get At-Risk Students (Fail)
    public List<Result> getAtRiskStudents(int limit) {
        List<Student> allStudents = studentRepository.findAll();

        return allStudents.stream()
                .map(student -> {
                    List<Marks> marksList = marksRepository.findByStudentRollNumber(student.getRollNumber());
                    return marksList.isEmpty() ? null
                            : ResultBuilder.buildResult(
                                    student.getName(),
                                    student.getRollNumber(),
                                    student.getClassName(),
                                    marksList,
                                    gradeCalculatorService);
                })
                .filter(result -> result != null && "FAIL".equalsIgnoreCase(result.getResult()))
                .sorted((r1, r2) -> Double.compare(r1.getGpa(), r2.getGpa())) // Lowest GPA first
                .limit(limit)
                .collect(Collectors.toList());
    }
}
