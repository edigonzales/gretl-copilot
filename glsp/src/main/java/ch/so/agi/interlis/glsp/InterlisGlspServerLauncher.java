package ch.so.agi.interlis.glsp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.glsp.server.di.ServerModule;
import org.eclipse.glsp.server.launch.SocketGLSPServerLauncher;

import ch.so.agi.interlis.glsp.di.InterlisServerModule;

/**
 * Entry point that starts the INTERLIS GLSP server as a classic socket server.
 * <p>
 * The launcher wires up the Guice modules defined in {@link InterlisServerModule}
 * and hands them over to the GLSP runtime. The module takes care of
 * diagram-specific bindings, GModel creation and (future) source model
 * compilation. Keeping this class tiny ensures it can also be reused by future
 * deployment options such as dedicated processes started from VS Code.
 */
public final class InterlisGlspServerLauncher extends SocketGLSPServerLauncher {

    private static final Logger LOGGER = LogManager.getLogger(InterlisGlspServerLauncher.class);
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 7012;

    public InterlisGlspServerLauncher() {
        super(createServerModule());
    }

    private static ServerModule createServerModule() {
        return new InterlisServerModule();
    }

    /**
     * Starts the GLSP socket server. The default host/port values are aligned
     * with the VS Code integration and can be overridden via command line
     * arguments: {@code <host> <port>}.
     */
    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        InterlisGlspServerLauncher launcher = new InterlisGlspServerLauncher();
        LOGGER.info("Starting INTERLIS GLSP server on {}:{}", host, port);
        launcher.start(host, port);
    }
}
