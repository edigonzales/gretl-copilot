## Was sind sind few shots examples?

### Überblick

Few-shot examples sind kuratierte Beispiel-Dialoge, die dem Sprachmodell vor der eigentlichen Nutzeranfrage mitgegeben werden. Sie demonstrieren, wie unterschiedliche Anfragearten klassifiziert werden, welche Kontextdokumente zur Verfügung stehen und wie eine ideal strukturierte Antwort aussieht. Dadurch lernt das Modell implizit das gewünschte Antwortformat und kann neue, ähnliche Anfragen zuverlässiger bearbeiten.
Aufbau eines Few-shot-Beispiels

Jedes Beispiel enthält:

- Kategorie und Nutzerfrage: Repräsentiert eine typische Aufgabe, z. B. Validierung oder CSV→Postgres-Import.
- Intent-Klassifikation: Zeigt dem Modell das erwartete Intent-Label inklusive Konfidenz und Begründung, sodass es die spätere Nutzereingabe analog einordnet.
- Wissensdokumente: Liefert Beispielmetadaten zu gefundenen Referenzen, damit das Modell lernt, wie Dokumenttreffer zu zitieren sind.
- Assistant-Antwort: Eine vollständig ausgearbeitete Antwort mit vorgeschriebenen Abschnitten (Beschreibung, Pflicht- und optionale Properties, Beispiel-Task), die das gewünschte Format demonstriert. 

### Verfügbare Few-shot-Kategorien

1. Validierung: Erklärt, wie eine INTERLIS-Datei mit Ili2pgValidate geprüft wird, inklusive erforderlicher Parameter und Beispiel-Gradle-Task.
2. CSV→Postgres: Zeigt einen Import mit CsvImport, hebt Datenbank- und Tabellenangaben hervor und illustriert optionale CSV-Parameter.
3. SQL-Ausführung: Beschreibt die Verwendung von SqlExecutor, um Skripte innerhalb der Pipeline auszuführen.
4. Export: Demonstriert Ili2pgExport für den INTERLIS-Export aus einer PostGIS-Datenbank.
5. IliValidator: Verdeutlicht den Einsatz des Standalone-Validators mit eigener Konfiguration und Log-Ausgabe.

### Nutzen für neue Anfragen

Durch diese Beispiele versteht das Modell, wie es:

- die Anfrage korrekt klassifizieren und die richtige Intent-Bezeichnung wählen soll,
- Kontextdokumente strukturiert referenziert,
- und vor allem, wie die finalen Antworten formal aufgebaut sein müssen (Abschnitte, Tabellen, Codeblöcke).

So bleibt die Antwortqualität konsistent, auch wenn die eigentliche Nutzerfrage neue Details enthält.

## Can you explain me why there is the IntentClassifier step? It's just looking at the rag.task_examples table. why not at doc_chunks?

### Hintergrund

Der Intent-Schritt ist als eigenständiger Baustein in der Pipeline dokumentiert: Nach dem Speichern der neuen Chat-Nachricht wird classify(userMessage) aufgerufen, die gefundenen Intent-Hinweise fließen anschließend in die Retrieval-Schicht und in den Prompt, bevor die Antwort generiert wird.

### Zweck des IntentClassifier

DatabaseIntentClassifier vergleicht die Nutzerfrage mit einer kuratierten Tabelle rag.task_examples. Diese Beispiele sind kurze, handverlesene Beschreibungen typischer GRETL-Aufgaben, die bereits mit den gewünschten Task-Namen verknüpft sind. Die Klassifizierung liefert damit ein stabiles Label (z. B. task.csvimport), eine Konfidenz sowie eine Begründung und erlaubt konfigurierbare Fallbacks, wenn keine ausreichende Übereinstimmung gefunden wird.

### Warum rag.task_examples statt doc_chunks?

Die Tabelle rag.doc_chunks enthält lange, heterogene Dokumentabschnitte – sie ist für die Kontextsuche zuständig. Die Retrieval-Schicht kombiniert dort Vektor- und Volltextsuche, kann zusätzlich ein Reranking durchführen und liefert die konkreten Absätze für den Prompt zurück.

Für die Intent-Erkennung wäre diese Menge jedoch zu breit und zu unstrukturiert: Viele Chunks behandeln Teilaspekte desselben Tasks, andere beschreiben mehrere Befehle zugleich. Ein dediziertes Few-shot-Set in rag.task_examples bietet deutlich klarere Signale pro Intent, erleichtert das Tuning (z. B. top-k, Mindestkonfidenz, Fallback-Label) und stellt sicher, dass das Modell immer einen wohldefinierten Task-Namen mit kurzer, erklärender Begründung erhält.

### Zusammenspiel mit doc_chunks

Sobald der Intent feststeht, nutzt RetrievalService die gleiche Nutzerfrage – kombiniert mit dem Intent-Hinweis – um relevante doc_chunks zu holen. Dadurch bekommt das Sprachmodell nicht nur die korrekte Aufgabenklasse, sondern auch die passenden Text- oder Codeabschnitte für die Antwort. Der Intent-Schritt ist also eine vorgeschaltete, grobgranulare Klassifizierung, die dem nachgelagerten, feineren Dokument-Retrieval Orientierung gibt.

