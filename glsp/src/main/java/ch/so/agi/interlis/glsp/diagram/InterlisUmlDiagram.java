package ch.so.agi.interlis.glsp.diagram;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.interlis.ili2c.metamodel.TransferDescription;

/**
 * Lightweight UML abstraction that mirrors the structure used by the VS Code
 * language server.
 * <p>
 * The class stores namespaces, nodes and associations that can later be mapped
 * to the GLSP GModel. For now the builder only creates an empty diagram but the
 * skeleton is ready for a full implementation.
 */
public final class InterlisUmlDiagram {

    private static final Logger LOGGER = LogManager.getLogger(InterlisUmlDiagram.class);

    private InterlisUmlDiagram() {
    }

    /** Builds a diagram representation for the provided transfer description. */
    public static Diagram build(TransferDescription td) {
        Objects.requireNonNull(td, "TransferDescription must not be null");
        LOGGER.debug("Building UML diagram from transfer description");
        return new Diagram();
    }

    /** Container for namespaces, nodes and edges. */
    public static final class Diagram {
        public final Map<String, Namespace> namespaces = new LinkedHashMap<>();
        public final Map<String, Node> nodes = new LinkedHashMap<>();
        public final List<Inheritance> inheritances = new ArrayList<>();
        public final List<Assoc> assocs = new ArrayList<>();

        Namespace getOrCreateNamespace(String label) {
            return namespaces.computeIfAbsent(label, Namespace::new);
        }
    }

    /** Logical grouping comparable to INTERLIS models and topics. */
    public static final class Namespace {
        public final String label;
        public final List<String> nodeOrder = new ArrayList<>();

        Namespace(String label) {
            this.label = label;
        }
    }

    /** UML node information. */
    public static final class Node {
        public final String fqn;
        public final String displayName;
        public final Set<String> stereotypes = new LinkedHashSet<>();
        public final List<String> attributes = new ArrayList<>();
        public final List<String> methods = new ArrayList<>();

        public Node(String fqn, String displayName, Set<String> stereotypes) {
            this.fqn = fqn;
            this.displayName = displayName;
            if (stereotypes != null) {
                this.stereotypes.addAll(stereotypes);
            }
        }
    }

    /** Inheritance edge descriptor. */
    public static final class Inheritance {
        public final String subFqn;
        public final String supFqn;

        public Inheritance(String subFqn, String supFqn) {
            this.subFqn = subFqn;
            this.supFqn = supFqn;
        }
    }

    /** Association descriptor. */
    public static final class Assoc {
        public final String leftFqn;
        public final String rightFqn;
        public final String leftCard;
        public final String rightCard;
        public final String label;

        public Assoc(String leftFqn, String rightFqn, String leftCard, String rightCard, String label) {
            this.leftFqn = leftFqn;
            this.rightFqn = rightFqn;
            this.leftCard = leftCard;
            this.rightCard = rightCard;
            this.label = label;
        }
    }
}
