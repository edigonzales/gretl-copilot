package ch.so.agi.interlis.glsp.diagram;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.glsp.graph.GGraph;
import org.eclipse.glsp.graph.builder.impl.GGraphBuilder;
import org.eclipse.glsp.graph.builder.impl.GLabelBuilder;
import org.eclipse.glsp.graph.builder.impl.GNodeBuilder;
import org.eclipse.glsp.server.features.core.model.GModelFactory;
import org.eclipse.glsp.server.model.GModelState;

/**
 * Creates the visual graph (GModel) for the INTERLIS diagram.
 * <p>
 * The initial implementation always returns a single red rectangle with a label
 * to prove the client/server integration. The class already exposes helper
 * methods that will later translate the compiled INTERLIS model into nodes and
 * edges.
 */
public class InterlisDiagramGModelFactory implements GModelFactory {

    private static final Logger LOGGER = LogManager.getLogger(InterlisDiagramGModelFactory.class);
    private static final String PLACEHOLDER_NODE_ID = "interlis-placeholder";

    private final GModelState modelState;
    private final InterlisDiagramStyling styling;

    @Inject
    public InterlisDiagramGModelFactory(GModelState modelState, InterlisDiagramStyling styling) {
        this.modelState = modelState;
        this.styling = styling;
    }

    @Override
    public void createGModel() {
        LOGGER.debug("Creating placeholder GModel for INTERLIS diagram");
        GGraph root = createPlaceholderGraph();
        modelState.updateRoot(root);
    }

    private GGraph createPlaceholderGraph() {
        GNodeBuilder classNode = new GNodeBuilder(InterlisDiagramConfiguration.CLASS_NODE_TYPE)
                .id(PLACEHOLDER_NODE_ID)
                .layout("vbox")
                .size(240, 140)
                .addCssClass(styling.classNodeCss())
                .add(new GLabelBuilder(InterlisDiagramConfiguration.CLASS_LABEL_TYPE)
                        .id(PLACEHOLDER_NODE_ID + "-label")
                        .text("INTERLIS Model")
                        .addCssClass(styling.classLabelCss())
                        .build());

        return new GGraphBuilder(InterlisDiagramConfiguration.GRAPH_TYPE)
                .id("interlis-class-diagram")
                .addCssClass(styling.rootCss())
                .add(classNode.build())
                .build();
    }
}
