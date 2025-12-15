package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.dto.Result;
import mh.cyb.root.rms.dto.ResultBuilder;
import mh.cyb.root.rms.entity.Marks;
import mh.cyb.root.rms.entity.Student;
import mh.cyb.root.rms.repository.MarksRepository;
import mh.cyb.root.rms.repository.StudentRepository;
import mh.cyb.root.rms.service.GradeCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class DebugController {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private MarksRepository marksRepository;
    @Autowired
    private GradeCalculatorService gradeService;

    @GetMapping("/debug/{rollNumber}")
    public String debugStudent(@PathVariable String rollNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>Debug Report for Roll: ").append(rollNumber).append("</h1>");

        // Find ALL students with this roll number, not just the first one
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> s.getRollNumber().equals(rollNumber))
                .collect(Collectors.toList());

        if (students.isEmpty()) {
            return "Student not found with roll number: " + rollNumber;
        }

        sb.append("Found <b>").append(students.size()).append("</b> student(s) with this roll number.<hr>");

        for (Student s : students) {
            sb.append("<div style='background:#f0f0f0; margin:10px; padding:10px; border:1px solid #ccc'>");
            sb.append("<h2>Student ID: ").append(s.getId()).append("</h2>");
            sb.append("Name: <b>").append(s.getName()).append("</b><br>");
            sb.append("Class: ").append(s.getClassName()).append("<br>");
            sb.append("Session: ").append(s.getSession().getSessionName()).append(" (Active: ")
                    .append(s.getSession().getActive()).append(")<br>");
            sb.append("Student Active: ").append(s.getActive()).append("<br>");

            // RAW MARKS QUERY (Fixed Logic: ID + Session)
            List<Marks> marks = marksRepository.findByStudentIdAndSessionId(s.getId(), s.getSession().getId());

            // SUPER RAW CHECK (Bypass Repository Filter)
            List<Marks> allRawMarks = marksRepository.findAll().stream()
                    .filter(m -> m.getStudent().getId().equals(s.getId())
                            && m.getStudent().getSession().getId().equals(s.getSession().getId()))
                    .collect(Collectors.toList());

            sb.append("<h3>DB State Analysis</h3>");
            sb.append("Total Raw Marks in DB (Active+Inactive): <b>").append(allRawMarks.size()).append("</b><br>");
            sb.append("Filtered Marks via Repository (Active Only): <b>").append(marks.size()).append("</b><br>");

            if (allRawMarks.size() > marks.size()) {
                sb.append(
                        "<div style='color:red'>Found Inactive Exam Marks! Verify they are not in the table below.</div>");
            }

            sb.append("<h3>Marks Found (Count: ").append(marks.size()).append(")</h3>");
            if (!marks.isEmpty()) {
                sb.append(
                        "<table border='1'><tr><th>Subject</th><th>Exam</th><th>Exam Active?</th><th>Obtained</th><th>Max</th><th>%</th><th>Grade</th><th>Optional?</th></tr>");
                for (Marks m : marks) {
                    double p = (m.getObtainedMarks() * 100.0) / m.getSubject().getMaxMarks();
                    String g = gradeService.calculateGrade(p);

                    sb.append("<tr>");
                    sb.append("<td>").append(m.getSubject().getSubjectName()).append("</td>");
                    sb.append("<td>").append(m.getExam().getExamName()).append("</td>");
                    sb.append("<td>").append(m.getExam().getActive()).append("</td>");
                    sb.append("<td>").append(m.getObtainedMarks()).append("</td>");
                    sb.append("<td>").append(m.getSubject().getMaxMarks()).append("</td>");
                    sb.append("<td>").append(String.format("%.2f", p)).append("%</td>");
                    sb.append("<td>").append(g).append("</td>");
                    sb.append("<td>").append(m.getSubject().isOptional()).append("</td>");
                    sb.append("</tr>");
                }
                sb.append("</table>");

                // RESULT BUILDER CALCULATION
                Result r = ResultBuilder.buildResult(s.getName(), s.getRollNumber(), s.getClassName(), marks,
                        gradeService);
                sb.append("<h3>Calculated Result</h3>");
                sb.append("Total: ").append(r.getTotalObtained()).append("/").append(r.getTotalMax()).append("<br>");
                sb.append("Percentage: ").append(r.getPercentage()).append("%<br>");
                sb.append("Overall Grade: ").append(r.getGrade()).append("<br>");
                sb.append("Result Status: <b>").append(r.getResult()).append("</b><br>");
            } else {
                sb.append("No marks found for this specific student ID.<br>");
            }
            sb.append("</div>");
        }

        return sb.toString();
    }
}
