package ai.agentic.samples.course.model;

import java.util.List;

/**
 * A quiz: the typed result of an LLM query. Using a record (instead of parsing
 * a raw String) lets the agent ask for {@code Quiz.class} and receive structured
 * data straight from Jakarta JSON Binding.
 *
 * @param questions the quiz questions
 */
public record Quiz(List<QuizQuestion> questions) {
}
