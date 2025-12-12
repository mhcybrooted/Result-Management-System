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

    // Get top performing students with pagination
    public List<Result> getTopPerformers(int page, int size) {
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
                .filter(result -> result != null && "PASS".equalsIgnoreCase(result.getResult()))
                .sorted((r1, r2) -> Double.compare(r2.getGpa(), r1.getGpa()))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    // Get total count of top performers for pagination
    public int getTopPerformersCount() {
        return (int) studentRepository.findAll().stream()
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
                .filter(result -> result != null && "PASS".equalsIgnoreCase(result.getResult()))
                .count();
    }

    // Get At-Risk Students (Fail) with pagination
    public List<Result> getAtRiskStudents(int page, int size) {
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
                .sorted((r1, r2) -> Double.compare(r1.getGpa(), r2.getGpa())) // Ascending GPA (lowest first)
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    // Get total count of At-Risk Students for pagination
    public int getAtRiskStudentsCount() {
        return (int) studentRepository.findAll().stream()
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
                .count();
    }

    // Teacher-Specific Dashboard Stats
    public Map<String, Object> getTeacherStats(Long teacherId, Long sessionId) {
        Map<String, Object> stats = new HashMap<>();

        // 1. Get Assigned Subjects
        List<mh.cyb.root.rms.entity.Subject> assignedSubjects = teacherAssignmentRepository
                .findByTeacherIdAndSessionIdAndActiveTrue(teacherId, sessionId)
                .stream()
                .map(mh.cyb.root.rms.entity.TeacherAssignment::getSubject)
                .collect(Collectors.toList());

        List<Long> subjectIds = assignedSubjects.stream().map(mh.cyb.root.rms.entity.Subject::getId)
                .collect(Collectors.toList());
        stats.put("totalSubjects", assignedSubjects.size());

        // 2. Get Marks for these subjects ONLY
        List<Marks> teacherMarks = marksRepository.findAll().stream()
                .filter(m -> subjectIds.contains(m.getSubject().getId()))
                .collect(Collectors.toList());

        // 3. Count Unique Students taught
        long studentCount = teacherMarks.stream()
                .map(m -> m.getStudent().getRollNumber())
                .distinct()
                .count();
        stats.put("totalStudents", studentCount);

        // 4. Calculate Analytics (Pass/Fail, GPA, Grade Distribution)
        int totalPass = 0;
        int totalFail = 0;
        double totalGpSum = 0.0;
        Map<String, Integer> gradeDistribution = new HashMap<>();

        for (Marks mark : teacherMarks) {
            // Calculate Percentage
            double percentage = (mark.getObtainedMarks() * 100.0) / mark.getSubject().getMaxMarks();

            // Get Grade & GP using service
            String grade = gradeCalculatorService.calculateGrade(percentage);
            double gp = gradeCalculatorService.calculateGradePoint(percentage);

            // Update Distribution
            gradeDistribution.put(grade, gradeDistribution.getOrDefault(grade, 0) + 1);

            // Update Totals
            totalGpSum += gp;

            // Pass/Fail Logic (Subject Level)
            if ("F".equals(grade)) {
                totalFail++;
            } else {
                totalPass++;
            }
        }

        stats.put("totalPass", totalPass);
        stats.put("totalFail", totalFail);
        stats.put("gradeDistribution", gradeDistribution); // Required for Chart

        // Average GPA (Average accumulated GP per mark entry)
        double averageGpa = teacherMarks.isEmpty() ? 0.0 : (totalGpSum / teacherMarks.size());
        stats.put("averageGpa", Math.round(averageGpa * 100.0) / 100.0);

        // Also put averageMarks for compatibility if needed, but UI uses averageGpa for
        // the gauge usually
        // Let's keep averageMarks as average percentage for "Average Marks" card if
        // distinct
        double totalPercentageSum = teacherMarks.stream()
                .mapToDouble(m -> (m.getObtainedMarks() * 100.0) / m.getSubject().getMaxMarks())
                .sum();
        double avgMarks = teacherMarks.isEmpty() ? 0.0 : (totalPercentageSum / teacherMarks.size());
        stats.put("averageMarks", Math.round(avgMarks * 10.0) / 10.0);

        // 5. Subject Performance (Teacher's subjects only)
        Map<String, Double> subjectPerformance = teacherMarks.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getSubject().getSubjectName(),
                        Collectors.averagingInt(Marks::getObtainedMarks)));

        Map<String, Double> formattedPerf = new HashMap<>();
        subjectPerformance.forEach((k, v) -> formattedPerf.put(k, Math.round(v * 10.0) / 10.0));
        stats.put("subjectPerformance", formattedPerf);

        return stats;
    }

    @Autowired
    private mh.cyb.root.rms.repository.TeacherAssignmentRepository teacherAssignmentRepository;

}
