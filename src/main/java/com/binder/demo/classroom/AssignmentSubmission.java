package com.binder.demo.classroom;

import java.time.Instant;
import java.util.UUID;

/*
    AssignmentSubmission
    Represents a student's submission for an assignment.
    Backed by the assignment_submissions table.

    Grade is optional and can be populated by the service.
 */
public class AssignmentSubmission {

    private UUID assignmentId;
    private UUID studentId;
    private UUID attachmentId;
    private Instant submissionTime;

    private Grade grade; // null if ungraded

    public UUID getAssignmentId() { return assignmentId; }
    public void setAssignmentId(UUID assignmentId) { this.assignmentId = assignmentId; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public UUID getAttachmentId() { return attachmentId; }
    public void setAttachmentId(UUID attachmentId) { this.attachmentId = attachmentId; }

    public Instant getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(Instant submissionTime) { this.submissionTime = submissionTime; }

    public Grade getGrade() { return grade; }
    public void setGrade(Grade grade) { this.grade = grade; }
}
