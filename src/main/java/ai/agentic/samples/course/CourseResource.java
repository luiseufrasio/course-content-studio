package ai.agentic.samples.course;

import ai.agentic.samples.course.model.CoursePacket;
import ai.agentic.samples.course.model.CoursePacketRequest;
import ai.agentic.samples.course.model.LessonApproved;
import ai.agentic.samples.course.model.PublishedLesson;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.util.List;

/**
 * REST surface for the Course Content Studio.
 * <p>
 * Every write endpoint fires the {@link CoursePacketRequest} CDI event. Because
 * {@code Event.fire(...)} is synchronous, the whole agent workflow (all LLM
 * calls) completes before it returns, so the resource can read the finished
 * packet back from the {@link PacketStore} and return it in the same response.
 * Live progress is streamed separately over {@code /api/progress/{runId}}.
 */
@Path("/")
@RequestScoped
public class CourseResource {

    @Inject
    Event<CoursePacketRequest> trigger;

    @Inject
    Event<LessonApproved> approvals;

    @Inject
    PacketStore store;

    @Inject
    PublishedLessonStore publishedLessons;

    @Inject
    SubjectRubric rubric;

    @Inject
    ProgressTracker progress;

    @GET
    @Path("subjects")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> subjects() {
        return rubric.subjects();
    }

    /** Live phase progress for one run, as Server-Sent Events. */
    @GET
    @Path("progress/{runId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void progress(@PathParam("runId") String runId,
                         @Context SseEventSink sink,
                         @Context Sse sse) {
        progress.register(runId, sink, sse);
    }

    @GET
    @Path("packet")
    @Produces(MediaType.APPLICATION_JSON)
    public Response current() {
        return respond(store.current());
    }

    @POST
    @Path("packet/generate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generate(GenerateRequest body) {
        if (body == null || body.subject() == null || body.subject().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new Message("A subject is required.")).build();
        }
        trigger.fire(new CoursePacketRequest(
                body.subject(), body.chapterTitle(), body.chapterBody(),
                null, "all", null, body.runId()));
        return respond(store.current());
    }

    @POST
    @Path("packet/refine")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refine(RefineRequest body) {
        return refineSection(new RefineSectionRequest("all",
                body == null ? null : body.instruction(),
                body == null ? null : body.runId()));
    }

    @POST
    @Path("packet/refine-section")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refineSection(RefineSectionRequest body) {
        CoursePacket base = store.current();
        if (base == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new Message("Nothing to refine yet — generate a packet first."))
                    .build();
        }
        if (body == null || body.instruction() == null || body.instruction().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new Message("A refine instruction is required.")).build();
        }
        String section = body.section() == null || body.section().isBlank()
                ? "all" : body.section();
        trigger.fire(new CoursePacketRequest(
                base.getSubject(), base.getChapterTitle(), null,
                body.instruction(), section, store.currentJson(), body.runId()));
        return respond(store.current());
    }

    @POST
    @Path("packet/approve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response approve(ApproveRequest body) {
        CoursePacket approved = store.approve();
        if (approved == null) {
            return respond(null);
        }
        // Chain to the second agent: firing this event triggers PublishAgent,
        // which builds the student-facing lesson synchronously.
        approvals.fire(new LessonApproved(
                approved.getSubject(), approved.getChapterTitle(),
                store.currentJson(), body == null ? null : body.runId()));
        return respond(approved);
    }

    /** The student-facing published lesson (produced by PublishAgent). */
    @GET
    @Path("lesson")
    @Produces(MediaType.APPLICATION_JSON)
    public Response lesson() {
        PublishedLesson lesson = publishedLessons.current();
        if (lesson == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new Message("No lesson has been published yet.")).build();
        }
        return Response.ok(lesson).build();
    }

    private Response respond(CoursePacket packet) {
        if (packet == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new Message("No packet available.")).build();
        }
        return Response.ok(packet).build();
    }

    public record GenerateRequest(String subject, String chapterTitle, String chapterBody,
                                  String runId) {
    }

    public record RefineRequest(String instruction, String runId) {
    }

    public record RefineSectionRequest(String section, String instruction, String runId) {
    }

    public record ApproveRequest(String runId) {
    }

    public record Message(String message) {
    }
}
