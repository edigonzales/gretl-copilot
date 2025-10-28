package ch.so.agi.gretl.copilot.chat;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void rendersTablesHeadingsListsAndInlineCode() {
        String markdown = """
                ## Überblick

                | Property | Typ | Pflicht | Standardwert | Beschreibung |
                | --- | --- | --- | --- | --- |
                | dataFiles | FileCollection | Nein | – | Zu validierende INTERLIS-Datei(en), z. B. eine XTF-Datei. |
                | logFile | File | Nein | – | Datei für das Validator-Protokoll. |
                | allObjectsAccessible | Boolean | Nein | – | Option zur Steuerung der Zugänglichkeit aller Objekte während der Prüfung. |

                * Unterstützt Listenpunkte
                * Und weitere Elemente

                Eingebetteter `Code` innerhalb eines Absatzes.
                """;

        String html = renderer.render(markdown);

        assertAll(
                () -> assertTrue(html.contains("<h2>Überblick</h2>"), "Heading should be rendered"),
                () -> assertTrue(html.contains("<table>"), "Table should be rendered"),
                () -> assertTrue(html.contains("<th>Property</th>"), "Table headers should be rendered"),
                () -> assertTrue(html.contains("<td>FileCollection</td>"), "Table cells should be rendered"),
                () -> assertTrue(html.contains("<li>Unterstützt Listenpunkte</li>"), "List items should be rendered"),
                () -> assertTrue(html.contains("<code>Code</code>"), "Inline code should be rendered"));
    }
}
