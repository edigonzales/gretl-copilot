package ch.so.agi.interlis.glsp.diagram;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.glsp.server.diagram.BaseDiagramConfiguration;
import org.eclipse.glsp.server.types.EdgeTypeHint;
import org.eclipse.glsp.server.types.ShapeTypeHint;

import ch.so.agi.interlis.glsp.di.InterlisDiagramModule;

/**
 * Describes the INTERLIS UML class diagram type.
 * <p>
 * The configuration exposes the diagram type id used on the server and client
 * side and registers shape hints so that the GLSP runtime knows which nodes are
 * available. New node and edge hints can be added here once the real UML diagram
 * is returned instead of the current placeholder rectangle.
 */
public class InterlisDiagramConfiguration extends BaseDiagramConfiguration {

    public static final String GRAPH_TYPE = "interlis.graph";
    public static final String CLASS_NODE_TYPE = "interlis.class";
    public static final String CLASS_LABEL_TYPE = "interlis.class.label";

    public InterlisDiagramConfiguration() {
        this.diagramType = InterlisDiagramModule.DIAGRAM_TYPE;
    }

    @Override
    public Map<String, EClass> getTypeMappings() {
        return Collections.emptyMap();
    }

    @Override
    public List<ShapeTypeHint> getShapeTypeHints() {
        return List.of(createDefaultShapeTypeHint(CLASS_NODE_TYPE));
    }

    @Override
    public List<EdgeTypeHint> getEdgeTypeHints() {
        return Collections.emptyList();
    }
}
