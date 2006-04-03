package org.codehaus.groovy.grails.web.servlet;

import org.springframework.validation.Errors;
import org.springframework.context.ApplicationContext;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import groovy.lang.GroovyObject;

import javax.servlet.ServletRequest;
import javax.servlet.ServletContext;

/**
 * An interface defining the names of and methods to retrieve Grails specific request and servlet attributes
 *
 * @author Graeme Rocher
 * @since 17-Jan-2006
 */
public interface GrailsApplicationAttributes {

	String PATH_TO_VIEWS = "/WEB-INF/grails-app/views";
    String GSP_TEMPLATE_ENGINE = "org.codehaus.groovy.grails.GSP_TEMPLATE_ENGINE";
    String APPLICATION_CONTEXT = "org.codehaus.groovy.grails.APPLICATION_CONTEXT";
    String FLASH_SCOPE = "org.codehaus.groovy.grails.FLASH_SCOPE";
    String CONTROLLER = "org.codehaus.groovy.grails.CONTROLLER";
    String ERRORS =  "org.codehaus.groovy.grails.ERRORS";
    String TAG_CACHE = "org.codehaus.groovy.grails.TAG_CACHE";
    String ID_PARAM = "id";
    String PARENT_APPLICATION_CONTEXT = "org.codehaus.groovy.grails.PARENT_APPLICATION_CONTEXT";

    /**
     * @return The application context for servlet
     */
    ApplicationContext getApplicationContext();

    /**
     * @return The controller for the request
     */
    GroovyObject getController(ServletRequest request);

    /**
     *
     * @param request
     * @return The uri of the controller within the request
     */
    String getControllerUri(ServletRequest request);

    /**
     *
     * @param request
     * @return The uri of the application relative to the server root
     */
    String getApplicationUri(ServletRequest request);

    /**
     * Retrieves the servlet context instance
     * @return The servlet context instance
     */
    ServletContext getServletContext();

    /**
     * Retrieves the flash scope instance for the given requeste
     * @param request
     * @return The FlashScope instance
     */
    FlashScope getFlashScope(ServletRequest request);
    /**
     *
     * @param templateName
     * @param request
     * @return The uri of a named template for the current controller
     */
    String getTemplateUri(String templateName, ServletRequest request);

    /**
     *
     * @param request
     * @return The uri of the action called within the controller
     */
    String getControllerActionUri(ServletRequest request);

    /**
     *
     * @param request
     * @return The errors instance contained within the request
     */
    Errors getErrors(ServletRequest request);

    /**
     *
     * @return Retrieves the shared GSP template engine
     */
    GroovyPagesTemplateEngine getPagesTemplateEngine();

    /**
     *
     * @return Retrieves the grails application instance
     */
    GrailsApplication getGrailsApplication();

    /**
     * Retrieves a Grails tag library from the request for the named tag
     * @param tagName The name of the tag that contains the tag library
     * 
     * @return An instance of the tag library or null if not found
     */
	GroovyObject getTagLibraryForTag(ServletRequest request,String tagName);
}
