package mh.cyb.root.rms.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {
    private int totalRows;
    private int successCount;
    private int errorCount;
    private int skippedCount;
    private List<String> errors;
    private List<String> warnings;
    
    public ImportResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }
    
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    
    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    
    public void addError(String error) { this.errors.add(error); }
    public void addWarning(String warning) { this.warnings.add(warning); }
    
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
}
