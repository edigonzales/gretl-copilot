package ch.so.agi.interlis.glsp.diagram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.GLabel;
import org.eclipse.glsp.graph.GModelRoot;
import org.eclipse.glsp.graph.GNode;
import org.eclipse.glsp.server.model.DefaultGModelState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterlisDiagramGModelFactoryTest {

    private DefaultGModelState modelState;
    private InterlisDiagramGModelFactory factory;

    @BeforeEach
    void setUp() {
        modelState = new DefaultGModelState();
        modelState.init();
        factory = new InterlisDiagramGModelFactory(modelState, new InterlisDiagramStyling());
    }

    @Test
    void createGModel_buildsPlaceholderDiagram() {
        factory.createGModel();

        GModelRoot root = modelState.getRoot();
        assertNotNull(root, "The placeholder GModel root should be created");
        GGraph graph = assertInstanceOf(GGraph.class, root);
        assertEquals("interlis-class-diagram", graph.getId());
        assertEquals(1, graph.getChildren().size(), "Exactly one placeholder node is expected");

        GNode node = assertInstanceOf(GNode.class, graph.getChildren().get(0));
        assertEquals("interlis-placeholder", node.getId());
        assertEquals(1, node.getChildren().size(), "The placeholder node should contain a label child");

        GLabel label = assertInstanceOf(GLabel.class, node.getChildren().get(0));
        assertEquals("INTERLIS Model", label.getText());
    }
}
