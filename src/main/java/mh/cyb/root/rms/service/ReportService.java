package mh.cyb.root.rms.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import mh.cyb.root.rms.dto.ReportCardData;
import mh.cyb.root.rms.dto.SubjectReport;
import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private MarksRepository marksRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TemplateEngine templateEngine;

    public Optional<ReportCardData> generateReportCard(Long studentId, Long sessionId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (!studentOpt.isPresent() || !sessionOpt.isPresent()) {
            return Optional.empty();
        }

        Student student = studentOpt.get();
        Session session = sessionOpt.get();

        // Get all marks for this student in this session
        List<Marks> allMarks = marksRepository.findAll().stream()
                .filter(m -> {
                    boolean match = m.getStudent().getId().equals(studentId) &&
                            m.getStudent().getSession().getId().equals(sessionId);
                    if (!match && m.getStudent().getId().equals(studentId)) {
                        System.out.println("DEBUG: No match. Mark Session: " + m.getStudent().getSession().getId()
                                + " vs Req: " + sessionId);
                    }
                    return match;
                })
                .collect(Collectors.toList());

        if (allMarks.isEmpty()) {
            return Optional.empty();
        }

        // Group marks by subject
        Map<String, List<Marks>> marksBySubject = allMarks.stream()
                .collect(Collectors.groupingBy(m -> m.getSubject().getSubjectName()));

        List<SubjectReport> subjectReports = new ArrayList<>();

        for (Map.Entry<String, List<Marks>> entry : marksBySubject.entrySet()) {
            String subjectName = entry.getKey();
            List<Marks> subjectMarks = entry.getValue();

            // Create exam marks map
            Map<String, Integer> examMarks = new HashMap<>();
            int totalMaximum = 0;

            for (Marks mark : subjectMarks) {
                examMarks.put(mark.getExam().getExamName(), mark.getObtainedMarks());
                totalMaximum += mark.getSubject().getMaxMarks();
            }

            SubjectReport subjectReport = new SubjectReport(subjectName, examMarks, totalMaximum);
            double perc = subjectReport.getPercentage();
            subjectReport.setGrade(calculateGrade(perc));
            subjectReport.setGradePoint(calculateGradePoint(perc));

            // Set optional flag
            if (!subjectMarks.isEmpty()) {
                subjectReport.setOptional(subjectMarks.get(0).getSubject().isOptional());
            }

            subjectReports.add(subjectReport);
        }

        ReportCardData reportCard = new ReportCardData(student, session, subjectReports);

        // Calculate Overall Grade
        reportCard.setOverallGrade(calculateGrade(reportCard.getOverallPercentage()));

        // --- GPA Calculation with Optional Subject Logic ---
        double totalGP = 0.0;
        int compulsoryCount = 0;
        boolean isFail = false;

        for (SubjectReport sr : subjectReports) {
            String grade = sr.getGrade();

            if (sr.isOptional()) {
                // Optional Logic: If GP >= 2.0, add (GP - 2.0)
                if (sr.getGradePoint() >= 2.0) {
                    totalGP += (sr.getGradePoint() - 2.0);
                }
            } else {
                // Compulsory Logic: Add full GP
                totalGP += sr.getGradePoint();
                compulsoryCount++;

                // CHECK FAIL CONDITION
                if ("F".equals(grade)) {
                    isFail = true;
                }
            }
        }

        double gpa;
        if (compulsoryCount > 0) {
            gpa = totalGP / compulsoryCount;
        } else {
            gpa = 0.0;
        }

        // Cap at 5.00
        if (gpa > 5.00) {
            gpa = 5.00;
        }

        // Apply Fail Logic
        if (isFail) {
            gpa = 0.00;
        }

        reportCard.setGpa(Math.round(gpa * 100.0) / 100.0);

        if (isFail) {
            reportCard.setResult("FAIL");
        }
        // ----------------------------------------------------

        return Optional.of(reportCard);
    }

    @org.springframework.beans.factory.annotation.Value("${school.name}")
    private String schoolName;

    @org.springframework.beans.factory.annotation.Value("${school.address}")
    private String schoolAddress;

    @org.springframework.beans.factory.annotation.Value("${report.title}")
    private String reportTitle;

    @org.springframework.beans.factory.annotation.Value("${grade.aplus.min}")
    private int minAPlus;
    @org.springframework.beans.factory.annotation.Value("${grade.a.min}")
    private int minA;
    @org.springframework.beans.factory.annotation.Value("${grade.b.min}")
    private int minB;
    @org.springframework.beans.factory.annotation.Value("${grade.c.min}")
    private int minC;
    @org.springframework.beans.factory.annotation.Value("${grade.d.min}")
    private int minD;
    @org.springframework.beans.factory.annotation.Value("${grade.aminus.min}")
    private int minAMinus;

    @org.springframework.beans.factory.annotation.Value("${grade.aplus.point}")
    private double pointAPlus;
    @org.springframework.beans.factory.annotation.Value("${grade.a.point}")
    private double pointA;
    @org.springframework.beans.factory.annotation.Value("${grade.aminus.point}")
    private double pointAMinus;
    @org.springframework.beans.factory.annotation.Value("${grade.b.point}")
    private double pointB;
    @org.springframework.beans.factory.annotation.Value("${grade.c.point}")
    private double pointC;
    @org.springframework.beans.factory.annotation.Value("${grade.d.point}")
    private double pointD;

    public byte[] generatePDF(ReportCardData reportData) {
        try {
            Context context = new Context();
            context.setVariable("reportCard", reportData);
            context.setVariable("schoolName", schoolName);
            context.setVariable("schoolAddress", schoolAddress);
            context.setVariable("reportTitle", reportTitle);

            // Create grade scale legend
            Map<String, String> gradeLegend = new LinkedHashMap<>();
            gradeLegend.put("A+", minAPlus + "%+ (GP " + pointAPlus + ")");
            gradeLegend.put("A", minA + "-" + (minAPlus - 1) + "% (GP " + pointA + ")");
            gradeLegend.put("A-", minAMinus + "-" + (minA - 1) + "% (GP " + pointAMinus + ")");
            gradeLegend.put("B", minB + "-" + (minAMinus - 1) + "% (GP " + pointB + ")");
            gradeLegend.put("C", minC + "-" + (minB - 1) + "% (GP " + pointC + ")");
            gradeLegend.put("D", minD + "-" + (minC - 1) + "% (GP " + pointD + ")");
            gradeLegend.put("F", "0%-" + (minD - 1) + "% (GP 0.00)");
            context.setVariable("gradeLegend", gradeLegend);

            String html = templateEngine.process("report-card-pdf", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConverterProperties properties = new ConverterProperties();
            HtmlConverter.convertToPdf(html, outputStream, properties);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private String calculateGrade(double percentage) {
        if (percentage >= minAPlus)
            return "A+";
        if (percentage >= minA)
            return "A";
        if (percentage >= minAMinus)
            return "A-";
        if (percentage >= minB)
            return "B";
        if (percentage >= minC)
            return "C";
        if (percentage >= minD)
            return "D";
        return "F";
    }

    private double calculateGradePoint(double percentage) {
        if (percentage >= minAPlus)
            return pointAPlus;
        if (percentage >= minA)
            return pointA;
        if (percentage >= minAMinus)
            return pointAMinus;
        if (percentage >= minB)
            return pointB;
        if (percentage >= minC)
            return pointC;
        if (percentage >= minD)
            return pointD;
        return 0.00;
    }

    public List<ReportCardData> generateClassReports(String className, Long sessionId) {
        List<Student> classStudents = studentRepository.findAll().stream()
                .filter(s -> s.getClassName().equals(className) &&
                        s.getSession().getId().equals(sessionId))
                .collect(Collectors.toList());

        List<ReportCardData> reports = new ArrayList<>();

        for (Student student : classStudents) {
            Optional<ReportCardData> report = generateReportCard(student.getId(), sessionId);
            report.ifPresent(reports::add);
        }

        return reports;
    }
}
