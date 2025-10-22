package ch.so.agi.interlis.glsp.diagram;

import javax.inject.Singleton;

/**
 * Centralises CSS class names used by the GLSP GModel.
 * <p>
 * Using constants ensures that server and client stay in sync when additional
 * styles are introduced for richer diagrams.
 */
@Singleton
public class InterlisDiagramStyling {

    private static final String ROOT_CSS = "interlis-diagram-root";
    private static final String CLASS_NODE_CSS = "interlis-class-node";
    private static final String CLASS_LABEL_CSS = "interlis-class-label";

    public String rootCss() {
        return ROOT_CSS;
    }

    public String classNodeCss() {
        return CLASS_NODE_CSS;
    }

    public String classLabelCss() {
        return CLASS_LABEL_CSS;
    }
}
