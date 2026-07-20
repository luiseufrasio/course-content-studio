package ai.agentic.samples.course.model;

import jakarta.validation.constraints.NotBlank;

/**
 * The CDI event that triggers the {@code CourseContentAgent} workflow.
 * <p>
 * The event carries the <em>mode</em>, exactly like a document-editing agent:
 * <ul>
 *   <li>{@code currentDraftJson} null/blank &rarr; <strong>generate</strong> a
 *       brand-new packet from {@code chapterBody};</li>
 *   <li>{@code currentDraftJson} present &rarr; <strong>refine</strong> the
 *       existing packet by applying {@code instruction}.</li>
 * </ul>
 * {@code section} scopes a refinement to a single part ({@code "intro"},
 * {@code "quiz"}, {@code "conclusion"}) or the whole packet ({@code "all"} /
 * null), so refining the quiz never disturbs the introduction.
 *
 * @param subject          the subject area (e.g. {@code "Mathematics"}); shapes
 *                         the writing rubric
 * @param chapterTitle     the chapter title
 * @param chapterBody      the raw chapter content (used in generate mode)
 * @param instruction      the teacher's refinement instruction (refine mode)
 * @param section          which part to (re)write: {@code intro|quiz|conclusion|all}
 * @param currentDraftJson JSON of the current packet, or null to generate
 */
public record CoursePacketRequest(
        @NotBlank String subject,
        String chapterTitle,
        String chapterBody,
        String instruction,
        String section,
        String currentDraftJson) {

    /** True when there is no existing draft, i.e. we generate from scratch. */
    public boolean isGenerate() {
        return currentDraftJson == null || currentDraftJson.isBlank();
    }

    /** True when the given section should be (re)written in this run. */
    public boolean targets(String candidate) {
        return section == null || section.isBlank()
                || section.equalsIgnoreCase("all")
                || section.equalsIgnoreCase(candidate);
    }
}
