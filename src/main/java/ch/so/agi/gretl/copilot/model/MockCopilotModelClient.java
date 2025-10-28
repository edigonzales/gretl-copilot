package ch.so.agi.gretl.copilot.model;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(CopilotModelClient.class)
public class MockCopilotModelClient implements CopilotModelClient {

    @Override
    public List<CopilotStreamSegment> generateResponse(CopilotPrompt prompt) {
        String explanation = """
                ## Beschreibung
                Beispielantwort aus dem Mock-Modell: Importiert ein INTERLIS-Transferfile nach PostGIS inklusive kurzer Erläuterung.

                ## Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | dataset | Path | Ja | – | Pfad zum INTERLIS-Transferfile. |
                | dbUrl | String | Ja | – | JDBC-Verbindungszeichenfolge der Ziel-Datenbank. |

                ## Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | schema | String | Nein | public | Ziel-Schema für den Import. |
                | createSqlLog | Boolean | Nein | false | Schreibt die generierten SQL-Befehle in eine Logdatei. |

                ## Beispiel-Task
                Siehe Codeblock unten.
                """;

        String buildGradle = """
                task importInterlis(type: ch.so.agi.gretl.tasks.Ili2pgImport) {
                    dataset = file("data/example.xtf")
                    dbUrl = "jdbc:postgresql://localhost:5432/gretl"
                    schema = "interlis_import"
                    createSqlLog = true
                }
                """;

        CopilotStreamSegment textSegment = new CopilotStreamSegment(CopilotStreamSegment.SegmentType.TEXT,
                explanation.strip());

        CopilotStreamSegment codeSegment = new CopilotStreamSegment(CopilotStreamSegment.SegmentType.CODE_BLOCK,
                buildGradle.strip());

        CopilotStreamSegment linksSegment = new CopilotStreamSegment(CopilotStreamSegment.SegmentType.LINKS, "");

        return List.of(textSegment, codeSegment, linksSegment);
    }
}
