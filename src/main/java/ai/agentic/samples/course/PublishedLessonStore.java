package ai.agentic.samples.course;

import ai.agentic.samples.course.model.PublishedLesson;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the latest published lesson (the student-facing artifact produced by
 * {@code PublishAgent}). Single-slot for this single-author demo.
 */
@ApplicationScoped
public class PublishedLessonStore {

    private final AtomicReference<PublishedLesson> current = new AtomicReference<>();

    public void publish(PublishedLesson lesson) {
        current.set(lesson);
    }

    public PublishedLesson current() {
        return current.get();
    }
}
