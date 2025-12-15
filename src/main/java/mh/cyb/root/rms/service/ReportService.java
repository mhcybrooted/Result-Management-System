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

    @Autowired
    private GradeCalculatorService gradeCalculatorService;

    public Optional<ReportCardData> generateReportCard(Long studentId, Long sessionId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);

        if (!studentOpt.isPresent() || !sessionOpt.isPresent()) {
            return Optional.empty();
        }

        Student student = studentOpt.get();
        Session session = sessionOpt.get();

        // Get all marks for this student in this session (Optimized Query)
        List<Marks> allMarks = marksRepository.findByStudentIdAndSessionId(studentId, sessionId);

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
            subjectReport.setGrade(gradeCalculatorService.calculateGrade(perc));
            subjectReport.setGradePoint(gradeCalculatorService.calculateGradePoint(perc));

            // Set optional flag
            if (!subjectMarks.isEmpty()) {
                subjectReport.setOptional(subjectMarks.get(0).getSubject().isOptional());
            }

            subjectReports.add(subjectReport);
        }

        ReportCardData reportCard = new ReportCardData(student, session, subjectReports);

        // --- Centralized Calculation using GradeCalculatorService ---

        // 1. Calculate Totals
        double totalObtained = subjectReports.stream().mapToDouble(SubjectReport::getTotalObtained).sum();
        double totalMaximum = subjectReports.stream().mapToDouble(SubjectReport::getTotalMaximum).sum();
        double overallPercentage = totalMaximum > 0 ? (totalObtained / totalMaximum) * 100 : 0;

        reportCard.setOverallPercentage(Math.round(overallPercentage * 100.0) / 100.0); // Round for display

        // 2. Calculate Overall Grade
        String overallGrade = gradeCalculatorService.calculateGrade(overallPercentage);
        reportCard.setOverallGrade(overallGrade);

        // 3. GPA Calculation with Optional Logic
        double totalGP = 0.0;
        int compulsoryCount = 0;
        boolean isFail = false;
        double optionalThreshold = gradeCalculatorService.getOptionalMinThreshold();

        for (SubjectReport sr : subjectReports) {
            String grade = sr.getGrade();

            if (sr.isOptional()) {
                // Optional Logic: If GP >= Threshold, add (GP - Threshold)
                if (sr.getGradePoint() >= optionalThreshold) {
                    totalGP += (sr.getGradePoint() - optionalThreshold);
                }
            } else {
                // Compulsory Logic: Add full GP
                totalGP += sr.getGradePoint();
                compulsoryCount++;

                // CHECK FAIL CONDITION
                if (!gradeCalculatorService.isPass(grade)) {
                    isFail = true;
                }
            }
        }

        // 4. GPA Finalization
        double gpa;
        if (compulsoryCount > 0) {
            gpa = totalGP / compulsoryCount;
        } else {
            gpa = 0.0;
        }

        // Cap at Max
        if (gpa > gradeCalculatorService.getGpaMaxCap()) {
            gpa = gradeCalculatorService.getGpaMaxCap();
        }

        // Apply Fail Logic
        if (isFail) {
            gpa = 0.00;
        }
        reportCard.setGpa(Math.round(gpa * 100.0) / 100.0);

        // 5. Set Result Logic (PASS/FAIL)
        // If fail flag is set, it's FAIL.
        // OR if overall percentage is below passing (Grade D min)
        if (isFail || !gradeCalculatorService.isPass(overallPercentage)) {
            reportCard.setResult(gradeCalculatorService.getFailStatus());
        } else {
            reportCard.setResult(gradeCalculatorService.getPassStatus());
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

    public byte[] generatePDF(ReportCardData reportData) {
        try {
            Context context = new Context();
            context.setVariable("reportCard", reportData);
            context.setVariable("schoolName", schoolName);
            context.setVariable("schoolAddress", schoolAddress);
            context.setVariable("reportTitle", reportTitle);

            // Create grade scale legend (Currently using hardcoded properties for Legend to
            // stay simple or should fetch from Service?
            // The logic was moved to Service, but the PDF legend values were hardcoded
            // props here.
            // For now, I will keep the legend generation as is, since
            // GradeCalculatorService doesn't expose the limits easily without getters.
            // Wait, I can't access private fields here anymore. I'll rely on
            // GradeCalculatorService if it has getters, otherwise I might break the legend.
            // GradeCalculatorService DOES NOT have getters for minAPlus etc.
            // I should update GradeCalculatorService to expose them or just keep the
            // properties here for the legend.
            // Since "fix all item" implies keeping it working, I will keep the properties
            // here for the Legend but use the Service for calculation.)
            // Actually, I can leave the properties here for the PDF legend ONLY.

            // Wait, I am replacing the WHOLE local calculation logic.
            // `minAPlus` etc are used in `generatePDF` for the Legend.
            // I will keep the @Values, but remove `calculateGrade` and
            // `calculateGradePoint`.

            Map<String, String> gradeLegend = new LinkedHashMap<>();
            // Re-using the @Value fields which I am keeping below
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

    // PDF Legend Properties (Kept for Legend display)
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

    public List<ReportCardData> generateClassReports(String className, Long sessionId) {
        List<Student> classStudents = studentRepository.findByActiveSession().stream() // Optimizable too, but one step
                                                                                       // at a time
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
