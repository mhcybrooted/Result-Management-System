package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.entity.Session;
import mh.cyb.root.rms.service.ExcelService;
import mh.cyb.root.rms.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
@RequestMapping("/export")
public class ExportController {
    
    @Autowired
    private ExcelService excelService;
    
    @Autowired
    private ExamService examService;
    
    @GetMapping("/marks")
    public ResponseEntity<byte[]> exportMarks(
            @RequestParam(required = false) String className,
            @RequestParam(required = false) Long examId) {
        
        try {
            Optional<Session> activeSession = examService.getActiveSession();
            Long sessionId = activeSession.isPresent() ? activeSession.get().getId() : null;
            
            byte[] excelData = excelService.exportMarksToExcel(className, examId, sessionId);
            
            // Generate filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "marks_export_" + timestamp + ".xlsx";
            
            if (className != null) {
                filename = className.replace(" ", "_") + "_marks_" + timestamp + ".xlsx";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
