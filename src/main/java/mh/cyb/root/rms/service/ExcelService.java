package mh.cyb.root.rms.service;

import mh.cyb.root.rms.dto.ImportResult;
import mh.cyb.root.rms.dto.MarkImportRow;
import mh.cyb.root.rms.entity.*;
import mh.cyb.root.rms.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExcelService {
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private MarksRepository marksRepository;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    public List<MarkImportRow> parseExcelFile(MultipartFile file) throws IOException {
        List<MarkImportRow> rows = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                MarkImportRow importRow = new MarkImportRow();
                
                try {
                    // Expected columns: StudentRoll, StudentName, SubjectName, ExamName, ObtainedMarks
                    importRow.setStudentRoll(getCellValueAsString(row.getCell(0)));
                    importRow.setStudentName(getCellValueAsString(row.getCell(1)));
                    importRow.setSubjectName(getCellValueAsString(row.getCell(2)));
                    importRow.setExamName(getCellValueAsString(row.getCell(3)));
                    
                    Cell marksCell = row.getCell(4);
                    if (marksCell != null) {
                        if (marksCell.getCellType() == CellType.NUMERIC) {
                            importRow.setObtainedMarks((int) marksCell.getNumericCellValue());
                        } else {
                            String marksStr = getCellValueAsString(marksCell);
                            if (marksStr != null && !marksStr.trim().isEmpty()) {
                                importRow.setObtainedMarks(Integer.parseInt(marksStr.trim()));
                            }
                        }
                    }
                    
                    // Basic validation
                    if (importRow.getStudentRoll() == null || importRow.getStudentRoll().trim().isEmpty()) {
                        importRow.setStatus("Error");
                        importRow.setErrorMessage("Student roll number is required");
                    } else if (importRow.getObtainedMarks() == null || importRow.getObtainedMarks() < 0) {
                        importRow.setStatus("Error");
                        importRow.setErrorMessage("Valid marks are required");
                    }
                    
                } catch (Exception e) {
                    importRow.setStatus("Error");
                    importRow.setErrorMessage("Error parsing row: " + e.getMessage());
                }
                
                rows.add(importRow);
            }
        }
        
        return rows;
    }
    
    @Transactional
    public ImportResult processImport(List<MarkImportRow> rows, Long teacherId) {
        ImportResult result = new ImportResult();
        result.setTotalRows(rows.size());
        
        Optional<Session> activeSession = sessionRepository.findByActiveTrue();
        if (!activeSession.isPresent()) {
            result.addError("No active session found");
            return result;
        }
        
        int success = 0;
        int errors = 0;
        int skipped = 0;
        
        for (MarkImportRow row : rows) {
            if ("Error".equals(row.getStatus())) {
                skipped++;
                continue;
            }
            
            try {
                if (processImportRow(row, teacherId, activeSession.get(), result)) {
                    success++;
                } else {
                    errors++;
                }
            } catch (Exception e) {
                errors++;
                result.addError("Error processing row for " + row.getStudentRoll() + ": " + e.getMessage());
            }
        }
        
        result.setSuccessCount(success);
        result.setErrorCount(errors);
        result.setSkippedCount(skipped);
        
        return result;
    }
    
    private boolean processImportRow(MarkImportRow row, Long teacherId, Session session, ImportResult result) {
        // Find student by roll number in active session
        Optional<Student> student = studentRepository.findAll().stream()
                .filter(s -> s.getRollNumber().equals(row.getStudentRoll()) && 
                           s.getSession().getId().equals(session.getId()))
                .findFirst();
        
        if (!student.isPresent()) {
            result.addError("Student not found: " + row.getStudentRoll());
            return false;
        }
        
        // Find subject by name
        Optional<Subject> subject = subjectRepository.findAll().stream()
                .filter(s -> s.getSubjectName().equalsIgnoreCase(row.getSubjectName()))
                .findFirst();
        
        if (!subject.isPresent()) {
            result.addError("Subject not found: " + row.getSubjectName());
            return false;
        }
        
        // Find exam by name in active session
        Optional<Exam> exam = examRepository.findAll().stream()
                .filter(e -> e.getExamName().equalsIgnoreCase(row.getExamName()) && 
                           e.getSession().getId().equals(session.getId()))
                .findFirst();
        
        if (!exam.isPresent()) {
            result.addError("Exam not found: " + row.getExamName());
            return false;
        }
        
        // Validate marks don't exceed max marks
        if (row.getObtainedMarks() > subject.get().getMaxMarks()) {
            result.addError("Marks for " + row.getStudentRoll() + 
                           " exceed maximum (" + subject.get().getMaxMarks() + ")");
            return false;
        }
        
        // Check for existing marks
        Optional<Marks> existingMarks = marksRepository.findByStudentIdAndSubjectIdAndExamId(
                student.get().getId(), subject.get().getId(), exam.get().getId());
        
        Marks marks;
        if (existingMarks.isPresent()) {
            // Update existing marks
            marks = existingMarks.get();
            marks.setObtainedMarks(row.getObtainedMarks());
            marks.setEnteredDate(LocalDateTime.now());
            result.addWarning("Updated existing marks for " + row.getStudentRoll());
        } else {
            // Create new marks
            marks = new Marks(student.get(), subject.get(), exam.get(), 
                            row.getObtainedMarks(), LocalDate.now());
        }
        
        // Set teacher if provided
        if (teacherId != null) {
            marks.setEnteredBy(new Teacher());
            marks.getEnteredBy().setId(teacherId);
        }
        
        marksRepository.save(marks);
        return true;
    }
    
    public byte[] exportMarksToExcel(String className, Long examId, Long sessionId) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Marks Export");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Student Roll");
            headerRow.createCell(1).setCellValue("Student Name");
            headerRow.createCell(2).setCellValue("Class");
            headerRow.createCell(3).setCellValue("Subject");
            headerRow.createCell(4).setCellValue("Exam");
            headerRow.createCell(5).setCellValue("Obtained Marks");
            headerRow.createCell(6).setCellValue("Max Marks");
            headerRow.createCell(7).setCellValue("Percentage");
            headerRow.createCell(8).setCellValue("Entered By");
            headerRow.createCell(9).setCellValue("Date");
            
            // Get marks data
            List<Marks> marksList = marksRepository.findAll().stream()
                    .filter(m -> sessionId == null || m.getStudent().getSession().getId().equals(sessionId))
                    .filter(m -> className == null || m.getStudent().getClassName().equals(className))
                    .filter(m -> examId == null || m.getExam().getId().equals(examId))
                    .collect(java.util.stream.Collectors.toList());
            
            // Fill data rows
            int rowNum = 1;
            for (Marks marks : marksList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(marks.getStudent().getRollNumber());
                row.createCell(1).setCellValue(marks.getStudent().getName());
                row.createCell(2).setCellValue(marks.getStudent().getClassName());
                row.createCell(3).setCellValue(marks.getSubject().getSubjectName());
                row.createCell(4).setCellValue(marks.getExam().getExamName());
                row.createCell(5).setCellValue(marks.getObtainedMarks());
                row.createCell(6).setCellValue(marks.getSubject().getMaxMarks());
                
                double percentage = (marks.getObtainedMarks() * 100.0) / marks.getSubject().getMaxMarks();
                row.createCell(7).setCellValue(String.format("%.2f%%", percentage));
                
                row.createCell(8).setCellValue(marks.getEnteredBy() != null ? marks.getEnteredBy().getName() : "Unknown");
                row.createCell(9).setCellValue(marks.getExamDate() != null ? marks.getExamDate().toString() : "");
            }
            
            // Auto-size columns
            for (int i = 0; i < 10; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convert to byte array
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }
}
