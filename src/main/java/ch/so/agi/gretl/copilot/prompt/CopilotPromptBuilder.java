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
            ## Beschreibung
            - Kurze Zusammenfassung der empfohlenen GRETL-Task oder des Vorgehens.
            - Wenn Angaben fehlen, freundlich nach fehlenden Informationen fragen.

            ## Pflicht-Properties
            - Falls keine Pflicht-Properties notwendig sind: schreibe „_Keine._“
            - Andernfalls eine Markdown-Tabelle mit den Spalten: | Property | Typ | Pflicht | Standardwert | Beschreibung |

            ## Optionale Properties
            - Falls keine optionalen Properties verwendet werden: schreibe „_Keine._“
            - Ansonsten die gleiche Tabellenstruktur wie oben und knappe Beschreibungen.

            ## Beispiel-Task
            - Wenn ein Beispiel sinnvoll ist, gib einen vollständigen Gradle-Task in einem ```gradle```-Codeblock an.
            - Wenn noch Informationen fehlen, erkläre stattdessen, welche Angaben benötigt werden.

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
        builder.append("## Benutzeranfrage\n");
        builder.append(userMessage == null ? "" : userMessage.trim());
        builder.append("\n\n");

        if (classification != null) {
            builder.append("## Klassifikation\n");
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

        builder.append("## Wissensdatenbank-Treffer\n");
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

        builder.append("## Ausgabehinweis\n");
        builder.append(
                "Formatiere die Antwort exakt wie im Systemprompt beschrieben und verwende die Kontextquellen als Grundlage.\n");

        return builder.toString();
    }

    private static FewShotExample validierungExample() {
        IntentClassification classification = new IntentClassification("task.ili2pgvalidate", 0.91,
                "Schlüsselwörter zu ili2pg Validate erkannt.");

        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tasks", "Ili2pgValidate", "Validiert INTERLIS-Transferdateien gegen ein Modell.",
                        "https://gretl.app/reference.html#Ili2pgValidate", 0.89),
                new RetrievedDocument("Properties", "Pflichtparameter", "dataset und models sind zwingend anzugeben.",
                        "https://gretl.app/reference.html#Ili2pgValidate-properties", 0.72));

        String assistant = """
                ## Beschreibung
                Verwende die Task `ch.so.agi.gretl.tasks.Ili2pgValidate`, um die INTERLIS-Datei gegen das angegebene Modell zu prüfen und einen Validierungsbericht zu erzeugen.

                ## Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | dataset | Path | Ja | – | Pfad zur zu validierenden INTERLIS-Transferdatei (`.xtf`). |
                | models | String | Ja | – | Modellnamen oder Pfad zur Modellablage (z. B. `SO_AFU_Liegenschaften_2020`). |

                ## Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | logFile | Path | Nein | – | Datei für den ausführlichen Validierungsreport. |
                | failOnError | Boolean | Nein | true | Stoppt die Pipeline bei Validierungsfehlern sofort. |
                | configFile | Path | Nein | – | Optionale iliValidator-Konfigurationsdatei. |

                ## Beispiel-Task
                ```gradle
                task validateParzellen(type: ch.so.agi.gretl.tasks.Ili2pgValidate) {
                    dataset = file("data/parzellen.xtf")
                    models = "SO_AFU_Liegenschaften_2020"
                    logFile = file("${buildDir}/reports/validation.log")
                }
                ```
                """;

        return new FewShotExample("Validierung", "Ich muss ein neues INTERLIS-Transferfile vor dem Import prüfen.",
                classification, docs, assistant);
    }

    private static FewShotExample csvToPostgresExample() {
        IntentClassification classification = new IntentClassification("task.csvimport", 0.88,
                "Frage erwähnt CSV und PostGIS-Zieltabelle.");

        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tasks", "CsvImport", "Importiert strukturierte CSV-Dateien in eine PostGIS-Tabelle.",
                        "https://gretl.app/reference.html#CsvImport", 0.84),
                new RetrievedDocument("Properties", "Schema und Tabelle",
                        "schemaName und tableName definieren das Ziel in der Datenbank.",
                        "https://gretl.app/reference.html#CsvImport-properties", 0.71));

        String assistant = """
                ## Beschreibung
                Nutze die Task `ch.so.agi.gretl.tasks.CsvImport`, um die CSV-Datei direkt in die gewünschte PostGIS-Tabelle zu schreiben.

                ## Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | database | String | Ja | – | JDBC-Verbindungszeichenfolge inkl. Benutzer und Passwort. |
                | schemaName | String | Ja | – | Schema, in das die Daten importiert werden sollen. |
                | tableName | String | Ja | – | Ziel-Tabelle für den Import. |
                | file | Path | Ja | – | Pfad zur CSV-Datei. |

                ## Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | delimiter | String | Nein | ; | Feldtrenner der CSV-Datei. |
                | quoteChar | String | Nein | " | Zeichen für Text-Kapselung. |
                | srid | Integer | Nein | 2056 | SRID für allfällige Geometriespalten. |

                ## Beispiel-Task
                ```gradle
                task importBodenbedeckung(type: ch.so.agi.gretl.tasks.CsvImport) {
                    database = "jdbc:postgresql://localhost:5432/gretl?user=gretl&password=secret"
                    schemaName = "boden"
                    tableName = "bodenbedeckung"
                    file = file("data/bodenbedeckung.csv")
                    delimiter = ","
                }
                ```
                """;

        return new FewShotExample("CSV→Postgres", "Wie bringe ich eine CSV-Datei in eine bestehende PostGIS-Tabelle?",
                classification, docs, assistant);
    }

    private static FewShotExample sqlExecutionExample() {
        IntentClassification classification = new IntentClassification("task.sqlexecutor", 0.86,
                "Stichworte zu Skript-Ausführung erkannt.");

        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tasks", "SqlExecutor",
                        "Führt SQL-Skripte oder Statements gegen eine konfigurierte Datenbank aus.",
                        "https://gretl.app/reference.html#SqlExecutor", 0.81));

        String assistant = """
                ## Beschreibung
                Setze `ch.so.agi.gretl.tasks.SqlExecutor` ein, um das bereitgestellte SQL-Skript innerhalb der GRETL-Pipeline auszuführen.

                ## Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | database | String | Ja | – | JDBC-URL inklusive Zugangsdaten. |
                | sqlFile | Path | Ja | – | Pfad zum SQL-Skript, das ausgeführt werden soll. |

                ## Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | autocommit | Boolean | Nein | false | Führt jedes Statement in einer eigenen Transaktion aus. |
                | stopOnError | Boolean | Nein | true | Bricht bei Fehlern sofort ab. |

                ## Beispiel-Task
                ```gradle
                task runCleanup(type: ch.so.agi.gretl.tasks.SqlExecutor) {
                    database = "jdbc:postgresql://localhost:5432/gretl?user=gretl&password=secret"
                    sqlFile = file("sql/cleanup.sql")
                    stopOnError = true
                }
                ```
                """;

        return new FewShotExample("SQL-Ausführung", "Ich möchte ein SQL-Skript am Ende der Pipeline laufen lassen.",
                classification, docs, assistant);
    }

    private static FewShotExample exportExample() {
        IntentClassification classification = new IntentClassification("task.ili2pgexport", 0.89,
                "Begriffe zu INTERLIS-Export erkannt.");

        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tasks", "Ili2pgExport",
                        "Exportiert Daten aus PostGIS in ein INTERLIS-Transferfile.",
                        "https://gretl.app/reference.html#Ili2pgExport", 0.83));

        String assistant = """
                ## Beschreibung
                Verwende `ch.so.agi.gretl.tasks.Ili2pgExport`, um Daten aus der PostGIS-Tabelle wieder als INTERLIS-Datei auszugeben.

                ## Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | database | String | Ja | – | JDBC-Verbindung zur Quell-Datenbank. |
                | dataset | Path | Ja | – | Pfad zur Ziel-XTF-Datei, die geschrieben werden soll. |
                | models | String | Ja | – | Modell(e), für die exportiert werden soll. |

                ## Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | schema | String | Nein | public | Schema, aus dem exportiert wird. |
                | disableValidation | Boolean | Nein | false | Überspringt die abschließende Validierung. |
                | additionalSettings | Map | Nein | – | Weitere ili2pg-Schalter im Schlüssel/Wert-Format. |

                ## Beispiel-Task
                ```gradle
                task exportParzellen(type: ch.so.agi.gretl.tasks.Ili2pgExport) {
                    database = "jdbc:postgresql://localhost:5432/gretl?user=gretl&password=secret"
                    schema = "parzellen"
                    dataset = file("build/export/parzellen.xtf")
                    models = "SO_AFU_Liegenschaften_2020"
                }
                ```
                """;

        return new FewShotExample("Export", "Wir müssen nach dem Import wieder ein INTERLIS-File erzeugen.", classification,
                docs, assistant);
    }

    private static FewShotExample iliValidatorExample() {
        IntentClassification classification = new IntentClassification("task.ilivalidator", 0.9,
                "Explizite Nachfrage nach iliValidator.");

        List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Tools", "IliValidator Integration",
                        "Beschreibt die Verwendung des standalone IliValidators innerhalb von GRETL.",
                        "https://gretl.app/reference.html#IliValidator", 0.78));

        String assistant = """
                ## Beschreibung
                Nutze die Task `ch.so.agi.gretl.tasks.IliValidator`, um den offiziellen iliValidator mit benutzerdefiniertem Konfigurationsfile auszuführen.

                ## Pflicht-Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | dataset | Path | Ja | – | Zu prüfendes Transferfile. |
                | configFile | Path | Ja | – | iliValidator-Konfiguration, z. B. mit Modelldirectory und Checks. |

                ## Optionale Properties
                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | logFile | Path | Nein | – | Ausgabe des Validators als Protokolldatei. |
                | failOnError | Boolean | Nein | true | Liefert einen Build-Fehler bei gefundenen Fehlern. |

                ## Beispiel-Task
                ```gradle
                task validateMitConfig(type: ch.so.agi.gretl.tasks.IliValidator) {
                    dataset = file("data/ligdi.xtf")
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
