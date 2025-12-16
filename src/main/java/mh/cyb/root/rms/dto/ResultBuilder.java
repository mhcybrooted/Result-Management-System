package mh.cyb.root.rms.dto;

import mh.cyb.root.rms.entity.Marks;
import mh.cyb.root.rms.service.GradeCalculatorService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class to build Result DTOs with grade calculations
 */
public class ResultBuilder {

    /**
     * Build a Result with grade calculations from GradeCalculatorService
     */
    public static Result buildResult(String studentName, String rollNumber, String className,
            List<Marks> marksList, GradeCalculatorService gradeCalc) {
        Result result = new Result();
        result.setStudentName(studentName);
        result.setRollNumber(rollNumber);
        result.setClassName(className);
        result.setMarksList(marksList);

        // Calculate totals with grade calculator service
        calculateTotalsWithService(result, marksList, gradeCalc);

        return result;
    }

    private static void calculateTotalsWithService(Result result, List<Marks> marksList,
            GradeCalculatorService gradeCalc) {
        // Group marks by subject to handle optional subjects properly
        Map<String, List<Marks>> marksBySubject = marksList.stream()
                .collect(Collectors.groupingBy(m -> m.getSubject().getSubjectName()));

        double totalGP = 0.0;
        int compulsoryCount = 0;
        boolean isFail = false;
        double totalObtained = 0;
        double totalMax = 0;

        for (Map.Entry<String, List<Marks>> entry : marksBySubject.entrySet()) {
            List<Marks> subjectMarks = entry.getValue();

            // Calculate subject totals
            int subjectObtained = subjectMarks.stream().mapToInt(Marks::getObtainedMarks).sum();
            int subjectMax = subjectMarks.stream().mapToInt(m -> m.getSubject().getMaxMarks()).sum();

            double subjectPercentage = subjectMax > 0 ? (subjectObtained * 100.0 / subjectMax) : 0;
            String subjectGrade = gradeCalc.calculateGrade(subjectPercentage);
            double subjectGP = gradeCalc.calculateGradePoint(subjectPercentage);

            // Check if subject is optional
            boolean isOptional = !subjectMarks.isEmpty() && subjectMarks.get(0).getSubject().isOptional();

            if (isOptional) {
                // Optional Logic: If GP >= threshold, add (GP - threshold)
                if (subjectGP >= gradeCalc.getOptionalMinThreshold()) {
                    totalGP += (subjectGP - gradeCalc.getOptionalMinThreshold());
                }
            } else {
                // Compulsory Logic: Add full GP
                totalGP += subjectGP;
                compulsoryCount++;

                // Add to totals for percentage
                totalObtained += subjectObtained;
                totalMax += subjectMax;

                // CHECK FAIL CONDITION
                if (!gradeCalc.isPass(subjectGrade)) {
                    isFail = true;
                }
            }
        }

        // Calculate overall percentage (compulsory subjects only)
        double percentage = totalMax > 0 ? (totalObtained / totalMax) * 100 : 0;
        String grade = gradeCalc.calculateGrade(percentage);

        // Calculate GPA
        double gpa;
        if (compulsoryCount > 0) {
            gpa = totalGP / compulsoryCount;
        } else {
            gpa = 0.0;
        }

        // Cap at max
        if (gpa > gradeCalc.getGpaMaxCap()) {
            gpa = gradeCalc.getGpaMaxCap();
        }

        // Apply Fail Logic
        String resultStatus;
        if (isFail) {
            gpa = 0.00;
            resultStatus = gradeCalc.getFailStatus();
        } else {
            resultStatus = gradeCalc.getPassStatus();
        }

        // Round GPA to 2 decimal places
        gpa = Math.round(gpa * 100.0) / 100.0;

        // Set all calculated values
        result.setTotalObtained(totalObtained);
        result.setTotalMax(totalMax);
        result.setPercentage(percentage);
        result.setGrade(grade);
        result.setGpa(gpa);
        result.setResult(resultStatus);
    }
}
