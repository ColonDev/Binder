CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
                       user_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email      TEXT NOT NULL UNIQUE,
                       full_name  TEXT NOT NULL,
                       role       TEXT NOT NULL CHECK (role IN ('STUDENT', 'TEACHER')),
                       created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE authentications (
                                 auth_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 user_id       UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                 provider      TEXT NOT NULL CHECK (provider IN ('LOCAL', 'GOOGLE', 'GITHUB')),
                                 provider_id   TEXT NOT NULL,
                                 password_hash TEXT,
                                 created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
                                 UNIQUE (user_id, provider)
);

-- CLASSROOMS & ENROLLMENTS
CREATE TABLE classrooms (
                            class_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name        TEXT NOT NULL,
                            description TEXT,
                            created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE enrollments (
                             enrollment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             class_id      UUID NOT NULL REFERENCES classrooms(class_id) ON DELETE CASCADE,
                             student_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                             UNIQUE (class_id, student_id)
);

CREATE TABLE classroom_teachers (
                                    class_id   UUID NOT NULL REFERENCES classrooms(class_id) ON DELETE CASCADE,
                                    teacher_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                    PRIMARY KEY (class_id, teacher_id)
);

CREATE TABLE assignments (
                             assignment_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             class_id           UUID NOT NULL REFERENCES classrooms(class_id) ON DELETE CASCADE,
                             title              TEXT NOT NULL,
                             description        TEXT,
                             creator_teacher_id UUID NOT NULL REFERENCES users(user_id),
                             created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
                             time_to_complete   TEXT,
                             due_date           TIMESTAMP,
                             maximum_marks      INT CHECK (maximum_marks > 0)
);

CREATE TABLE resources (
                           resource_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           class_id           UUID NOT NULL REFERENCES classrooms(class_id) ON DELETE CASCADE,
                           title              TEXT NOT NULL,
                           description        TEXT,
                           creator_teacher_id UUID NOT NULL REFERENCES users(user_id),
                           created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE attachments (
                             attachment_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             attachment_type TEXT NOT NULL CHECK (attachment_type IN ('FILE', 'LINK', 'IMAGE')),
                             url             TEXT NOT NULL,
                             uploaded_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                             user_owner      UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE assignment_attachments (
                                        assignment_id UUID NOT NULL REFERENCES assignments(assignment_id) ON DELETE CASCADE,
                                        attachment_id UUID NOT NULL REFERENCES attachments(attachment_id) ON DELETE CASCADE,
                                        PRIMARY KEY (assignment_id, attachment_id)
);

CREATE TABLE resource_attachments (
                                      resource_id   UUID NOT NULL REFERENCES resources(resource_id) ON DELETE CASCADE,
                                      attachment_id UUID NOT NULL REFERENCES attachments(attachment_id) ON DELETE CASCADE,
                                      PRIMARY KEY (resource_id, attachment_id)
);

CREATE TABLE assignment_submissions (
                                        submission_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        assignment_id    UUID NOT NULL REFERENCES assignments(assignment_id) ON DELETE CASCADE,
                                        student_id       UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                        submission_time  TIMESTAMP NOT NULL DEFAULT NOW(),
                                        attachment_id    UUID REFERENCES attachments(attachment_id) ON DELETE SET NULL,
                                        UNIQUE (assignment_id, student_id)
);


CREATE TABLE grades (
                        submission_id UUID PRIMARY KEY REFERENCES assignment_submissions(submission_id) ON DELETE CASCADE,
                        teacher_id    UUID NOT NULL REFERENCES users(user_id),
                        marks_scored  INT CHECK (marks_scored >= 0),
                        feedback      TEXT
);


CREATE TABLE flashcard_sets (
                                flashcard_set_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                created_by       UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                title            TEXT NOT NULL,
                                created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE flashcards (
                            flashcard_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            flashcard_set_id UUID NOT NULL REFERENCES flashcard_sets(flashcard_set_id) ON DELETE CASCADE,
                            front            TEXT NOT NULL,
                            back             TEXT NOT NULL
);

CREATE TABLE flashcard_assignments (
                                       flashcard_set_id UUID NOT NULL REFERENCES flashcard_sets(flashcard_set_id) ON DELETE CASCADE,
                                       assignment_id    UUID NOT NULL REFERENCES assignments(assignment_id) ON DELETE CASCADE,
                                       PRIMARY KEY (flashcard_set_id, assignment_id)
);

CREATE TABLE flashcard_resources (
                                     flashcard_set_id UUID NOT NULL REFERENCES flashcard_sets(flashcard_set_id) ON DELETE CASCADE,
                                     resource_id      UUID NOT NULL REFERENCES resources(resource_id) ON DELETE CASCADE,
                                     PRIMARY KEY (flashcard_set_id, resource_id)
);

CREATE TABLE user_flashcard_sets (
                                     flashcard_set_id UUID NOT NULL REFERENCES flashcard_sets(flashcard_set_id) ON DELETE CASCADE,
                                     user_id          UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                     last_studied     TIMESTAMP,
                                     PRIMARY KEY (flashcard_set_id, user_id)
);

CREATE TABLE user_flashcard_progress (
                                         user_id         UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                         flashcard_id    UUID NOT NULL REFERENCES flashcards(flashcard_id) ON DELETE CASCADE,
                                         ease_factor     FLOAT NOT NULL DEFAULT 2.5,
                                         review_interval INT   NOT NULL DEFAULT 0, -- avoid reserved word "interval"
                                         repetitions     INT   NOT NULL DEFAULT 0,
                                         last_reviewed   TIMESTAMP,
                                         next_review     TIMESTAMP,
                                         PRIMARY KEY (user_id, flashcard_id)
);

CREATE TABLE sessions (
                          session_id  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          user_id     UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                          login_time  TIMESTAMP NOT NULL DEFAULT NOW(),
                          logout_time TIMESTAMP
);

CREATE TABLE events (
                        event_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        event_time TIMESTAMP NOT NULL DEFAULT NOW(),
                        event_type TEXT NOT NULL
);

CREATE TABLE session_events (
                                event_id UUID NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
                                user_id  UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                PRIMARY KEY (event_id, user_id)
);

CREATE TABLE notifications (
                               notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id         UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                               context_id      UUID NOT NULL,
                               sent_at         TIMESTAMP NOT NULL DEFAULT NOW(),
                               is_read         BOOL NOT NULL DEFAULT FALSE
);

CREATE TABLE chats (
                       chat_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       context_type TEXT NOT NULL CHECK (context_type IN ('CLASSROOM', 'DIRECT', 'ASSIGNMENT'))
);

CREATE TABLE chat_participants (
                                   chat_id UUID NOT NULL REFERENCES chats(chat_id) ON DELETE CASCADE,
                                   user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                   PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE messages (
                          message_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          chat_id        UUID NOT NULL REFERENCES chats(chat_id) ON DELETE CASCADE,
                          sender_user_id UUID NOT NULL REFERENCES users(user_id),
                          content        TEXT NOT NULL,
                          sent_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- TEST DATA
INSERT INTO users (user_id, email, full_name, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'teacher@binder.com', 'Joseph Joestar', 'TEACHER');

INSERT INTO authentications (user_id, provider, provider_id, password_hash)
VALUES ('00000000-0000-0000-0000-000000000001', 'LOCAL', 'teacher@binder.com', crypt('teacher123', gen_salt('bf')));

INSERT INTO users (user_id, email, full_name, role)
VALUES ('00000000-0000-0000-0000-000000000002', 'student@binder.com', 'Jotaro Kujoh', 'STUDENT');

INSERT INTO authentications (user_id, provider, provider_id, password_hash)
VALUES ('00000000-0000-0000-0000-000000000002', 'LOCAL', 'student@binder.com', crypt('student123', gen_salt('bf')));
