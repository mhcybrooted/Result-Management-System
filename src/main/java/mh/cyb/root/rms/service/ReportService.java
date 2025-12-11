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
            subjectReports.add(subjectReport);
        }

        ReportCardData reportCard = new ReportCardData(student, session, subjectReports);
        return Optional.of(reportCard);
    }

    public byte[] generatePDF(ReportCardData reportData) {
        try {
            Context context = new Context();
            context.setVariable("reportCard", reportData);

            String html = templateEngine.process("report-card-pdf", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ConverterProperties properties = new ConverterProperties();

            HtmlConverter.convertToPdf(html, outputStream, properties);

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
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
