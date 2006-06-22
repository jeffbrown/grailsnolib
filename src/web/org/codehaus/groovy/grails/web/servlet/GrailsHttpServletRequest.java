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
package org.codehaus.groovy.grails.web.servlet;

import groovy.lang.GroovyObject;
import groovy.lang.MissingMethodException;

import org.codehaus.groovy.grails.web.metaclass.GetParamsDynamicProperty;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

/**
 * <p>Wrapper for HttpServletRequest instance that also implements
 * java.util.Map to delegate to request attributes
 *
 * @author Steven Devijver
 * @author Graeme Rocher
 * @since Jul 2, 2005
 * @see javax.servlet.http.HttpServletRequest
 * @see java.util.Map
 */
public class GrailsHttpServletRequest extends HttpServletRequestWrapper implements Map, MultipartHttpServletRequest {

    Map controllerParams = Collections.EMPTY_MAP;
    BeanWrapper requestBean;

    public GrailsHttpServletRequest(HttpServletRequest delegate) {
        super(delegate);
         requestBean = new BeanWrapperImpl(delegate);
    }

    public GrailsHttpServletRequest(HttpServletRequest request, GroovyObject controller) {
        super(request);
        requestBean = new BeanWrapperImpl(request);
        controllerParams = (Map)controller.getProperty(GetParamsDynamicProperty.PROPERTY_NAME);
    }

    /* (non-Javadoc)
      * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
      */
    public String getParameter(String paramName) {
        if(controllerParams.containsKey(paramName))
            return controllerParams.get(paramName).toString();
        return super.getParameter(paramName);
    }

    public int size() {
        throw new UnsupportedOperationException("Method java.util.Map.size() is not supported by ["+getClass()+"]");
    }

    public boolean isEmpty() {
        return getRequest().getAttributeNames().hasMoreElements();
    }

    public boolean containsKey(Object key) {
        if(key instanceof String)
            return getRequest().getAttribute((String)key) != null;
        else
            return false;
    }

    public boolean containsValue(Object value) {
        while (getRequest().getAttributeNames().hasMoreElements()) {
            String s =  (String)getRequest().getAttributeNames().nextElement();
            if(getAttribute(s).equals(value))
                return true;
        }
        return false;
    }

    public Object get(Object key) {
        if(key instanceof String)     {
            Object result = getRequest().getAttribute(key.toString());
            if(result == null && requestBean.isReadableProperty(key.toString()))
                return requestBean.getPropertyValue(key.toString());
            else
                return result;
        }
        else
            return null;
    }

    public Object put(Object key, Object value) {
        if(key instanceof String)
            getRequest().setAttribute((String)key,value);
        return value;
    }

    public Object remove(Object key) {
        if(key instanceof String) {
            Object v = getRequest().getAttribute((String)key) ;
            getRequest().removeAttribute((String)key);
            return v;
        }
        return null;
    }

    public void putAll(Map arg0) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        ServletRequest r = getRequest();
        while (r.getAttributeNames().hasMoreElements()) {
            String s =  (String)r.getAttributeNames().nextElement();
            getRequest().removeAttribute(s);
        }
    }

    public Set keySet() {
        Set set = new HashSet();
        ServletRequest r = getRequest();
        while (r.getAttributeNames().hasMoreElements()) {
            String s =  (String)r.getAttributeNames().nextElement();
            set.add(s);
        }
        return set;
    }

    public Collection values() {
        Collection values = new ArrayList();
        ServletRequest r = getRequest();
        while (r.getAttributeNames().hasMoreElements()) {
            String s =  (String)r.getAttributeNames().nextElement();
            values.add(r.getAttribute(s));
        }
        return values;
    }

    public Set entrySet() {
        throw new UnsupportedOperationException("Method java.util.Map.entrySet() is not supported by ["+getClass()+"]");
    }

	public MultipartFile getFile(String name) {
		ServletRequest r = getRequest();
		if(r instanceof MultipartHttpServletRequest) {
			return ((MultipartHttpServletRequest)r).getFile(name);
		}
		else {
			throw new MissingMethodException("getFile", GrailsHttpServletRequest.class,new Object[]{name});
		}
	}

	public Map getFileMap() {
		ServletRequest r = getRequest();
		if(r instanceof MultipartHttpServletRequest) {
			return ((MultipartHttpServletRequest)r).getFileMap();
		}
		else {
			throw new MissingMethodException("getFileMap", GrailsHttpServletRequest.class,new Object[0]);
		}
	}

	public Iterator getFileNames() {
		ServletRequest r = getRequest();
		if(r instanceof MultipartHttpServletRequest) {
			return ((MultipartHttpServletRequest)r).getFileNames();
		}
		else {
			throw new MissingMethodException("getFileNames", GrailsHttpServletRequest.class,new Object[0]);
		}
	}

}
