package ai.agentic.samples.course.model;

/**
 * The CDI event fired when the teacher approves a packet. It triggers the
 * second agent ({@code PublishAgent}) — this is agent-to-agent chaining through
 * plain CDI events: the approval gate (human-in-the-loop) decouples the
 * authoring agent from the publishing agent, and each keeps its own workflow
 * and LLM conversation.
 *
 * @param subject      the subject area
 * @param chapterTitle the chapter title
 * @param packetJson   JSON of the approved {@link CoursePacket}
 * @param runId        correlation id for the live-progress SSE stream
 */
public record LessonApproved(
        String subject,
        String chapterTitle,
        String packetJson,
        String runId) {
}
