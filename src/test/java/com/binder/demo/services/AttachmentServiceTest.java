package com.binder.demo.services;

import com.binder.demo.attachments.Attachment;
import com.binder.demo.attachments.AttachmentType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private EntityManager em;

    private AttachmentService service;

    @BeforeEach
    void setUp() {
        service = new AttachmentService();
        ReflectionTestUtils.setField(service, "em", em);
    }

    @Test
    void uploadGetUpdateDeleteFlow() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentType(AttachmentType.FILE);
        attachment.setUrl("http://example.com/file.txt");
        attachment.setUserOwner(UUID.randomUUID());

        doAnswer(invocation -> {
            Attachment arg = invocation.getArgument(0);
            if (arg.getAttachmentId() == null) {
                arg.setAttachmentId(UUID.randomUUID());
            }
            if (arg.getUploadedAt() == null) {
                arg.setUploadedAt(Instant.now());
            }
            return null;
        }).when(em).persist(any(Attachment.class));

        Attachment uploaded = service.upload(attachment);
        assertNotNull(uploaded.getAttachmentId());
        assertNotNull(uploaded.getUploadedAt());

        when(em.find(Attachment.class, uploaded.getAttachmentId())).thenReturn(uploaded);
        Optional<Attachment> fetched = service.get(uploaded.getAttachmentId());
        assertTrue(fetched.isPresent());
        assertEquals(uploaded.getAttachmentId(), fetched.get().getAttachmentId());

        Instant newTime = Instant.now().plusSeconds(60);
        uploaded.setUploadedAt(newTime);
        uploaded.setUrl("http://example.com/updated.txt");
        when(em.merge(uploaded)).thenReturn(uploaded);
        Attachment updated = service.update(uploaded);
        assertEquals(newTime, updated.getUploadedAt());
        assertEquals("http://example.com/updated.txt", updated.getUrl());

        service.delete(uploaded.getAttachmentId());
        verify(em).remove(uploaded);
        when(em.find(Attachment.class, uploaded.getAttachmentId())).thenReturn(null);
        assertTrue(service.get(uploaded.getAttachmentId()).isEmpty());
    }

    @Test
    void updateWithoutIdCreatesNewId() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentType(AttachmentType.LINK);
        attachment.setUrl("http://example.com");
        attachment.setUserOwner(UUID.randomUUID());

        doAnswer(invocation -> {
            Attachment arg = invocation.getArgument(0);
            if (arg.getAttachmentId() == null) {
                arg.setAttachmentId(UUID.randomUUID());
            }
            if (arg.getUploadedAt() == null) {
                arg.setUploadedAt(Instant.now());
            }
            return null;
        }).when(em).persist(any(Attachment.class));

        Attachment updated = service.update(attachment);
        assertNotNull(updated.getAttachmentId());
        assertNotNull(updated.getUploadedAt());
    }

    @Test
    void uploadRejectsPathTraversalUrl() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentType(AttachmentType.FILE);
        attachment.setUrl("../etc/passwd");
        attachment.setUserOwner(UUID.randomUUID());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(attachment)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("path traversal"));
        assertNull(attachment.getAttachmentId());
    }

    @Test
    void uploadRejectsMissingType() {
        Attachment attachment = new Attachment();
        attachment.setUrl("http://example.com/file.txt");
        attachment.setUserOwner(UUID.randomUUID());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(attachment)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("type"));
    }

    @Test
    void uploadRejectsMissingOwner() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentType(AttachmentType.FILE);
        attachment.setUrl("http://example.com/file.txt");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(attachment)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("owner"));
    }

    @Test
    void uploadRejectsUnsupportedScheme() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentType(AttachmentType.LINK);
        attachment.setUrl("javascript:alert(1)");
        attachment.setUserOwner(UUID.randomUUID());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(attachment)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("scheme"));
    }

    @Test
    void uploadRejectsRelativeUrlForLink() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentType(AttachmentType.LINK);
        attachment.setUrl("/files/attachment.txt");
        attachment.setUserOwner(UUID.randomUUID());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.upload(attachment)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("absolute"));
    }

    @Test
    void uploadAllowsRelativeUrlForFile() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentType(AttachmentType.FILE);
        attachment.setUrl("uploads/file.txt");
        attachment.setUserOwner(UUID.randomUUID());

        doAnswer(invocation -> {
            Attachment arg = invocation.getArgument(0);
            if (arg.getAttachmentId() == null) {
                arg.setAttachmentId(UUID.randomUUID());
            }
            if (arg.getUploadedAt() == null) {
                arg.setUploadedAt(Instant.now());
            }
            return null;
        }).when(em).persist(any(Attachment.class));

        Attachment uploaded = service.upload(attachment);
        assertNotNull(uploaded.getAttachmentId());
    }
}