##  Warum werden beim Intent nur 5 Resultate gesucht wohingegen bei der normalen retrieval Anfrage zuerst ingesamt 60.

## Hintergrund

Der Intent-Schritt greift auf die Eigenschaft gretl.copilot.intent.top-k zurück, die in IntentClassifierProperties standardmäßig auf 5 gesetzt ist. Diese Zahl bestimmt, wie viele Einträge aus rag.task_examples geholt werden, bevor das beste Label berechnet wird.

### Intent-spezifische Suche (Top 5)

Für die Intentklassifikation reicht ein sehr kleiner Kandidatenpool: Die Tabelle rag.task_examples enthält kuratierte, bewusst kompakte Beispiele, die jede Task-Klasse eindeutig beschreiben. Fünf Treffer decken hier in der Regel alle relevanten Varianten ab, verkürzen die Anfragezeit und verhindern, dass schwache Ähnlichkeiten den Confidence-Wert verwässern.

### Dokument-Retrieval (Top 60)

Die nachgelagerte Retrieval-Schicht arbeitet mit doc_chunks, also einer viel größeren und heterogeneren Wissensbasis. Darum zieht DatabaseRetrievalService zunächst 60 Kandidaten (candidateLimit = 60) aus der Datenbank, bevor Reranking und finale Auswahl stattfinden. Dieser größere Puffer stellt sicher, dass selbst seltene oder breit gestellte Fragen genügend Material liefern, das anschließend durch Hybrid-Scoring und optionales Cross-Encoder-Reranking gefiltert wird.

### Warum der Unterschied?

Intentklassifikation ist ein grobgranularer Schritt mit streng kuratierten Beispielen; hier genügt eine kleine, hochwertige Auswahl. Das Dokument-Retrieval dagegen muss aus einem umfangreichen Korpus alle potenziell relevanten Absätze einsammeln, weshalb eine deutlich höhere Obergrenze gewählt wurde. Beide Grenzwerte sind konfigurierbar: Du kannst gretl.copilot.intent.top-k bzw. gretl.copilot.retrieval.candidate-limit in den Anwendungseigenschaften anpassen, falls Du ein anderes Verhältnis testen möchtest.

## Es reicht meines Erachtens nicht nur einen Intent zu berücksichtigen. Wenn man z.B. schreibt man will XTF validieren und in eine Postgis-Datebank importieren, liefert der Intentschritt ja nur genau ein Resultat zurück. Dieses wird dann der Usermessage angehägt und anschliessend wird der retrieval Schritt ausgeführt. Was bringt mir hier noch der Intentschritt?

### Beschreibung

Der Intent-Schritt läuft zwingend vor jedem Retrieval an: Der ChatService holt zunächst die Klassifikation zur letzten Nutzeräußerung, übergibt das Ergebnis zusammen mit den Retrieval-Dokumenten an den Prompt und startet erst dann das Streaming der Modellantwort.
### Rolle des Intent-Schritts

- Explizite Aufgabenmarkierung im Prompt: Der CopilotPromptBuilder bettet das erkannte Label, die Konfidenz und die Begründung in die Nutzersektion ein. Dadurch bekommt das Sprachmodell einen klaren Hinweis, welche Aufgabenschablone es bevorzugt verwenden soll (z. B. Tabellenaufbau, empfohlene Task-Namen), auch wenn die eigentliche Frage mehrere Aspekte nennt.

- Kuratiertes Few-shot-Gewicht: Der DatabaseIntentClassifier wählt aus den kuratierten Beispielen das bestpassende Intent-Label, versieht es mit einer Konfidenz und einer Begründung und fällt andernfalls auf ein generisches Hilfelabel zurück. Das sorgt für konsistente Antworten und erleichtert Debugging sowie Telemetrie, weil man das erkannte Intent samt Score nachvollziehen kann.

### Grenzen und mögliche Erweiterungen

- Single-Label-Entscheidung: Aktuell wird nur das Top-Intent zurückgegeben; eine Mischanfrage (z. B. „XTF validieren und danach importieren“) wird daher auf das dominierende Beispiel abgebildet. Das Retrieval selbst arbeitet aber weiterhin mit der kompletten Nutzerprosa und kann so trotzdem Dokumente zu beiden Aspekten liefern.

- Erweiterung auf Multi-Label: Wenn Du explizit mehrere Intents brauchst, müsste der Klassifizierer erweitert werden, z. B. indem er die Top-N-Kandidaten oberhalb einer Schwelle zurückgibt oder über Thresholding mehrere Labels auswählt und im Prompt markiert. Die aktuelle Implementierung und Konfiguration (Top K=5, Mindestkonfidenz 0.45) bilden dafür bereits die Grundlage, nur die Aggregationslogik wäre anzupassen.

### Fazit

Der Intent-Schritt bringt auch bei Mischanfragen Nutzen, weil er das Sprachmodell auf eine bekannte Antwortschablone kalibriert und transparente Telemetrie liefert. Gleichzeitig bleibt das Retrieval frei, die vollständige Nutzerfrage zu interpretieren. Für echte Multi-Intent-Antworten müsste der Klassifizierer jedoch erweitert werden, damit mehrere Labels in den Prompt einfließen können.