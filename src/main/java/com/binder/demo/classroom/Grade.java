package com.binder.demo.classroom;

import java.util.UUID;

/*
    Grade
    Represents teacher feedback + marks for a submission.
    Backed by the grades table.
 */
public class Grade {

    private UUID assignmentId;
    private UUID studentId;
    private UUID teacherId;

    private Integer marksScored;
    private String feedback;

    public UUID getAssignmentId() { return assignmentId; }
    public void setAssignmentId(UUID assignmentId) { this.assignmentId = assignmentId; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public UUID getTeacherId() { return teacherId; }
    public void setTeacherId(UUID teacherId) { this.teacherId = teacherId; }

    public Integer getMarksScored() { return marksScored; }
    public void setMarksScored(Integer marksScored) { this.marksScored = marksScored; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
