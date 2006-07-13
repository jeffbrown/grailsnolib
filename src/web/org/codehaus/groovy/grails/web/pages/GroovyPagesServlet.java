/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.pages;

import groovy.lang.Writable;
import groovy.text.Template;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * Main servlet class.  Example usage in web.xml:
 * 	<servlet>
 *       <servlet-name>GroovyPagesServlet</servlet-name>
 *       <servlet-class>org.codehaus.groovy.grails.web.pages.GroovyPagesServlet</servlet-class>
 *		<init-param>
 *			<param-name>showSource</param-name>
 *			<param-value>1</param-value>
 *			<description>
 *             Allows developers to view the intermediade source code, when they pass
 *				a showSource argument in the URL (eg /edit/list?showSource=true.
 *          </description>
 *		</init-param>
 *    </servlet>
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 * Date: Jan 10, 2004
 *
 */
public class GroovyPagesServlet extends HttpServlet /*implements GroovyObject*/ {
	
	private static final Log LOG = LogFactory.getLog(GroovyPagesServlet.class);
	
    private ServletContext context;
    private boolean showSource = false;

    private static ClassLoader parent;
    private GroovyPagesTemplateEngine engine;
    private GrailsApplicationAttributes grailsAttributes;


    /**
     * @return the servlet context
     */
    public ServletContext getServletContext() { return context; }
    /**
     * Initialize the servlet, set it's parameters.
     * @param config servlet settings
     */
    public void init(ServletConfig config) {
        // Get the servlet context
        context = config.getServletContext();
        context.log("GSP servlet initialized");

        // Ensure that we use the correct classloader so that we can find
        // classes in an application server.
        parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) parent = getClass().getClassLoader();

        showSource = config.getInitParameter("showSource") != null;
        this.grailsAttributes = new DefaultGrailsApplicationAttributes(context);
    } // init()

    /**
     * Handle HTTP GET requests.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPage(request, response);
    } // doGet()

    /**
     * Handle HTTP POST requests.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPage(request, response);
    } // doPost()

    /**
     * Execute page and produce output.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	request.setAttribute(GrailsApplicationAttributes.REQUEST_SCOPE_ID, grailsAttributes);
    	
        this.engine = grailsAttributes.getPagesTemplateEngine();
        this.engine.setShowSource(this.showSource);
        
        
        String pageId = (String)request.getAttribute(GrailsApplicationAttributes.GSP_TO_RENDER);
        if(pageId == null)
        	pageId = engine.getPageId(request);
        URL pageUrl = engine.getPageUrl(context,pageId);
        if (pageUrl == null) {
            context.log("GroovyPagesServlet:  \"" + pageUrl + "\" not found");
            response.sendError(404, "\"" + pageUrl + "\" not found.");
            return;
        }

        Template t = engine.createTemplate(context,request,response);
        if(t == null) {
            context.log("GroovyPagesServlet:  \"" + pageUrl + "\" not found");
            response.sendError(404, "\"" + pageUrl + "\" not found.");
            return;        	
        }
        Writable w = t.make();
        Writer out = GSPResonseWriter.getInstance(response, 8192);
        try {
            w.writeTo(out);
        }
        catch(Exception e) {
        	LOG.debug("Error processing GSP: " + e.getMessage(), e);
            request.setAttribute("exception",new GrailsWrappedRuntimeException(context,e));
            RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/grails-app/views/error.jsp");
            rd.forward(request,response);
        }
        finally {
            if (out != null) out.close();
        }
    } // doPage()

}
