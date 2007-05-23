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
package org.codehaus.groovy.grails.web.metaclass;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsControllerClass;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.scaffolding.GrailsScaffolder;
import org.codehaus.groovy.grails.web.servlet.FlashScope;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsHttpServletResponse;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.ControllerExecutionException;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implements the "chain" Controller method for action chaining
 * 
 * @author Graeme Rocher
 * @since 0.2
 * 
 * Created: Oct 27, 2005
 */
public class ChainDynamicMethod extends AbstractDynamicMethodInvocation {
	private static final String SCAFFOLDER = "Scaffolder";
    private static final Log LOG = LogFactory.getLog(ChainDynamicMethod.class);

    public static final String METHOD_SIGNATURE = "chain";
    public static final Pattern METHOD_PATTERN = Pattern.compile('^'+METHOD_SIGNATURE+'$');

    public static final String ARGUMENT_CONTROLLER = "controller";
    public static final String ARGUMENT_ACTION = "action";
    public static final String ARGUMENT_ID = "id";
    public static final String ARGUMENT_PARAMS = "params";
    public static final String ARGUMENT_ERRORS = "errors";
    public static final Object ARGUMENT_MODEL = "model";

    static public final String PROPERTY_CHAIN_MODEL = "chainModel";

    public ChainDynamicMethod() {
        super(METHOD_PATTERN);
    }

    public Object invoke(Object target, String methodName, Object[] arguments) {
        if(arguments.length == 0)
            throw new MissingMethodException(METHOD_SIGNATURE,target.getClass(),arguments);

        GrailsWebRequest webRequest = (GrailsWebRequest)RequestContextHolder.currentRequestAttributes();
        Object actionRef;
        String controllerName;
        Object id;
        Map params;
        Map model;
        Errors errors;
        GroovyObject controller = (GroovyObject)target;

        if(arguments[0] instanceof Map) {
            Map argMap = (Map)arguments[0];
            actionRef = argMap.get(ARGUMENT_ACTION);
            controllerName = (String)argMap.get(ARGUMENT_CONTROLLER);
            id =  argMap.get(ARGUMENT_ID);
            params = (Map)argMap.get(ARGUMENT_PARAMS);
            model = (Map)argMap.get(ARGUMENT_MODEL);
            errors = (Errors)argMap.get(ARGUMENT_ERRORS);
        }
        else {
            throw new MissingMethodException(METHOD_SIGNATURE,target.getClass(),arguments);
        }
        // place the chain model in flash scope
        FlashScope fs = webRequest.getFlashScope();
        if(fs.containsKey(PROPERTY_CHAIN_MODEL)) {
            Map chainModel = (Map)fs.get(PROPERTY_CHAIN_MODEL);
            if(chainModel != null) {
                chainModel.putAll(model);
                model = chainModel;
            }
        }

        fs.put(PROPERTY_CHAIN_MODEL, model);

        // if there are errors add it to the list of errors
        Errors controllerErrors = (Errors)controller.getProperty( ControllerDynamicMethods.ERRORS_PROPERTY );
        if(controllerErrors != null) {
            controllerErrors.addAllErrors(errors);
        }
        else {
            controller.setProperty( ControllerDynamicMethods.ERRORS_PROPERTY, errors);
        }

        String actionName = null;
        if(actionRef instanceof String) {
           actionName = (String)actionRef;
        }
        else if(actionRef instanceof Closure) {
            Closure c = (Closure)actionRef;
            PropertyDescriptor prop = GrailsClassUtils.getPropertyDescriptorForValue(target,c);
            if(prop != null) {
                actionName = prop.getName();
            }
            else {
                GrailsScaffolder scaffolder = getScaffolderForController(target.getClass().getName(), webRequest);
                if(scaffolder != null) {
                        actionName = scaffolder.getActionName(c);
                }
            }
        }

        if(actionName != null) {
        	GrailsApplicationAttributes attrs = webRequest.getAttributes();
        	HttpServletRequest request = webRequest.getCurrentRequest();
            StringBuffer actualUri = new StringBuffer(attrs.getApplicationUri(request));
            if(controllerName != null) {
                actualUri.append('/')
                         .append(controllerName);
            }
            else {
                actualUri.append(attrs.getControllerUri(request));
            }
            actualUri.append('/')
                     .append(actionName);
            if(id != null) {
                actualUri.append('/')
                         .append(id);
            }
            if(params != null) {
                actualUri.append('?');
                for (Iterator i = params.keySet().iterator(); i.hasNext();) {
                    Object name = i.next();
                    actualUri.append(name)
                             .append('=')
                             .append(params.get(name));
                    if(i.hasNext())
                        actualUri.append('&');
                }
            }
            if(LOG.isDebugEnabled()) {
                LOG.debug("Dynamic method [chain] redirecting request to ["+actualUri+"]");
            }

            try {
            	GrailsHttpServletResponse response = webRequest.getCurrentResponse();
                response.sendRedirect(response.encodeRedirectURL(actualUri.toString()));
            } catch (IOException e) {
                throw new ControllerExecutionException("Error redirecting request for url ["+actualUri+"]: " + e.getMessage(),e);
            }

        }
        else {
            throw new ControllerExecutionException("Action not found in redirect for name ["+actionName+"]");
        }

        return null;
    }
    
    public GrailsScaffolder getScaffolderForController(String controllerName, GrailsWebRequest webRequest) {
    	GrailsApplicationAttributes attributes = webRequest.getAttributes();
		GrailsControllerClass controllerClass = (GrailsControllerClass) attributes.getGrailsApplication().getArtefact(
            ControllerArtefactHandler.TYPE, controllerName);
        return (GrailsScaffolder)attributes
        							.getApplicationContext()
        							.getBean(controllerClass.getFullName() + SCAFFOLDER );
    }        

}
