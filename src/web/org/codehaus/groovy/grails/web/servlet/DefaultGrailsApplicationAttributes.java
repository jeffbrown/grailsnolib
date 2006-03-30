/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.web.servlet;

import org.springframework.web.util.UrlPathHelper;
import org.springframework.validation.Errors;
import org.springframework.context.ApplicationContext;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import groovy.lang.GroovyObject;

import javax.servlet.ServletRequest;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author Graeme Rocher
 * @since 17-Jan-2006
 */
public class DefaultGrailsApplicationAttributes implements GrailsApplicationAttributes {

    private UrlPathHelper urlHelper = new UrlPathHelper();
    private ServletContext context;

    public DefaultGrailsApplicationAttributes(ServletContext context) {
        this.context = context;
    }

    public ApplicationContext getApplicationContext() {
        return (ApplicationContext)this.context.getAttribute(APPLICATION_CONTEXT);
    }

    public GroovyObject getController(ServletRequest request) {
        return (GroovyObject)request.getAttribute(CONTROLLER);
    }

    public String getControllerUri(ServletRequest request) {
        GroovyObject controller = getController(request);
        if(controller != null)
            return (String)controller.getProperty(ControllerDynamicMethods.CONTROLLER_URI_PROPERTY);
        else
            return null;
    }

    public String getApplicationUri(ServletRequest request) {
        return this.urlHelper.getContextPath((HttpServletRequest)request);
    }

    public ServletContext getServletContext() {
        return this.context;
    }

    public FlashScope getFlashScope(ServletRequest request) {
        if(request instanceof HttpServletRequest) {
            HttpSession session = ((HttpServletRequest)request).getSession(true);
            FlashScope fs = (FlashScope)session.getAttribute(FLASH_SCOPE);
            if(fs == null) {
                fs = new GrailsFlashScope();
                session.setAttribute(FLASH_SCOPE,fs);
            }
            return fs;
        }
        return null;
    }

    public String getTemplateUri(String templateName, ServletRequest request) {
       	
       StringBuffer buf = new StringBuffer(PATH_TO_VIEWS);
       
       if(templateName.startsWith("/")) {
    	   String tmp = templateName.substring(1,templateName.length());
    	   if(tmp.indexOf('/') > -1) {
    		   buf.append('/');
    		   buf.append(tmp.substring(0,tmp.lastIndexOf('/')));
    		   buf.append("/_");
    		   buf.append(tmp.substring(tmp.lastIndexOf('/') + 1,tmp.length()));
    	   }
    	   else {
    		   buf.append("/_");
    		   buf.append(templateName.substring(1,templateName.length()));
    	   }
       }
       else {
           buf.append(getControllerUri(request))
           .append("/_")
           .append(templateName);
    	   
       }
       return buf
       			.append(".gsp")
       			.toString();
   }

    public String getControllerActionUri(ServletRequest request) {
        GroovyObject controller = getController(request);

        return (String)controller.getProperty(ControllerDynamicMethods.ACTION_URI_PROPERTY);
    }

    public Errors getErrors(ServletRequest request) {
        return (Errors)request.getAttribute(ERRORS);
    }

    public GroovyPagesTemplateEngine getPagesTemplateEngine() {
       GroovyPagesTemplateEngine engine = (GroovyPagesTemplateEngine)this.context.getAttribute(GSP_TEMPLATE_ENGINE);
       if(engine == null) {
           engine = new GroovyPagesTemplateEngine();
           engine.setClassLoader(getGrailsApplication().getClassLoader());
           this.context.setAttribute(GSP_TEMPLATE_ENGINE,engine);
       }
       return engine;
    }

    public GrailsApplication getGrailsApplication() {
        return (GrailsApplication)getApplicationContext()
                                    .getBean(GrailsApplication.APPLICATION_ID);
    }
}
