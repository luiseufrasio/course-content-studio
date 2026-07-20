package ai.agentic.samples.course;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activates JAX-RS under {@code /api}.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
