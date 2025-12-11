package mh.cyb.root.rms.service;

import mh.cyb.root.rms.dto.ReportCardData;
import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.entity.Class;
import mh.cyb.root.rms.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
public class FailLogicFinalTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private ExamRepository examRepository;
    @Autowired
    private MarksRepository marksRepository;
    @Autowired
    private ClassRepository classRepository;

    @Test
    public void verifyFailLogicAndGeneratePDF() throws Exception {
        // --- Setup Shared Data ---
        Session session = new Session("FAIL-VERIFY-SESSION", LocalDate.now(), LocalDate.now().plusMonths(1));
        sessionRepository.save(session);

        Class cls = new Class("VerifyClass", "Description");
        classRepository.save(cls);

        Exam exam = new Exam("Final Exam", LocalDate.now(), session);
        examRepository.save(exam);

        // --- Scenario 1: Compulsory Fail ---
        Student studentFail = new Student("Compulsory Fail Student", "CF001", "VerifyClass", session);
        studentRepository.save(studentFail);

        Subject subFail1 = new Subject("Mathematics", cls, 100);
        subFail1.setOptional(false);
        subjectRepository.save(subFail1);

        Subject subFail2 = new Subject("English", cls, 100);
        subFail2.setOptional(false);
        subjectRepository.save(subFail2);

        // Marks: 25/100 (F) in Math, 80/100 (A+) in English
        marksRepository.save(new Marks(studentFail, subFail1, exam, 25, LocalDate.now()));
        marksRepository.save(new Marks(studentFail, subFail2, exam, 80, LocalDate.now()));

        Optional<ReportCardData> reportFailOpt = reportService.generateReportCard(studentFail.getId(), session.getId());
        ReportCardData reportFail = reportFailOpt
                .orElseThrow(() -> new RuntimeException("Report not generated for Fail Student"));

        System.out.println("Scenario 1 (Compulsory F) Result: " + reportFail.getResult());
        System.out.println("Scenario 1 (Compulsory F) GPA: " + reportFail.getGpa());

        assertEquals("FAIL", reportFail.getResult(), "Should FAIL if compulsory subject is F");
        assertEquals(0.00, reportFail.getGpa(), 0.01, "GPA should be 0.00 if FAIL");

        // --- Scenario 2: Optional Fail ---
        Student studentOptFail = new Student("Optional Fail Student", "OF001", "VerifyClass", session);
        studentRepository.save(studentOptFail);

        Subject subComp = new Subject("Science", cls, 100);
        subComp.setOptional(false);
        subjectRepository.save(subComp);

        Subject subOpt = new Subject("Advanced Art", cls, 100);
        subOpt.setOptional(true);
        subjectRepository.save(subOpt);

        // Marks: 80/100 (A+) in Science, 25/100 (F) in Optional Art
        marksRepository.save(new Marks(studentOptFail, subComp, exam, 80, LocalDate.now()));
        marksRepository.save(new Marks(studentOptFail, subOpt, exam, 25, LocalDate.now()));

        Optional<ReportCardData> reportOptFailOpt = reportService.generateReportCard(studentOptFail.getId(),
                session.getId());
        ReportCardData reportOptFail = reportOptFailOpt
                .orElseThrow(() -> new RuntimeException("Report not generated for Opt Fail Student"));

        System.out.println("Scenario 2 (Optional F) Result: " + reportOptFail.getResult());
        System.out.println("Scenario 2 (Optional F) GPA: " + reportOptFail.getGpa());

        assertEquals("PASS", reportOptFail.getResult(), "Should PASS if only optional subject is F");
        // GPA calc: 5.0 (Science) + 0 (Art, ignored) = 5.0 / 1 = 5.0
        assertEquals(5.00, reportOptFail.getGpa(), 0.01, "GPA should be calculated normally ignoring optional F");

        // --- Generate PDFs ---
        generatePDF(reportFail, "compulsory_fail_verify.pdf");
        generatePDF(reportOptFail, "optional_fail_verify.pdf");
    }

    private void generatePDF(ReportCardData data, String filename) {
        try {
            byte[] pdfBytes = reportService.generatePDF(data);
            Path path = Paths.get("C:\\Users\\HP\\.gemini\\antigravity\\brain\\1c9aaa6b-500b-4a8c-865f-7d65dbf64fea",
                    filename);
            Files.write(path, pdfBytes);
            System.out.println("Generated PDF: " + path.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("PDF Generation Failed", e);
        }
    }
}
