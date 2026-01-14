package com.binder.demo.services;

import com.binder.demo.classroom.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles resource post persistence operations.
 */
@Service
public class ResourcePostService {

    /**
     * JPA entity manager used for resource persistence and queries.
     */
    @PersistenceContext
    private EntityManager em;

    /**
     * Persists a new resource post.
     *
     * @param resource resource to save
     */
    @Transactional
    public void addResource(Resource resource) {
        if (resource == null) return;
        if (resource.getCreatedAt() == null) resource.setCreatedAt(Instant.now());
        em.persist(resource);
    }

    /**
     * Deletes a resource by id.
     *
     * @param resourceId resource id to delete
     */
    @Transactional
    public void removeResource(UUID resourceId) {
        if (resourceId == null) return;
        Resource resource = em.find(Resource.class, resourceId);
        if (resource != null) em.remove(resource);
    }

    /**
     * Finds a resource by id.
     *
     * @param resourceId resource id to find
     * @return optional resource
     */
    @Transactional(readOnly = true)
    public Optional<Resource> findResource(UUID resourceId) {
        if (resourceId == null) return Optional.empty();
        return Optional.ofNullable(em.find(Resource.class, resourceId));
    }

    /**
     * Updates a resource with new values.
     *
     * @param resource resource to update
     */
    @Transactional
    public void updateResource(Resource resource) {
        if (resource == null) return;
        em.merge(resource);
    }
}
