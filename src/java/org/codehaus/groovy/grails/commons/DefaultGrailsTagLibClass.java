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
package org.codehaus.groovy.grails.commons;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Graeme Rocher
 * @since 14-Jan-2006
 *
 * Default implementation of a tag lib class
 *
 */
public class DefaultGrailsTagLibClass extends AbstractInjectableGrailsClass implements GrailsTagLibClass {
    protected static final String TAG_LIB = "TagLib";

    private List supportedControllers;
    private Set tags = new HashSet();
    private String namespace = GrailsTagLibClass.DEFAULT_NAMESPACE;
    private Set<String> returnObjectForTagsSet = new HashSet<String>();
    
    /**
     * <p>Default contructor
     *
     * @param clazz        the tag library class
     */
    public DefaultGrailsTagLibClass(Class clazz) {
        super(clazz, TAG_LIB);
        Class supportedControllerClass = getStaticPropertyValue(SUPPORTS_CONTROLLER, Class.class);
        if(supportedControllerClass != null) {
            supportedControllers = new ArrayList();
            supportedControllers.add(supportedControllerClass);
        }
        else {
            List tmp = getStaticPropertyValue(SUPPORTS_CONTROLLER, List.class);
            if(tmp != null) {
                supportedControllers = tmp;
            }
        }

        PropertyDescriptor[] props = getPropertyDescriptors();
        for (int i = 0; i < props.length; i++) {
            PropertyDescriptor prop = props[i];
            Method readMethod = prop.getReadMethod();
			if(readMethod != null) {
				if(!Modifier.isStatic(readMethod.getModifiers())) {
					Class type = prop.getPropertyType();
					if(type == Object.class) {
		                tags.add(prop.getName());
		            }					
				}				
			}
        }
        
        String ns = getStaticPropertyValue(NAMESPACE_FIELD_NAME, String.class);
        if (ns != null && !"".equals(ns.trim())) {
        	namespace = ns.trim();
        }
        
        List returnObjectForTagsList = getStaticPropertyValue(RETURN_OBJECT_FOR_TAGS_FIELD_NAME, List.class);
        if(returnObjectForTagsList != null) {
        	for(Object tagName : returnObjectForTagsList) {
        		returnObjectForTagsSet.add(String.valueOf(tagName));
        	}
        }
    }

    public boolean supportsController(GrailsControllerClass controllerClass) {
        if(controllerClass == null)
            return false;
        else if(supportedControllers != null) {
           if(supportedControllers.contains(controllerClass.getClazz())) {
               return true;
           }
           else {
               return false;
           }
        }
        return true;
    }

    public boolean hasTag(String tagName) {
        return tags.contains(tagName);
    }

    public Set getTagNames() {
        return tags;
    }

	public String getNamespace() {
		return namespace;
	}

	public Set<String> getTagNamesThatReturnObject() {
		return returnObjectForTagsSet;
	}
}
