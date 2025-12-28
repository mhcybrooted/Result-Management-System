package mh.cyb.root.rms.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        // Log the error (optional, but good practice)
        e.printStackTrace();

        // Add error details to model
        model.addAttribute("error", "An unexpected error occurred: " + e.getMessage());
        model.addAttribute("message", e.getMessage());

        // Return the error view
        return "error";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e, Model model) {
        e.printStackTrace();
        model.addAttribute("error", e.getMessage());
        model.addAttribute("message", e.getMessage()); // For specific messages like "Cannot delete yourself"
        return "error";
    }
}
