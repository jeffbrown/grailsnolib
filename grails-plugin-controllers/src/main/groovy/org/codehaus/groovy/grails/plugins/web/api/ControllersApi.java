/*
 * Copyright 2010 the original author or authors.
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
package org.codehaus.groovy.grails.plugins.web.api;

import groovy.lang.Closure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder;
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod;
import org.codehaus.groovy.grails.web.metaclass.ChainMethod;
import org.codehaus.groovy.grails.web.metaclass.ForwardMethod;
import org.codehaus.groovy.grails.web.metaclass.RedirectDynamicMethod;
import org.codehaus.groovy.grails.web.metaclass.RenderDynamicMethod;
import org.codehaus.groovy.grails.web.metaclass.WithFormMethod;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.Errors;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.ModelAndView;

/**
 * API for each controller in a Grails application
 *
 * @author Graeme Rocher
 * @since 1.4
 */
@SuppressWarnings("rawtypes")
public class ControllersApi extends CommonWebApi {

    private static final String RENDER_METHOD_NAME = "render";
    private static final String BIND_DATA_METHOD = "bindData";
    private static final String SLASH = "/";
    private RedirectDynamicMethod redirect;
    private RenderDynamicMethod render = new RenderDynamicMethod();
    private BindDynamicMethod bind = new BindDynamicMethod();
    private WithFormMethod withFormMethod = new WithFormMethod();
    private ForwardMethod forwardMethod;

    public ControllersApi(GrailsPluginManager pluginManager, ApplicationContext applicationContext) {
        super(pluginManager);

        this.redirect = new RedirectDynamicMethod(applicationContext);
        this.forwardMethod= new ForwardMethod((UrlMappingsHolder) applicationContext.getBean("grailsUrlMappingsHolder"));
    }
    /**
     * Returns the URI of the currently executing action
     *
     * @return The action URI
     */
    public String getActionUri(@SuppressWarnings("unused") Object instance) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        return new StringBuilder(SLASH).append(webRequest.getControllerName()).append(SLASH).append(webRequest.getActionName()).toString();
    }

    /**
     * Returns the URI of the currently executing controller
     * @return The controller URI
     */
    public String getControllerUri(Object instance) {
        return new StringBuilder(SLASH).append(getControllerName(instance)).toString();
    }

    /**
     * Obtains a URI of a template by name
     *
     * @param name The name of the template
     * @return The template URI
     */
    public String getTemplateUri(@SuppressWarnings("unused") Object instance, String name) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        return webRequest.getAttributes().getTemplateUri(name, webRequest.getCurrentRequest());
    }

    /**
     * Obtains a URI of a view by name
     *
     * @param name The name of the view
     * @return The template URI
     */
    public String getViewUri(@SuppressWarnings("unused") Object instance, String name) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        return webRequest.getAttributes().getViewUri(name, webRequest.getCurrentRequest());
    }

    /**
     * Sets the errors instance of the current controller
     *
     * @param errors The error instance
     */
    public void setErrors(@SuppressWarnings("unused") Object instance, Errors errors ) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        webRequest.setAttribute(GrailsApplicationAttributes.ERRORS, errors, 0);
    }

    /**
     * Obtains the errors instance for the current controller
     *
     * @return The Errors instance
     */
    public Errors getErrors(@SuppressWarnings("unused") Object instance) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        return (Errors) webRequest.getAttribute(GrailsApplicationAttributes.ERRORS, 0);
    }

    /**
     * Sets the ModelAndView of the current controller
     *
     * @param mav The ModelAndView
     */
    public void setModelAndView(@SuppressWarnings("unused") Object instance, ModelAndView mav) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();

        webRequest.setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0);
    }

    /**
     * Obtains the ModelAndView for the currently executing controller
     *
     * @return The ModelAndView
     */
    public ModelAndView getModelAndView(@SuppressWarnings("unused") Object instance) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();

        return (ModelAndView) webRequest.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0);
    }

    /**
     * Obtains the chain model which is used to chain request attributes from one request to the next via flash scope
     * @return The chainModel
     */
    public Map getChainModel(Object instance) {
        return (Map) getFlash(instance).get("chainModel");
    }

    /**
     * Return true if there are an errors
     * @return True if there are errors
     */
    public boolean hasErrors(Object instance) {
        final Errors errors = getErrors(instance);
        return errors != null && errors.hasErrors() ? true : false;
    }

    /**
     * Redirects for the given arguments
     *
     * @param args The arguments
     * @return
     */
    public Object redirect(Object instance,Map args) {
        return redirect.invoke(instance, "redirect", new Object[]{ args });
    }

    /**
     * Invokes the chain method for the given arguments
     *
     * @param instance The instance
     * @param args The arguments
     * @return Result of the redirect call
     */
    public Object chain(Object instance, Map args){
        return ChainMethod.invoke( instance, args );
    }

    // the render method
    public Object render(Object instance, Object o ) {
        return render.invoke(instance, RENDER_METHOD_NAME, new Object[] { DefaultGroovyMethods.inspect(o) });
    }

    public Object render(Object instance, String txt) {
        return render.invoke(instance, RENDER_METHOD_NAME, new Object[] { txt });
    }

    public Object render(Object instance, Map args) {
        return render.invoke(instance, RENDER_METHOD_NAME, new Object[] { args });
    }

    public Object render(Object instance, Closure c) {
        return render.invoke(instance, RENDER_METHOD_NAME, new Object[] { c });
    }

    public Object render(Object instance, Map args, Closure c) {
        return render.invoke(instance, RENDER_METHOD_NAME, new Object[] { args , c});
    }

    // the bindData method
    public Object bindData(Object instance, Object target, Object args) {
        return bind.invoke(instance, BIND_DATA_METHOD, new Object[] { target, args});
    }

    @SuppressWarnings({ "serial", "unchecked" })
    public Object bindData(Object instance, Object target, Object args, final List disallowed) {
        return bind.invoke(instance, BIND_DATA_METHOD, new Object[] { target, args, new HashMap() {{
            put("exclude", disallowed);
        }}});
    }

    @SuppressWarnings({ "serial", "unchecked" })
    public Object bindData(Object instance, Object target, Object args, final List disallowed, String filter) {
        return bind.invoke(instance, BIND_DATA_METHOD, new Object[] { target, args, new HashMap() {{
            put("exclude", disallowed);
        }}, filter});
    }

    public Object bindData(Object instance, Object target, Object args, Map includeExclude) {
        return bind.invoke(instance, BIND_DATA_METHOD, new Object[] { target, args, includeExclude});
    }

    public Object bindData(Object instance, Object target, Object args, Map includeExclude, String filter) {
        return bind.invoke(instance, BIND_DATA_METHOD, new Object[] { target, args, includeExclude, filter});
    }

    public Object bindData(Object instance, Object target, Object args, String filter) {
        return bind.invoke(instance, BIND_DATA_METHOD, new Object[] { target, args, filter});
    }

    // the withForm method
    public Object withForm(Object instance, Closure callable) {
        return withFormMethod.withForm(getRequest(instance), callable);
    }

    public String forward(@SuppressWarnings("unused") Object instance, Map params) {
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        return forwardMethod.forward(webRequest.getCurrentRequest(), webRequest.getCurrentResponse(), params);
    }
}
