package com.binder.demo.services;

import com.binder.demo.classroom.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles create, update, and lookup operations for classroom posts and submissions.
 */
@Service
public class ClassroomPostService {

    /**
     * JPA entity manager used for post persistence and queries.
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
     * Persists a new assignment post.
     *
     * @param assignment assignment to save
     */
    @Transactional
    public void addAssignment(Assignment assignment) {
        if (assignment == null) return;
        if (assignment.getCreatedAt() == null) assignment.setCreatedAt(Instant.now());
        em.persist(assignment);
    }

    /**
     * Deletes an assignment by id.
     *
     * @param assignmentId assignment id to delete
     */
    @Transactional
    public void removeAssignment(UUID assignmentId) {
        if (assignmentId == null) return;
        Assignment assignment = em.find(Assignment.class, assignmentId);
        if (assignment != null) em.remove(assignment);
    }

    /**
     * Finds an assignment by id.
     *
     * @param assignmentId assignment id to find
     * @return optional assignment
     */
    @Transactional(readOnly = true)
    public Optional<Assignment> findAssignment(UUID assignmentId) {
        if (assignmentId == null) return Optional.empty();
        return Optional.ofNullable(em.find(Assignment.class, assignmentId));
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
     * Updates an assignment with new values.
     *
     * @param assignment assignment to update
     */
    @Transactional
    public void updateAssignment(Assignment assignment) {
        if (assignment == null) return;
        em.merge(assignment);
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

    /**
     * Loads all posts for a classroom and sorts them by creation time.
     *
     * @param classId classroom id
     * @return list of posts for the classroom
     */
    @Transactional(readOnly = true)
    public List<ClassroomPost> getPostsForClassroom(UUID classId) {
        if (classId == null) return List.of();

        List<Assignment> assignments = em.createQuery(
                "select a from Assignment a where a.classId = :classId",
                Assignment.class
        ).setParameter("classId", classId).getResultList();

        List<Resource> resources = em.createQuery(
                "select r from Resource r where r.classId = :classId",
                Resource.class
        ).setParameter("classId", classId).getResultList();

        List<ClassroomPost> posts = new ArrayList<>();
        posts.addAll(assignments);
        posts.addAll(resources);

        posts.sort(Comparator.comparing(ClassroomPost::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return posts;
    }

}
