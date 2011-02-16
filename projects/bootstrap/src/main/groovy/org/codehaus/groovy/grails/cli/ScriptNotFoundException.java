package org.codehaus.groovy.grails.cli;

/**
 * Exception thrown when Grails is asked to execute a script that it can't find.
 */
public class ScriptNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 2794605786839371674L;
    private String scriptName;

    public ScriptNotFoundException(String scriptName) {
        this.scriptName = scriptName;
    }

    public ScriptNotFoundException(String scriptName, String message, Throwable cause) {
        super(message, cause);
        this.scriptName = scriptName;
    }

    public ScriptNotFoundException(String scriptName, Throwable cause) {
        super(cause);
        this.scriptName = scriptName;
    }

    public String getScriptName() {
        return scriptName;
    }
}
