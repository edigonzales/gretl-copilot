package ch.so.agi.interlis.glsp.di;

import org.eclipse.glsp.server.diagram.DiagramConfiguration;
import org.eclipse.glsp.server.di.DiagramModule;
import org.eclipse.glsp.server.features.core.model.GModelFactory;
import org.eclipse.glsp.server.features.core.model.SourceModelStorage;
import org.eclipse.glsp.server.model.DefaultGModelState;
import org.eclipse.glsp.server.model.GModelState;

import ch.so.agi.interlis.glsp.diagram.InterlisDiagramConfiguration;
import ch.so.agi.interlis.glsp.diagram.InterlisDiagramGModelFactory;
import ch.so.agi.interlis.glsp.diagram.InterlisSourceModelStorage;

/**
 * GLSP diagram module wiring the INTERLIS specific services.
 * <p>
 * The module binds the diagram configuration, source model storage and GModel
 * factory that translates the INTERLIS compilation result into the visual graph.
 * In the first iteration we return a placeholder rectangle, but the binding
 * structure is already prepared for a full UML mapping.
 */
public class InterlisDiagramModule extends DiagramModule {

    public static final String DIAGRAM_TYPE = "interlis.uml.class";

    @Override
    protected Class<? extends DiagramConfiguration> bindDiagramConfiguration() {
        return InterlisDiagramConfiguration.class;
    }

    @Override
    protected Class<? extends SourceModelStorage> bindSourceModelStorage() {
        return InterlisSourceModelStorage.class;
    }

    @Override
    protected Class<? extends GModelFactory> bindGModelFactory() {
        return InterlisDiagramGModelFactory.class;
    }

    @Override
    protected Class<? extends GModelState> bindGModelState() {
        return DefaultGModelState.class;
    }

    @Override
    public String getDiagramType() {
        return DIAGRAM_TYPE;
    }
}
