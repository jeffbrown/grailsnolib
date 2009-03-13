/*
 * Copyright 2004-2008 the original author or authors.
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
package org.codehaus.groovy.grails.web.converters.configuration;

import org.codehaus.groovy.grails.web.converters.Converter;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;

import java.util.List;

/**
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public interface ConverterConfiguration<C extends Converter> {

    /**
     * Lookup the ObjectMarshaller with the highest priority that support to marshall the given object
     * @param o the object which is about to be converted
     * @return the ObjectMarshaller instance
     */
    public ObjectMarshaller<C> getMarshaller(Object o);

    /**
     * Lookup the configured default Character encoding for the Converter
     * @return the Charset name
     */
    public String getEncoding();

    /**
     * Lookup the configured CircularReferenceBehaviour (how the converter should behave when a circular reference is detected)
     * @see org.codehaus.groovy.grails.web.converters.Converter.CircularReferenceBehaviour
     * @return an instance of CircularReferenceBehaviour
     */
    public Converter.CircularReferenceBehaviour getCircularReferenceBehaviour();

    /**
     * Lookup method whether the converter should default to pretty printed output
     * @return a boolean
     */
    public boolean isPrettyPrint();

    /**
     * Retrieve the ordered list of ObjectMarshallers
     * @return the List of ObjectMarshallers ordered by priority
     */
    public List<ObjectMarshaller<C>> getOrderedObjectMarshallers();

}
