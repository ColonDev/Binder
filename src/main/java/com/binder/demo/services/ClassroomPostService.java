package com.binder.demo.services;

import com.binder.demo.classroom.Assignment;
import com.binder.demo.classroom.ClassroomPost;
import com.binder.demo.classroom.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Handles feed lookup operations for classroom posts.
 */
@Service
public class ClassroomPostService {

    /**
     * JPA entity manager used for post persistence and queries.
     */
    @PersistenceContext
    private EntityManager em;

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
