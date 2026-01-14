package com.binder.demo.classroom;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @MapsId("submissionId")
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", referencedColumnName = "submission_id", nullable = false)
    private AssignmentSubmission submission;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "marks_scored")
    private Integer marksScored;

    @Column(name = "feedback")
    private String feedback;

    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }

    public AssignmentSubmission getSubmission() { return submission; }
    public void setSubmission(AssignmentSubmission submission) { this.submission = submission; }

    public UUID getTeacherId() { return teacherId; }
    public void setTeacherId(UUID teacherId) { this.teacherId = teacherId; }

    public Integer getMarksScored() { return marksScored; }
    public void setMarksScored(Integer marksScored) { this.marksScored = marksScored; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
