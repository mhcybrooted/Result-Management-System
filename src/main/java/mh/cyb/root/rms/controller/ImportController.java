package mh.cyb.root.rms.controller;

import mh.cyb.root.rms.dto.ImportResult;
import mh.cyb.root.rms.dto.MarkImportRow;
import mh.cyb.root.rms.entity.Teacher;
import mh.cyb.root.rms.service.ExcelService;
import mh.cyb.root.rms.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/import")
public class ImportController {
    
    @Autowired
    private ExcelService excelService;
    
    @Autowired
    private ExamService examService;
    
    @GetMapping("/marks")
    public String importMarksForm(Model model) {
        List<Teacher> teachers = examService.getAllActiveTeachers();
        model.addAttribute("teachers", teachers);
        return "import-marks";
    }
    
    @PostMapping("/marks")
    public String importMarks(@RequestParam("file") MultipartFile file, 
                             @RequestParam(required = false) Long teacherId,
                             Model model, 
                             RedirectAttributes redirectAttributes) {
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
            return "redirect:/import/marks";
        }
        
        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            redirectAttributes.addFlashAttribute("error", "Please upload an Excel (.xlsx) file");
            return "redirect:/import/marks";
        }
        
        try {
            List<MarkImportRow> importRows = excelService.parseExcelFile(file);
            
            long validRows = importRows.stream().filter(row -> "Valid".equals(row.getStatus())).count();
            long errorRows = importRows.stream().filter(row -> "Error".equals(row.getStatus())).count();
            
            // Convert to JSON for form submission
            ObjectMapper mapper = new ObjectMapper();
            String importDataJson = mapper.writeValueAsString(importRows);
            
            model.addAttribute("importRows", importRows);
            model.addAttribute("importDataJson", importDataJson);
            model.addAttribute("totalRows", importRows.size());
            model.addAttribute("validRows", validRows);
            model.addAttribute("errorRows", errorRows);
            model.addAttribute("fileName", file.getOriginalFilename());
            model.addAttribute("teacherId", teacherId);
            model.addAttribute("teachers", examService.getAllActiveTeachers());
            
            return "import-preview";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error parsing Excel file: " + e.getMessage());
            return "redirect:/import/marks";
        }
    }
    
    @PostMapping("/marks/confirm")
    public String confirmImport(@RequestParam String importData,
                               @RequestParam(required = false) Long teacherId,
                               RedirectAttributes redirectAttributes) {
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<MarkImportRow> importRows = mapper.readValue(importData, new TypeReference<List<MarkImportRow>>() {});
            
            ImportResult result = excelService.processImport(importRows, teacherId);
            
            if (result.getSuccessCount() > 0) {
                String message = "Import completed! " + result.getSuccessCount() + " marks imported";
                if (result.getErrorCount() > 0) {
                    message += ", " + result.getErrorCount() + " errors";
                }
                if (result.getSkippedCount() > 0) {
                    message += ", " + result.getSkippedCount() + " skipped";
                }
                redirectAttributes.addFlashAttribute("success", message);
                
                if (result.hasWarnings()) {
                    redirectAttributes.addFlashAttribute("warnings", result.getWarnings());
                }
                if (result.hasErrors()) {
                    redirectAttributes.addFlashAttribute("importErrors", result.getErrors());
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "No marks were imported. " + 
                    String.join(", ", result.getErrors()));
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing import: " + e.getMessage());
        }
        
        return "redirect:/import/marks";
    }
    
    @GetMapping("/export")
    public String exportPage(Model model) {
        List<String> availableClasses = examService.getAllStudents().stream()
                .map(mh.cyb.root.rms.entity.Student::getClassName)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("availableClasses", availableClasses);
        model.addAttribute("exams", examService.getAllActiveExams());
        
        Optional<mh.cyb.root.rms.entity.Session> activeSession = examService.getActiveSession();
        if (activeSession.isPresent()) {
            model.addAttribute("activeSession", activeSession.get());
        }
        
        return "export";
    }
}
