package com.binder.demo.classroom;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic container class representing a resource post in a classroom.
 */
public class Resource extends ClassroomPost {

    public Resource() {setPostType(PostType.RESOURCE);
    }

    public Resource(UUID resourceId, UUID classId, String title, String description, Instant createdTime, UUID creatorTeacherId) {
        setPostType(PostType.RESOURCE);
        setId(resourceId);
        setClassId(classId);
        setTitle(title);
        setDescription(description);
        setCreatedTime(createdTime);
        setCreatorTeacherId(creatorTeacherId);
    }
}
