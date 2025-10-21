package ch.so.agi.gretl.copilot.retrieval;

import java.util.List;

import org.springframework.stereotype.Component;

import ch.so.agi.gretl.copilot.intent.IntentClassification;

@Component
public class MockRetrievalService implements RetrievalService {
    @Override
    public RetrievalResult retrieve(String userMessage, IntentClassification classification) {
        List<RetrievedDocument> documents = List.of(
                new RetrievedDocument("Task", "tasks/interlis/importPostgis",
                        "Imports an INTERLIS transfer file (.xtf) into a PostGIS schema using ili2pg.",
                        "https://gretl.app/tasks/interlis/importPostgis", 18.4),
                new RetrievedDocument("Example", "examples/interlis/postgis-import",
                        "Demonstrates downloading an INTERLIS dataset and loading it via ili2pg.",
                        "https://gretl.app/examples/interlis/postgis-import", 11.2),
                new RetrievedDocument("Property", "tasks/interlis/importPostgis#parameters",
                        "Parameters: dataset, modeldir, defaultSrsCode, targetSchema, createSqlLog.",
                        "https://gretl.app/tasks/interlis/importPostgis#parameters", 9.6));
        return new RetrievalResult(documents);
    }
}
