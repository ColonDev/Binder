package com.binder.demo.classroom;

import com.binder.demo.attachments.Attachment;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignment_submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id"}))
public class AssignmentSubmission {

    @Id
    @UuidGenerator
    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "submission_time", nullable = false)
    private Instant submissionTime;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Attachment attachment;

    @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL, optional = true)
    private Grade grade;

    @PrePersist
    void prePersist() {
        if (submissionTime == null) submissionTime = Instant.now();
    }

    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }

    public UUID getAssignmentId() { return assignmentId; }
    public void setAssignmentId(UUID assignmentId) { this.assignmentId = assignmentId; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public Instant getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(Instant submissionTime) { this.submissionTime = submissionTime; }

    public Attachment getAttachment() { return attachment; }
    public void setAttachment(Attachment attachment) { this.attachment = attachment; }

    public Grade getGrade() { return grade; }
    public void setGrade(Grade grade) { this.grade = grade; }
}
