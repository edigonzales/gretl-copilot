package ch.so.agi.gretl.copilot.prompt;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ch.so.agi.gretl.copilot.intent.IntentClassification;
import ch.so.agi.gretl.copilot.model.CopilotPrompt;
import ch.so.agi.gretl.copilot.retrieval.RetrievedDocument;

@Component
public class CopilotPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            Du bist GRETL-Assistent, Experte für Tasks/Properties. Antworte auf Deutsch und benutze nur die gelieferten Informationen.

            Struktur der Antwort (immer in dieser Reihenfolge, auch wenn einzelne Abschnitte leer sind):
            ### Beschreibung
            - Kurze Zusammenfassung der empfohlenen GRETL-Task oder des Vorgehens.
            - Wenn Angaben fehlen, freundlich nach fehlenden Informationen fragen.

            ### Pflicht-Properties
            - Falls keine Pflicht-Properties notwendig sind: schreibe „_Keine._“
            - Andernfalls eine Markdown-Tabelle mit den Spalten: | Property | Typ | Pflicht | Standardwert | Beschreibung |

            ### Optionale Properties
            - Falls keine optionalen Properties verwendet werden: schreibe „_Keine._“
            - Ansonsten die gleiche Tabellenstruktur wie oben und knappe Beschreibungen.

            ### Beispiel-Task
            - Wenn ein Beispiel sinnvoll ist, gib einen vollständigen Gradle-Task in einem ```gradle```-Codeblock an. Verzichte auf ein vollständiges Gradle Build-Script.
            - Wenn noch Informationen fehlen, erkläre stattdessen, welche Angaben benötigt werden.
            - Falls ein Task eine Datenbankverbindung benötigt, schreibe einfach "[dbUrl, dbUser, dbPwd]" als Wert für das Property.

            Fehlerhandling:
            - Fehlen wesentliche Angaben (z. B. Dateipfade, DB-Verbindungsdaten), bitte höflich nachfragen oder auf Limitierungen hinweisen.
            - Keine Annahmen ohne Grundlage treffen und keine nicht belegten URLs erfinden.
            """;

    private static final List<FewShotExample> FEW_SHOTS = List.of(
            validierungExample(), csvToPostgresExample(), sqlExecutionExample(), exportExample(), iliValidatorExample());

    public Prompt build(CopilotPrompt prompt) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        for (FewShotExample example : FEW_SHOTS) {
            messages.add(new UserMessage(renderUserMessage(example.userQuery(), example.classification(),
                    example.documents())));
            messages.add(new AssistantMessage(example.assistantReply()));
        }

        messages.add(new UserMessage(renderUserMessage(prompt.userMessage(), prompt.classification(), prompt.documents())));

        return new Prompt(messages);
    }

    private String renderUserMessage(String userMessage, IntentClassification classification,
            List<RetrievedDocument> documents) {
        StringBuilder builder = new StringBuilder();
        builder.append("### Benutzeranfrage\n");
        builder.append(userMessage == null ? "" : userMessage.trim());
        builder.append("\n\n");

        if (classification != null) {
            builder.append("### Klassifikation\n");
            builder.append("- Label: ").append(classification.label()).append("\n");
            if (classification.confidence() > 0) {
                builder.append("- Confidence: ")
                        .append(NumberFormat.getPercentInstance(Locale.GERMAN).format(classification.confidence()))
                        .append("\n");
            }
            if (StringUtils.hasText(classification.rationale())) {
                builder.append("- Begründung: ").append(classification.rationale().trim()).append("\n");
            }
            builder.append("\n");
        }

        builder.append("### Wissensdatenbank-Treffer\n");
        if (documents == null || documents.isEmpty()) {
            builder.append("Keine passenden Dokumente gefunden. Bitte antworte nach bestem Wissen aus den Richtlinien.\n\n");
        } else {
            int index = 1;
            for (RetrievedDocument doc : documents) {
                builder.append(index++).append(". Kategorie: ").append(doc.category()).append("\n");
                builder.append("   Titel: ").append(doc.title()).append("\n");
                if (StringUtils.hasText(doc.snippet())) {
                    builder.append("   Snippet: ").append(doc.snippet().trim()).append("\n");
                }
                builder.append("   URL: ").append(doc.url()).append("\n\n");
            }
        }

        builder.append("### Ausgabehinweis\n");
        builder.append(
                "Formatiere die Antwort exakt wie im Systemprompt beschrieben und verwende die Kontextquellen als Grundlage.\n");

        return builder.toString();
    }

    private static FewShotExample validierungExample() {
        IntentClassification classification = new IntentClassification("task.ili2pgvalidate", 0.91,
                "Schlüsselwörter zu ili2pg Validate erkannt.", List.of());
        
        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tasks", "Ili2pgValidate", "Validiert Daten in der Datenbank gegen ein Modell.",
                        "https://gretl.app/reference.html#Ili2pgValidate", 0.89),
                new RetrievedDocument("Properties", "Pflichtparameter", "dataset und models sind zwingend anzugeben.",
                        "https://gretl.app/reference.html#Ili2pgValidate-properties", 0.72));

        String assistant = """
                ### Beschreibung
                Verwende den Task `ch.so.agi.gretl.tasks.Ili2pgValidate`, um die Daten in der Datenbank mit ilivalidator gegenüber einem INTERLIS-Datenmodell zu prüfen.

                ### Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | dataset | Path | Ja | – | Pfad zur zu validierenden INTERLIS-Transferdatei (`.xtf`). |

                ### Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | models | String | Ja | – | Modellnamen oder Pfad zur Modellablage (z. B. `SO_AFU_Liegenschaften_2020`). |
                | logFile | Property<File> | Nein | – | Datei für den ausführlichen Validierungsreport. |

                ### Beispiel-Task
                ```gradle
                tasks.register('validateParzellen', Ili2pgValidate) {
                    database = [db_uri, db_user, db_pass]
                    models = "SO_AGI_AV_GB_Administrative_Einteilungen_20180613"
                    modeldir = rootProject.projectDir.toString() + ";http://models.interlis.ch"
                    dbschema = "agi_av_gb_admin_einteilungen_fail"
                    logFile = file("fubar.log")
                }
                ```
                """;

        return new FewShotExample("Validierung", "Ich muss Daten vor dem Export aus der Datenbank prüfen.",
                classification, docs, assistant);
    }

    private static FewShotExample csvToPostgresExample() {
        IntentClassification classification = new IntentClassification("task.csvimport", 0.88,
                "Frage erwähnt CSV und PostGIS-Zieltabelle.", List.of());
                
        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tasks", "CsvImport", "Importiert strukturierte CSV-Dateien in eine PostGIS-Tabelle.",
                        "https://gretl.app/reference.html#CsvImport", 0.84),
                new RetrievedDocument("Properties", "Schema und Tabelle",
                        "schemaName und tableName definieren das Ziel in der Datenbank.",
                        "https://gretl.app/reference.html#CsvImport-properties", 0.71));

        String assistant = """
                ### Beschreibung
                Nutze die Task `ch.so.agi.gretl.tasks.CsvImport`, um die CSV-Datei direkt in die gewünschte PostGIS-Tabelle zu schreiben.

                ### Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | database | ListProperty<String> | Ja | – | JDBC-URL inklusive Zugangsdaten. |
                | schemaName | String | Nein | – | Schema, in das die Daten importiert werden sollen. |
                | tableName | String | Ja | – | Ziel-Tabelle für den Import. |
                | file | Path | Ja | – | Pfad zur CSV-Datei. |

                ### Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |                
                | delimiter | String | Nein | ; | Feldtrenner der CSV-Datei. |
                | quoteChar | String | Nein | " | Zeichen für Text-Kapselung. |
                | schemaName | String | Nein | – | Schema, in das die Daten importiert werden sollen. |

                ### Beispiel-Task
                ```gradle
                tasks.register('importBodenbedeckung', CsvImport) {
                    database = [db_uri, db_user, db_pass]
                    schemaName = "boden"
                    tableName = "bodenbedeckung"
                    firstLineIsHeader = true
                    dataFile = file("data/bodenbedeckung.csv")
                    delimiter = ","
                }
                ```
                """;

        return new FewShotExample("CSV→Postgres", "Wie bringe ich eine CSV-Datei in eine bestehende PostGIS/PostgreSQL-Tabelle?",
                classification, docs, assistant);
    }

    private static FewShotExample sqlExecutionExample() {
        IntentClassification classification = new IntentClassification("task.sqlexecutor", 0.86,
                "Stichworte zu Skript-Ausführung erkannt.", List.of());
        
        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tasks", "SqlExecutor",
                        "Führt SQL-Skripte oder Statements gegen eine konfigurierte Datenbank aus.",
                        "https://gretl.app/reference.html#SqlExecutor", 0.81));

        String assistant = """
                ### Beschreibung
                Setze `ch.so.agi.gretl.tasks.SqlExecutor` ein, um das bereitgestellte SQL-Skript innerhalb der Datenbank auszuführen.

                ### Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |                
                | database | ListProperty<String> | Ja | – | JDBC-URL inklusive Zugangsdaten. |
                | sqlFile | Property<FileCollection> | Ja | – | SQL-Skript-Dateien, die ausgeführt werden soll. |

                ### Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | sqlParameters | Property<Object> | Nein | - | Eine Map mit Paaren von Parameter-Name und Parameter-Wert (Map<String,String>). Oder eine Liste mit Paaren von Parameter-Name und Parameter-Wert (List<Map<String,String>>). |

                ### Beispiel-Task
                ```gradle
                tasks.register('runCleanup', SqlExecutor) {
                    database = [db_uri, db_user, db_pass]
                    sqlFile = file("sql/cleanup.sql")
                }
                ```
                """;


        return new FewShotExample("SQL-Ausführung", "Ich möchte ein SQL-Skript in deiner Datenbank laufen lassen.",
                classification, docs, assistant);
    }

    private static FewShotExample exportExample() {        
        IntentClassification classification = new IntentClassification("task.ili2pgexport", 0.89,
                "Begriffe zu INTERLIS-Export erkannt.", List.of());
        

        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tasks", "Ili2pgExport",
                        "Exportiert Daten aus PostGIS in ein INTERLIS-Transferfile.",
                        "https://gretl.app/reference.html#Ili2pgExport", 0.83));

        String assistant = """
                ### Beschreibung
                Verwende `ch.so.agi.gretl.tasks.Ili2pgExport`, um Daten aus der PostGIS/PostgreSQL-Tabelle wieder als INTERLIS-Datei auszugeben.

                ### Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | database | ListProperty<String> | Ja | – | JDBC-Verbindung zur Quell-Datenbank. |

                ### Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | schema | String | Nein | public | Schema, aus dem exportiert wird. |
                | disableValidation | Boolean | Nein | false | Überspringt die abschließende Validierung. |
                | dataset | Property<Object> | Nein | – | Pfad zur Ziel-XTF-Datei, die geschrieben werden soll. |
                | models | String | Nein | – | Modell(e), für die exportiert werden soll. |


                ### Beispiel-Task
                ```gradle
                tasks.register('exportParzellen', Ili2pgExport) {
                    database = [db_uri, db_user, db_pass]
                    dataFile = files("254900-out.xtf","255000-out.xtf")
                    dataset = ["254900","255000"]
                    logFile = file("ili2pg.log")                }
                ```
                """;

        return new FewShotExample("Export", "Wir müssen nach dem Import wieder ein INTERLIS-File erzeugen.", classification,
                docs, assistant);
    }

    private static FewShotExample iliValidatorExample() {        
        IntentClassification classification = new IntentClassification("task.ilivalidator", 0.9,
                "Explizite Nachfrage nach iliValidator.", List.of());
        

        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tools", "IliValidator Integration",
                        "Beschreibt die Verwendung des standalone IliValidators innerhalb von GRETL.",
                        "https://gretl.app/reference.html#IliValidator", 0.78));

        String assistant = """
                ### Beschreibung
                Nutze den Task `ch.so.agi.gretl.tasks.IliValidator`, um den offiziellen iliValidator mit benutzerdefiniertem Konfigurationsfile auszuführen.

                ### Pflicht-Properties
                _Keine._

                ### Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | dataFiles | FileCollection | Nein | – | Liste der Dateien, die validiert werden sollen. Eine leere Liste ist kein Fehler. |
                | configFile | Object | Nein | – | Konfiguriert die Datenprüfung mit Hilfe einer ini-Datei (um z.B. die Prüfung von einzelnen Constraints auszuschalten). Siehe https://github.com/claeis/ilivalidator/blob/master/docs/ilivalidator.rst#konfiguration. File, falls eine lokale Datei verwendet wird. String, falls eine Datei aus einem Daten-Repository verwendet wird. |
                | logFile | Path | Nein | – | Ausgabe des Validators als Protokolldatei. |
                | failOnError | Boolean | Nein | true | Steuert, ob der Task bei einem Validierungsfehler fehlschlägt. |

                ### Beispiel-Task
                ```gradle
                tasks.register('validateMitConfig', IliValidator) {
                    dataFiles = files("Beispiel2a.xtf")
                    configFile = file("config/validator.ini")
                    logFile = file("${buildDir}/logs/validator.log")
                }
                ```
                """;

        return new FewShotExample("IliValidator",
                "Kann ich den standalone iliValidator in GRETL aufrufen, inklusive eigener Konfiguration?", classification, docs,
                assistant);
    }

    private record FewShotExample(String category, String userQuery, IntentClassification classification,
            List<RetrievedDocument> documents, String assistantReply) {
    }
}
