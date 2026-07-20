package ai.agentic.samples.course.model;

import java.util.List;

/**
 * A single multiple-choice question. Deserialized from the LLM response by
 * Jakarta JSON Binding via {@code LargeLanguageModel.query(prompt, Quiz.class)}.
 *
 * @param prompt       the question text
 * @param options      the answer choices (typically four)
 * @param correctIndex zero-based index of the correct option
 * @param explanation  why the correct option is right
 */
public record QuizQuestion(
        String prompt,
        List<String> options,
        int correctIndex,
        String explanation) {
}
