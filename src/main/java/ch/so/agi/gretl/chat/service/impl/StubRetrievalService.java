package ch.so.agi.gretl.chat.service.impl;

import ch.so.agi.gretl.chat.model.RagContext;
import ch.so.agi.gretl.chat.service.RetrievalService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StubRetrievalService implements RetrievalService {

    @Override
    public RagContext retrieve(String intentLabel, String prompt) {
        return switch (intentLabel) {
            case "import.interlis.postgis" -> new RagContext(intentLabel, List.of(
                "Task: importInterlis2Postgis - wraps ili2pg and handles schema creation",
                "Example: tasks/import/postgis_import.gradle",
                "Property: dbSchema (default: INTERLIS model name)"
            ));
            case "design.workflow" -> new RagContext(intentLabel, List.of(
                "Task: workflow - compose GRETL tasks",
                "Example: examples/workflows/designing_etl.gradle"
            ));
            default -> new RagContext(intentLabel, List.of(
                "Task: help - general guidance on GRETL tasks",
                "Example: docs/getting-started/overview.md"
            ));
        };
    }
}
