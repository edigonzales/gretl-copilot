package ch.so.agi.interlis.glsp.diagram;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.glsp.server.actions.SaveModelAction;
import org.eclipse.glsp.server.features.core.model.ModelSubmissionHandler;
import org.eclipse.glsp.server.features.core.model.RequestModelAction;
import org.eclipse.glsp.server.features.core.model.SourceModelStorage;
import org.eclipse.glsp.server.model.GModelState;

/**
 * Loads INTERLIS source files whenever the GLSP client requests a model.
 * <p>
 * The storage forwards the raw source to {@link ModelSubmissionHandler}. In the
 * current iteration the submission handler is only used to trigger the
 * placeholder rectangle, but the wiring is ready to compile the source using
 * the ili2c helper in the next step.
 */
public class InterlisSourceModelStorage implements SourceModelStorage {

    public static final String OPTION_SOURCE_URI = "sourceUri";
    private static final Logger LOGGER = LogManager.getLogger(InterlisSourceModelStorage.class);

    private final ModelSubmissionHandler modelSubmissionHandler;
    private final GModelState modelState;

    @Inject
    public InterlisSourceModelStorage(ModelSubmissionHandler modelSubmissionHandler, GModelState modelState) {
        this.modelSubmissionHandler = modelSubmissionHandler;
        this.modelState = modelState;
    }

    @Override
    public void loadSourceModel(RequestModelAction action) {
        Map<String, String> options = Optional.ofNullable(action.getOptions()).orElse(Map.of());
        String sourceUri = options.get(OPTION_SOURCE_URI);
        LOGGER.debug("Loading INTERLIS source from {}", sourceUri);

        String source = readSource(sourceUri);
        modelState.setProperty(OPTION_SOURCE_URI, sourceUri);
        modelSubmissionHandler.submitModelDirectly(source);
    }

    @Override
    public void saveSourceModel(SaveModelAction action) {
        LOGGER.info("Ignoring save request because the diagram is read-only in phase 1");
    }

    private String readSource(String sourceUri) {
        if (sourceUri == null || sourceUri.isBlank()) {
            return "";
        }
        try {
            Path path = toPath(sourceUri);
            if (!Files.exists(path)) {
                LOGGER.warn("Source file {} does not exist", sourceUri);
                return "";
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.warn("Failed to read INTERLIS source {}: {}", sourceUri, ex.getMessage());
            return "";
        }
    }

    private Path toPath(String sourceUri) {
        try {
            URI uri = URI.create(sourceUri);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return Paths.get(uri);
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.debug("Source URI is not a valid URI, treat it as path: {}", sourceUri);
        }
        return Paths.get(sourceUri);
    }
}
