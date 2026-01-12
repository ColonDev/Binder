package com.binder.demo.classroom;

import com.binder.demo.attachments.Attachment;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "assignments")
public class Assignment extends ClassroomPost {

    @Id
    @UuidGenerator
    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "time_to_complete")
    private String timeToComplete;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "maximum_marks")
    private Integer maxMarks;

    @ManyToMany
    @JoinTable(
            name = "assignment_attachments",
            joinColumns = @JoinColumn(name = "assignment_id"),
            inverseJoinColumns = @JoinColumn(name = "attachment_id")
    )
    private Set<Attachment> attachments = new HashSet<>();

    public Assignment() {
        setPostType(PostType.ASSIGNMENT);
    }

    @PostLoad
    void postLoad() {
        setPostType(PostType.ASSIGNMENT);
    }

    public UUID getAssignmentId() { return assignmentId; }
    public void setAssignmentId(UUID assignmentId) { this.assignmentId = assignmentId; }

    @Override
    public UUID getPostId() { return assignmentId; }

    public String getTimeToComplete() { return timeToComplete; }
    public void setTimeToComplete(String timeToComplete) { this.timeToComplete = timeToComplete; }

    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }

    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }

    public Set<Attachment> getAttachments() { return attachments; }
    public void setAttachments(Set<Attachment> attachments) {
        this.attachments = (attachments == null) ? new HashSet<>() : attachments;
    }
}
