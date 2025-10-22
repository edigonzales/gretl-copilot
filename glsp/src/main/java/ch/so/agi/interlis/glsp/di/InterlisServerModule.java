package ch.so.agi.interlis.glsp.di;

import org.eclipse.glsp.server.di.ServerModule;

/**
 * Registers the INTERLIS diagram specific {@link InterlisDiagramModule} with
 * the GLSP runtime. The server module is referenced from the launcher and keeps
 * all diagram bindings in a dedicated module so that additional diagrams can be
 * plugged in later on without touching the bootstrap code.
 */
public class InterlisServerModule extends ServerModule {

    public InterlisServerModule() {
        configureDiagramModule(new InterlisDiagramModule());
    }
}
