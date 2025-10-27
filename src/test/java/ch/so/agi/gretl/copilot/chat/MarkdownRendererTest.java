package ch.so.agi.gretl.copilot.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void rendersTablesAndOtherMarkdownElements() {
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

        assertThat(html).contains("<h2>Überblick</h2>");
        assertThat(html).contains("<table");
        assertThat(html).contains("<th>Property</th>");
        assertThat(html).contains("<td>FileCollection</td>");
        assertThat(html).contains("<ul>");
        assertThat(html).contains("<code>Code</code>");
    }
}
