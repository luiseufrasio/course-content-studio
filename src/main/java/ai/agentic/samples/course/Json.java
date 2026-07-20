package ai.agentic.samples.course;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * A tiny shared {@link Jsonb} instance. {@code Jsonb} is thread-safe and
 * expensive to build, so we keep a single application-wide instance for
 * serializing the current packet (for the refine round-trip and REST responses).
 */
public final class Json {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private Json() {
    }

    public static Jsonb instance() {
        return JSONB;
    }
}
