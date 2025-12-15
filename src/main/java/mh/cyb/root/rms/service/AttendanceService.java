package mh.cyb.root.rms.service;

import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.repository.AttendanceRepository;
import mh.cyb.root.rms.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ExamService examService;

    // Get Daily Attendance Sheet (With existing status if marked)
    public List<AttendanceDTO> getDailySheet(String className, LocalDate date) {
        List<Student> students = studentRepository.findByActiveSession().stream()
                .filter(s -> s.getClassName().equals(className))
                .collect(Collectors.toList());

        List<Attendance> existingRecords = attendanceRepository.findByDateAndStudentIn(date, students);
        Map<Long, Attendance> attendanceMap = existingRecords.stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        List<AttendanceDTO> sheet = new ArrayList<>();
        for (Student s : students) {
            AttendanceDTO dto = new AttendanceDTO();
            dto.setStudentId(s.getId());
            dto.setStudentName(s.getName());
            dto.setRollNumber(s.getRollNumber());

            if (attendanceMap.containsKey(s.getId())) {
                Attendance a = attendanceMap.get(s.getId());
                dto.setAttendanceId(a.getId());
                dto.setStatus(a.getStatus());
                dto.setRemarks(a.getRemarks());
            } else {
                dto.setStatus(AttendanceStatus.PRESENT); // Default to Present
            }
            sheet.add(dto);
        }
        return sheet;
    }

    @Transactional
    public void saveBulkAttendance(List<AttendanceDTO> dtos, LocalDate date, Long sessionId) {
        Optional<Session> session = examService.getSessionById(sessionId);
        if (!session.isPresent())
            return;

        for (AttendanceDTO dto : dtos) {
            Optional<Student> studentOpt = studentRepository.findById(dto.getStudentId());
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();

                // Check existing
                Optional<Attendance> existing = attendanceRepository.findByStudentAndDate(student, date);
                Attendance attendance;

                if (existing.isPresent()) {
                    attendance = existing.get();
                    attendance.setStatus(dto.getStatus());
                    attendance.setRemarks(dto.getRemarks());
                } else {
                    attendance = new Attendance(student, session.get(), date, dto.getStatus());
                    attendance.setRemarks(dto.getRemarks());
                }
                attendanceRepository.save(attendance);
            }
        }
    }

    // Public Student Attendance View
    public AttendanceSummaryDTO getStudentAttendanceSummary(Long studentId, Long sessionId) {
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Optional<Session> sessionOpt = examService.getSessionById(sessionId);

        if (!studentOpt.isPresent() || !sessionOpt.isPresent()) {
            return null;
        }

        Student student = studentOpt.get();
        Session session = sessionOpt.get();

        List<Attendance> history = attendanceRepository.findByStudentAndSessionOrderByDateDesc(student, session);

        AttendanceSummaryDTO summary = new AttendanceSummaryDTO();
        summary.setStudentName(student.getName());
        summary.setRollNumber(student.getRollNumber());
        summary.setClassName(student.getClassName());
        summary.setHistory(history);

        long totalDays = history.size();
        long present = 0;
        long absent = 0;
        long late = 0;
        long excused = 0;
        long holiday = 0;

        for (Attendance a : history) {
            switch (a.getStatus()) {
                case PRESENT:
                    present++;
                    break;
                case ABSENT:
                    absent++;
                    break;
                case LATE:
                    late++;
                    break;
                case EXCUSED:
                    excused++;
                    break;
                case HOLIDAY:
                    holiday++;
                    break;
            }
        }

        summary.setTotalDays(totalDays);
        summary.setPresentCount(present);
        summary.setAbsentCount(absent);
        summary.setLateCount(late);
        summary.setExcusedCount(excused);
        summary.setHolidayCount(holiday);

        // Calculate Percentage (Present / (Total - Holiday) * 100)
        long effectiveDays = totalDays - holiday;
        double percentage = 0.0;
        if (effectiveDays > 0) {
            percentage = (double) present / effectiveDays * 100;
        }
        summary.setPercentage(Math.round(percentage * 100.0) / 100.0);

        return summary;
    }

    // DTO Helper Class
    public static class AttendanceDTO {
        private Long attendanceId;
        private Long studentId;
        private String studentName;
        private String rollNumber;
        private AttendanceStatus status;
        private String remarks;

        // Getters Setters
        public Long getAttendanceId() {
            return attendanceId;
        }

        public void setAttendanceId(Long attendanceId) {
            this.attendanceId = attendanceId;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getRollNumber() {
            return rollNumber;
        }

        public void setRollNumber(String rollNumber) {
            this.rollNumber = rollNumber;
        }

        public AttendanceStatus getStatus() {
            return status;
        }

        public void setStatus(AttendanceStatus status) {
            this.status = status;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }

    public static class AttendanceSummaryDTO {
        private String studentName;
        private String rollNumber;
        private String className;
        private long totalDays;
        private long presentCount;
        private long absentCount;
        private long lateCount;
        private long excusedCount;
        private long holidayCount;
        private double percentage;
        private List<Attendance> history;

        // Getters Setters
        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getRollNumber() {
            return rollNumber;
        }

        public void setRollNumber(String rollNumber) {
            this.rollNumber = rollNumber;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public long getTotalDays() {
            return totalDays;
        }

        public void setTotalDays(long totalDays) {
            this.totalDays = totalDays;
        }

        public long getPresentCount() {
            return presentCount;
        }

        public void setPresentCount(long presentCount) {
            this.presentCount = presentCount;
        }

        public long getAbsentCount() {
            return absentCount;
        }

        public void setAbsentCount(long absentCount) {
            this.absentCount = absentCount;
        }

        public long getLateCount() {
            return lateCount;
        }

        public void setLateCount(long lateCount) {
            this.lateCount = lateCount;
        }

        public long getExcusedCount() {
            return excusedCount;
        }

        public void setExcusedCount(long excusedCount) {
            this.excusedCount = excusedCount;
        }

        public long getHolidayCount() {
            return holidayCount;
        }

        public void setHolidayCount(long holidayCount) {
            this.holidayCount = holidayCount;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }

        public List<Attendance> getHistory() {
            return history;
        }

        public void setHistory(List<Attendance> history) {
            this.history = history;
        }
    }
}
