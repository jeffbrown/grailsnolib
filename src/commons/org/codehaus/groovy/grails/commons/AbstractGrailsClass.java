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
package org.codehaus.groovy.grails.commons;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.grails.exceptions.NewInstanceCreationException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * 
 * 
 * @author Steven Devijver
 * @since Jul 2, 2005
 */
public abstract class AbstractGrailsClass implements GrailsClass {

	private Class clazz = null;
	private String fullName = null;
	private String name = null;
	private String packageName = null;
	private BeanWrapper reference = null;
    private String naturalName;
    private String shortName;



    /**
     * <p>Contructor to be used by all child classes to create a
     * new instance and get the name right.
     *
     * @param clazz the Grails class
     * @param trailingName the trailing part of the name for this class type
     */
    public AbstractGrailsClass(Class clazz, String trailingName) {
        super();
        setClazz(clazz);

        this.reference = new BeanWrapperImpl(newInstance());
        this.fullName = clazz.getName();
        this.packageName = ClassUtils.getPackageName(clazz);
        this.naturalName = GrailsClassUtils.getNaturalName(clazz.getName());
        if (StringUtils.isBlank(trailingName)) {
            this.name = fullName;
            this.shortName = getShortClassname(clazz);
        } else {
            this.shortName = getShortClassname(clazz);
            if(shortName.indexOf( trailingName ) > - 1) {
                this.name = shortName.substring(0, shortName.length() - trailingName.length());
            }
            else {
                this.name = fullName;
            }
        }
    }

    public String getShortName() {
        return this.shortName;
    }

    private void setClazz(Class clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("Clazz parameter should not be null");
		}
		this.clazz = clazz;
	}
	
	public Class getClazz() {
		return this.clazz;
	}
	
	public Object newInstance() {
		try {
			return getClazz().newInstance();
		} catch (Exception e) {
			Throwable targetException = null;
			if (e instanceof InvocationTargetException) {
				targetException = ((InvocationTargetException)e).getTargetException();
			} else {
				targetException = e;
			}
			throw new NewInstanceCreationException("Could not create a new instance of class [" + getClazz().getName() + "]!", targetException);
		}
	}

	public String getName() {
		return this.name;
	}

    public String getNaturalName() {
        return this.naturalName;
    }

    public String getFullName() {
		return this.fullName;
	}

    public String getPropertyName() {
        return GrailsClassUtils.getPropertyNameRepresentation(getShortName());
    }

    public String getPackageName() {
		return this.packageName;
	}
	
	private static String getShortClassname(Class clazz) {
		return ClassUtils.getShortClassName(clazz);
	}
	
	/**
	 * <p>The reference instance is used to get configured property values.
	 * 
	 * @return BeanWrapper instance that holds reference
	 */
	protected BeanWrapper getReference() {
		return this.reference;
	}
	
	/**
	 * <p>Looks for a property of the reference instance with a given name and type. If found
	 * its value is returned, otherwise look for a public static field with given name and type,
	 * otherwise return null.
	 * 
	 * @return property value or null if no property or static field was found
	 */
	protected Object getPropertyValue(String name, Class type) {
		BeanWrapper ref = getReference();
		if (ref.isReadableProperty(name) && ref.getPropertyType(name).isAssignableFrom(type)) {
			Object value = ref.getPropertyValue(name);
			if(value != null && type.isAssignableFrom(value.getClass())) {
				return value;
			}
			return null;
		} else {
			try {
				Field field = ref.getWrappedClass().getField(name);
				if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()) && field.getType().equals(type)) {
					return field.get(ref.getWrappedInstance());
				}
			} catch (NoSuchFieldException e) {
				// ignore
			} catch (IllegalAccessException e) {
				// ignore
			}
		}
		
		return null;
	}
}
