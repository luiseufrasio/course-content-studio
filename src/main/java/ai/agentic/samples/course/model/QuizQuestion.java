package ai.agentic.samples.course.model;

import java.util.List;

/**
 * A quiz question. Two shapes, distinguished by {@link #type()}:
 * <ul>
 *   <li>{@code "multiple_choice"} — uses {@link #options()},
 *       {@link #correctIndex()} and {@link #explanation()};</li>
 *   <li>{@code "open"} — a subjective question the student answers in free text;
 *       {@link #sampleAnswer()} holds the model answer used to grade it.</li>
 * </ul>
 * Deserialized from the LLM response by Jakarta JSON Binding.
 *
 * @param prompt       the question text
 * @param type         {@code "multiple_choice"} or {@code "open"}
 * @param options      MCQ answer choices (typically four); empty for open
 * @param correctIndex MCQ zero-based index of the correct option; -1 for open
 * @param explanation  MCQ rationale for the correct option
 * @param sampleAnswer open-question model answer (used for LLM grading)
 */
public record QuizQuestion(
        String prompt,
        String type,
        List<String> options,
        int correctIndex,
        String explanation,
        String sampleAnswer) {

    public static final String MULTIPLE_CHOICE = "multiple_choice";
    public static final String OPEN = "open";

    public boolean isOpen() {
        return OPEN.equalsIgnoreCase(type);
    }
}
