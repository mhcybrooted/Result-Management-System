package mh.cyb.root.rms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Centralized grade calculation service
 * Reads grade scale configuration from application.properties
 */
@Service
public class GradeCalculatorService {

    @Value("${grade.aplus.min}")
    private int minAPlus;

    @Value("${grade.a.min}")
    private int minA;

    @Value("${grade.aminus.min}")
    private int minAMinus;

    @Value("${grade.b.min}")
    private int minB;

    @Value("${grade.c.min}")
    private int minC;

    @Value("${grade.d.min}")
    private int minD;

    @Value("${grade.aplus.point}")
    private double pointAPlus;

    @Value("${grade.a.point}")
    private double pointA;

    @Value("${grade.aminus.point}")
    private double pointAMinus;

    @Value("${grade.b.point}")
    private double pointB;

    @Value("${grade.c.point}")
    private double pointC;

    @Value("${grade.d.point}")
    private double pointD;

    @Value("${gpa.optional.min.threshold}")
    private double optionalMinThreshold;

    @Value("${gpa.max.cap}")
    private double gpaMaxCap;

    /**
     * Calculate letter grade from percentage
     */
    public String calculateGrade(double percentage) {
        if (percentage >= minAPlus)
            return "A+";
        if (percentage >= minA)
            return "A";
        if (percentage >= minAMinus)
            return "A-";
        if (percentage >= minB)
            return "B";
        if (percentage >= minC)
            return "C";
        if (percentage >= minD)
            return "D";
        return "F";
    }

    /**
     * Calculate grade point from percentage
     */
    public double calculateGradePoint(double percentage) {
        if (percentage >= minAPlus)
            return pointAPlus;
        if (percentage >= minA)
            return pointA;
        if (percentage >= minAMinus)
            return pointAMinus;
        if (percentage >= minB)
            return pointB;
        if (percentage >= minC)
            return pointC;
        if (percentage >= minD)
            return pointD;
        return 0.00;
    }

    /**
     * Get optional subject minimum GP threshold
     */
    public double getOptionalMinThreshold() {
        return optionalMinThreshold;
    }

    public double getGpaMaxCap() {
        return gpaMaxCap;
    }

    // --- Threshold Getters ---
    public int getMinAPlus() {
        return minAPlus;
    }

    public int getMinA() {
        return minA;
    }

    public int getMinB() {
        return minB;
    }

    public int getMinC() {
        return minC;
    }

    public int getMinD() {
        return minD;
    }

    public double getPointAPlus() {
        return pointAPlus;
    }

    public double getPointA() {
        return pointA;
    }

    public double getPointB() {
        return pointB;
    }

    public double getPointC() {
        return pointC;
    }

    public double getPointD() {
        return pointD;
    }

    // --- Helper Methods ---

    public boolean isPass(double percentage) {
        return percentage >= minD;
    }

    public boolean isPass(String grade) {
        return !"F".equals(grade);
    }

    public String getPassStatus() {
        return "PASS";
    }

    public String getFailStatus() {
        return "FAIL";
    }

}
