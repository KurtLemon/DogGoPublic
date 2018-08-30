package com.kurtlemon.doggo;

import java.util.Date;

public class UserReport {

    // Report information including information on who the report was filed by, about, when, and
    //  what.
    private String userID;
    private Date date;
    private String reportingUserID;
    private String report;

    /**
     * Constructor with all the necessary information.
     *
     * @param userID The user who the report is about.
     * @param date The date and time when the report is filed.
     * @param reportingUserID The user who filed the report.
     * @param report The message string describing the report.
     */
    public UserReport(String userID, Date date, String reportingUserID, String report) {
        this.userID = userID;
        this.date = date;
        this.reportingUserID = reportingUserID;
        this.report = report;

    }

    // Getters and Setters.
    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getReportingUserID() {
        return reportingUserID;
    }

    public void setReportingUserID(String reportingUserID) {
        this.reportingUserID = reportingUserID;
    }
}
