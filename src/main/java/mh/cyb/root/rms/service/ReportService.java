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

        // 5. Calculate Overall Grade from Final GPA (BD Standard)
        // Previous logic used overallPercentage, which is incorrect for this system
        String overallGrade = gradeCalculatorService.calculateOverallGradeFromGPA(gpa);
        if (isFail) {
            overallGrade = "F"; // Enforce F if failed
        }
        reportCard.setOverallGrade(overallGrade);

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

    public List<mh.cyb.root.rms.dto.MeritListItem> generateMeritList(String className, Long sessionId) {
        // 1. Get raw report cards for everyone in the class
        List<ReportCardData> classReports = generateClassReports(className, sessionId);

        // 2. Sort Logic
        // Primary: FAIL goes to bottom (or just treat GPA 0.00 as sort key)
        // Secondary: GPA Descending
        // Tertiary: Total Marks Descending
        classReports.sort((r1, r2) -> {
            // Priority 1: Pass/Fail Status (PASS > FAIL)
            boolean r1Pass = "PASS".equalsIgnoreCase(r1.getResult());
            boolean r2Pass = "PASS".equalsIgnoreCase(r2.getResult());

            if (r1Pass && !r2Pass)
                return -1;
            if (!r1Pass && r2Pass)
                return 1;

            // Priority 2: GPA (Higher is better)
            int gpaCompare = Double.compare(r2.getGpa(), r1.getGpa());
            if (gpaCompare != 0)
                return gpaCompare;

            // Priority 3: Total Obtained Marks (Higher is better)
            double r1Total = r1.getSubjects().stream().mapToDouble(SubjectReport::getTotalObtained).sum();
            double r2Total = r2.getSubjects().stream().mapToDouble(SubjectReport::getTotalObtained).sum();
            return Double.compare(r2Total, r1Total);
        });

        // 3. Map to DTO with Rank
        List<mh.cyb.root.rms.dto.MeritListItem> meritList = new ArrayList<>();
        int rank = 1;
        for (ReportCardData report : classReports) {
            double totalObtained = report.getSubjects().stream().mapToDouble(SubjectReport::getTotalObtained)
                    .sum();
            double totalMax = report.getSubjects().stream().mapToDouble(SubjectReport::getTotalMaximum).sum();

            // Assign rank only if passed (optional preference, but usually Merit List
            // includes
            // everyone with rank or just passed?
            // "Merit" usually means passed. Let's rank EVERYONE based on the sort order,
            // but
            // maybe visual distinction.
            // Requirement didn't specify, so I will rank everyone.

            mh.cyb.root.rms.dto.MeritListItem item = new mh.cyb.root.rms.dto.MeritListItem(
                    rank++,
                    report.getStudent(),
                    report.getGpa(),
                    totalObtained,
                    totalMax,
                    report.getOverallGrade(),
                    report.getResult());
            meritList.add(item);
        }

        return meritList;
    }

    public byte[] generateMeritListPDF(List<mh.cyb.root.rms.dto.MeritListItem> meritList, String className,
            Session session) {
        try {
            Context context = new Context();
            context.setVariable("meritList", meritList);
            context.setVariable("className", className);
            context.setVariable("sessionName", session.getSessionName());
            context.setVariable("reportDate", java.time.LocalDate.now());
            context.setVariable("schoolName", schoolName);

            String html = templateEngine.process("merit-list-pdf", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConverterProperties properties = new ConverterProperties();
            HtmlConverter.convertToPdf(html, outputStream, properties);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Merit List PDF", e);
        }
    }

    public byte[] generateMeritListCSV(List<mh.cyb.root.rms.dto.MeritListItem> meritList) {
        StringBuilder csv = new StringBuilder();
        // Header
        csv.append("Rank,Roll Number,Student Name,Class,Total Obtained,Total Max,GPA,Grade,Result\n");

        for (mh.cyb.root.rms.dto.MeritListItem item : meritList) {
            csv.append(item.getRank()).append(",");
            csv.append(escapeSpecialCharacters(item.getStudent().getRollNumber())).append(",");
            csv.append(escapeSpecialCharacters(item.getStudent().getName())).append(",");
            csv.append(escapeSpecialCharacters(item.getStudent().getClassName())).append(",");
            csv.append(item.getTotalObtained()).append(",");
            csv.append(item.getTotalMax()).append(",");
            csv.append(item.getGpa()).append(",");
            csv.append(item.getOverallGrade()).append(",");
            csv.append(item.getResult()).append("\n");
        }

        return csv.toString().getBytes();
    }

    private String escapeSpecialCharacters(String data) {
        if (data == null) {
            return "";
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}
