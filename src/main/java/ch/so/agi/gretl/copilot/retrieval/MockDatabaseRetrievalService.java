package ch.so.agi.gretl.copilot.retrieval;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import ch.so.agi.gretl.copilot.intent.IntentClassification;

@Component
@ConditionalOnMissingBean(DatabaseRetrievalService.class)
public class MockDatabaseRetrievalService implements RetrievalService {

    @Override
    public RetrievalResult retrieve(String userMessage, IntentClassification classification) {
        RetrievedDocument introduction = new RetrievedDocument(
                "documentation",
                "Mocked database result",
                "Beispielantwort aus dem Mock-Retrieval: Liefert statische Dokumente für Entwicklungszwecke.",
                "https://github.com/sogis/gretl",
                0.85);

        RetrievedDocument reference = new RetrievedDocument(
                "reference",
                "Konfigurationsbeispiel",
                "Beispiel: Verwende gretl.tasks.Ili2pgImport für einen PostGIS Import (Mock).",
                "https://github.com/sogis/gretl/tree/master/examples",
                0.74);

        return new RetrievalResult(List.of(introduction, reference));
    }
}