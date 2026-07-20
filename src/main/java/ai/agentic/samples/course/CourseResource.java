package ai.agentic.samples.course;

import ai.agentic.samples.course.model.CoursePacket;
import ai.agentic.samples.course.model.CoursePacketRequest;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST surface for the Course Content Studio.
 * <p>
 * Every write endpoint fires the {@link CoursePacketRequest} CDI event. Because
 * {@code Event.fire(...)} is synchronous, the whole agent workflow (all LLM
 * calls) completes before it returns, so the resource can read the finished
 * packet back from the {@link PacketStore} and return it in the same response.
 */
@Path("/")
@RequestScoped
public class CourseResource {

    @Inject
    Event<CoursePacketRequest> trigger;

    @Inject
    PacketStore store;

    @Inject
    SubjectRubric rubric;

    @GET
    @Path("subjects")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> subjects() {
        return rubric.subjects();
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
                null, "all", null));
        return respond(store.current());
    }

    @POST
    @Path("packet/refine")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refine(RefineRequest body) {
        return refineSection(new RefineSectionRequest("all",
                body == null ? null : body.instruction()));
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
                body.instruction(), section, store.currentJson()));
        return respond(store.current());
    }

    @POST
    @Path("packet/approve")
    @Produces(MediaType.APPLICATION_JSON)
    public Response approve() {
        CoursePacket approved = store.approve();
        return respond(approved);
    }

    private Response respond(CoursePacket packet) {
        if (packet == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new Message("No packet available.")).build();
        }
        return Response.ok(packet).build();
    }

    public record GenerateRequest(String subject, String chapterTitle, String chapterBody) {
    }

    public record RefineRequest(String instruction) {
    }

    public record RefineSectionRequest(String section, String instruction) {
    }

    public record Message(String message) {
    }
}
