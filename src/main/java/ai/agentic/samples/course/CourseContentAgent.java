package ai.agentic.samples.course;

import ai.agentic.samples.course.model.CoursePacket;
import ai.agentic.samples.course.model.CoursePacketRequest;
import ai.agentic.samples.course.model.Quiz;
import ai.agentic.samples.course.model.QuizQuestion;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.HandleException;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import java.util.List;
import java.util.logging.Logger;

/**
 * Generates a chapter's introduction, quiz and conclusion, and refines them on
 * the teacher's request.
 * <p>
 * No scope annotation is present, so the runtime applies {@code @WorkflowScoped}
 * (the specification default). That is what makes the {@link #draft} instance
 * field safe to use as per-workflow state: one agent instance exists per
 * workflow execution, so the ordered actions can build the packet up
 * incrementally without leaking across concurrent requests.
 * <p>
 * Features on show:
 * <ul>
 *   <li><b>Ordered phases</b> — {@code @Decision}/{@code @Action} carry an
 *       explicit {@code order}, so intro &rarr; quiz &rarr; conclusion run in a
 *       guaranteed sequence (all phases must be ordered once any is).</li>
 *   <li><b>Typed JSON-B output</b> — the quiz comes back as a {@link Quiz}
 *       record straight from {@code query(prompt, Quiz.class, ...)}.</li>
 *   <li><b>Per-workflow conversational memory</b> — the conclusion query does
 *       not re-send the chapter; it relies on the earlier turns of the same
 *       workflow.</li>
 *   <li><b>Human-in-the-loop</b> — the event carries the mode (generate vs.
 *       refine) and a target section, so the teacher can refine one part.</li>
 *   <li><b>Resilience</b> — {@code @HandleException} recovers from a malformed
 *       quiz by re-prompting with a strict schema, falling back to a placeholder
 *       so the packet is still published.</li>
 * </ul>
 */
@Agent(name = "CourseContentAgent",
        description = "Generates and refines an intro, quiz and conclusion for a course chapter.")
public class CourseContentAgent {

    private static final Logger LOGGER = Logger.getLogger(CourseContentAgent.class.getName());

    private static final String QUIZ_SCHEMA =
            "Return ONLY valid JSON, no markdown fences, of the exact form: "
            + "{\"questions\":[{\"prompt\":\"...\",\"options\":[\"a\",\"b\",\"c\",\"d\"],"
            + "\"correctIndex\":0,\"explanation\":\"...\"}]}";

    @Inject
    LargeLanguageModel model;

    @Inject
    SubjectRubric rubric;

    @Inject
    PacketStore store;

    /** Per-workflow state: the packet being built or refined. */
    private CoursePacket draft;

    @Trigger
    void onRequest(@Valid CoursePacketRequest request) {
        if (request.isGenerate()) {
            draft = new CoursePacket(request.subject(), request.chapterTitle());
            LOGGER.info("[TRIGGER] generate packet for " + request.subject()
                    + " / " + request.chapterTitle());
        } else {
            draft = Json.instance().fromJson(request.currentDraftJson(), CoursePacket.class);
            LOGGER.info("[TRIGGER] refine section '" + request.section()
                    + "' with instruction: " + request.instruction());
        }
    }

    @Decision(order = 5)
    boolean hasTeachableContent(CoursePacketRequest request) {
        boolean ok = request.isGenerate()
                ? request.chapterBody() != null && request.chapterBody().strip().length() >= 80
                : draft != null;
        LOGGER.info("[DECISION] hasTeachableContent=" + ok);
        return ok;
    }

    @Action(order = 10)
    void writeIntro(CoursePacketRequest request) {
        if (!request.targets("intro")) {
            return;
        }
        LOGGER.info("[ACTION] writeIntro");
        if (request.isGenerate()) {
            draft.setIntro(model.query(
                    rubric.forSubject(request.subject())
                            + "\nWrite an engaging two-paragraph introduction for this chapter. "
                            + "Return plain prose only:\n{}",
                    request.chapterBody()));
        } else {
            draft.setIntro(model.query(
                    "Current introduction:\n{}\n\nApply this change and return only the "
                            + "new introduction as plain prose: {}",
                    draft.getIntro(), request.instruction()));
        }
    }

    @Action(order = 20)
    void writeQuiz(CoursePacketRequest request) {
        if (!request.targets("quiz")) {
            return;
        }
        LOGGER.info("[ACTION] writeQuiz (typed)");
        if (request.isGenerate()) {
            draft.setQuiz(model.query(
                    rubric.forSubject(request.subject())
                            + "\nCreate exactly 4 multiple-choice questions based on this chapter. "
                            + QUIZ_SCHEMA + "\nChapter:\n{}",
                    Quiz.class, request.chapterBody()));
        } else {
            draft.setQuiz(model.query(
                    "Current quiz JSON:\n{}\n\nApply this change: {}\n" + QUIZ_SCHEMA,
                    Quiz.class, draft.getQuiz(), request.instruction()));
        }
    }

    @Action(order = 30)
    void writeConclusion(CoursePacketRequest request) {
        if (!request.targets("conclusion")) {
            return;
        }
        LOGGER.info("[ACTION] writeConclusion (uses workflow memory)");
        if (request.isGenerate()) {
            // No chapter re-sent: relies on the intro and quiz produced earlier
            // in this same workflow (per-workflow conversational state).
            draft.setConclusion(model.query(
                    "Based on the chapter and the introduction and quiz you just produced, "
                            + "write a concise one-paragraph conclusion that ties them together. "
                            + "Return plain prose only."));
        } else {
            draft.setConclusion(model.query(
                    "Current conclusion:\n{}\n\nApply this change and return only the "
                            + "new conclusion as plain prose: {}",
                    draft.getConclusion(), request.instruction()));
        }
    }

    @Outcome
    void publish(CoursePacketRequest request) {
        store.publish(draft);
        LOGGER.info("[OUTCOME] packet published for " + draft.getChapterTitle());
    }

    @HandleException
    void onBadQuiz(LLMException e) {
        LOGGER.warning("[HANDLE] quiz step failed (" + e.getMessage()
                + ") — retrying with strict schema");
        try {
            draft.setQuiz(model.query(
                    "Regenerate the quiz. " + QUIZ_SCHEMA, Quiz.class));
        } catch (RuntimeException retryFailed) {
            LOGGER.warning("[HANDLE] strict retry failed too; using placeholder quiz");
            draft.setQuiz(new Quiz(List.of(new QuizQuestion(
                    "Quiz generation failed — please refine or regenerate.",
                    List.of("OK"), 0, "The model did not return valid quiz JSON."))));
        }
        // Still publish so the teacher keeps the intro/conclusion already produced.
        store.publish(draft);
    }

    @HandleException
    void onInvalidRequest(ConstraintViolationException e) {
        LOGGER.warning("[HANDLE] invalid request rejected: " + e.getMessage());
        // Nothing published; the REST layer reports "no packet".
    }
}
