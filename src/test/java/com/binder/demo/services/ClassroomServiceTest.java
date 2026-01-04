package com.binder.demo.services;

import com.binder.demo.classroom.Classroom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ClassroomServiceTest {

    @Autowired
    private ClassroomService classroomService;

    @Test
    void testCRUDOperations() {
        // Create
        Classroom classroom = new Classroom();
        classroom.setName("Test Class");
        classroom.setDescription("Test Description");
        // Use a known existing teacher from init.sql to avoid FK violation
        UUID creatorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        classroom.setCreatorId(creatorId);
        
        classroomService.createClass(classroom);
        UUID classId = classroom.getClassId();
        assertNotNull(classId);

        // Read
        Optional<Classroom> found = classroomService.getClassById(classId);
        assertTrue(found.isPresent());
        assertEquals("Test Class", found.get().getName());
        assertEquals("Test Description", found.get().getDescription());

        // Update
        Classroom toUpdate = found.get();
        toUpdate.setName("Updated Name");
        toUpdate.setDescription("Updated Description");
        classroomService.updateClass(toUpdate);

        Optional<Classroom> updated = classroomService.getClassById(classId);
        assertTrue(updated.isPresent());
        assertEquals("Updated Name", updated.get().getName());
        assertEquals("Updated Description", updated.get().getDescription());

        // List
        List<Classroom> all = classroomService.getAllClassrooms();
        assertTrue(all.stream().anyMatch(c -> c.getClassId().equals(classId)));

        // Delete
        classroomService.removeClass(classId);
        Optional<Classroom> deleted = classroomService.getClassById(classId);
        assertFalse(deleted.isPresent());
    }
}
