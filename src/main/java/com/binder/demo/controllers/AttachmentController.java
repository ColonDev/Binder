package com.binder.demo.controllers;

import com.binder.demo.attachments.Attachment;
import com.binder.demo.services.AttachmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Serves attachment downloads and inline previews.
 */
@Controller
@RequestMapping("/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final Path storageDir;

    /**
     * Creates a controller with required services and storage configuration.
     *
     * @param attachmentService attachment service
     * @param storageDir base directory for stored attachments
     */
    public AttachmentController(AttachmentService attachmentService,
                                @Value("${attachments.storage-dir:uploads}") String storageDir) {
        this.attachmentService = attachmentService;
        this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    /**
     * Downloads an attachment by ID.
     *
     * @param attachmentId attachment id
     * @param session current session
     * @return attachment response or error status
     */
    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID attachmentId,
                                                       HttpSession session) throws MalformedURLException {
        return handleAttachmentRequest(attachmentId, session, false);
    }

    /**
     * Streams an attachment inline by ID.
     *
     * @param attachmentId attachment id
     * @param session current session
     * @return attachment response or error status
     */
    @GetMapping("/{attachmentId}/inline")
    public ResponseEntity<Resource> inlineAttachment(@PathVariable UUID attachmentId,
                                                     HttpSession session) throws MalformedURLException {
        return handleAttachmentRequest(attachmentId, session, true);
    }

    /**
     * Loads an attachment and returns either a redirect or file response.
     *
     * @param attachmentId attachment id
     * @param session current session
     * @param inline true to use inline disposition
     * @return attachment response or error status
     */
    private ResponseEntity<Resource> handleAttachmentRequest(UUID attachmentId,
                                                             HttpSession session,
                                                             boolean inline) throws MalformedURLException {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Attachment> attachmentOpt = attachmentService.get(attachmentId);
        if (attachmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Attachment attachment = attachmentOpt.get();
        String url = attachment.getUrl().trim();
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (uri.isAbsolute()) {
            return redirectTo(uri);
        }

        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return serveLocalAttachment(path, inline);
    }

    /**
     * Determines if the current session has a logged-in user.
     *
     * @param session current session
     * @return true when authenticated
     */
    private boolean isAuthenticated(HttpSession session) {
        return session != null && session.getAttribute("userId") != null;
    }

    /**
     * Builds a redirect response to an external URL.
     *
     * @param uri external URI
     * @return redirect response
     */
    private ResponseEntity<Resource> redirectTo(URI uri) {
        return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
    }

    /**
     * Serves a local attachment file from the configured storage directory.
     *
     * @param url stored relative URL
     * @param inline true to use inline disposition
     * @return file response or error status
     */
    private ResponseEntity<Resource> serveLocalAttachment(String url, boolean inline) throws MalformedURLException {
        Path resolved = resolveStoragePath(url);
        if (resolved == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            return ResponseEntity.notFound().build();
        }
        return buildFileResponse(resolved, inline);
    }

    /**
     * Resolves a relative URL against the storage directory, preventing traversal.
     *
     * @param url stored relative URL
     * @return normalized path within storage or null when invalid
     */
    private Path resolveStoragePath(String url) {
        Path candidate = storageDir.resolve(url).normalize();
        if (!candidate.startsWith(storageDir)) {
            return null;
        }
        return candidate;
    }

    /**
     * Builds a response for a local file attachment.
     *
     * @param path local file path
     * @param inline true to use inline disposition
     * @return response entity with resource body
     */
    private ResponseEntity<Resource> buildFileResponse(Path path, boolean inline) throws MalformedURLException {
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = detectMediaType(path);
        ContentDisposition disposition = inline
                ? ContentDisposition.inline().filename(path.getFileName().toString()).build()
                : ContentDisposition.attachment().filename(path.getFileName().toString()).build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    /**
     * Determines the content type for a local file.
     *
     * @param path local file path
     * @return resolved media type or octet-stream
     */
    private MediaType detectMediaType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            if (contentType != null) {
                return MediaType.parseMediaType(contentType);
            }
        } catch (IOException ignored) {
            // Fall back to octet-stream when probing fails.
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
