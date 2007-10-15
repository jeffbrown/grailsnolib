/* Copyright 2006-2007 Graeme Rocher
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
package grails.converters;


import com.thoughtworks.xstream.XStream;
import groovy.util.XmlSlurper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.converters.AbstractConverter;
import org.codehaus.groovy.grails.web.converters.Converter;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.xtream.DomainClassConverter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

/**
 * A converter that converts domain classes to XML
 *
 * @author Siegfried Puchbauer
 */
public class XML extends AbstractConverter implements Converter {

    public static final Log log = LogFactory.getLog(XML.class);

    private Object target;

    /**
     * Configures the XStream instance
     *
     * @param xs an XStream instance
     */
    public void configureXStream(XStream xs) {
        DomainClassConverter dcConverter = new DomainClassConverter();
        dcConverter.setRenderDomainClassRelations(false);
        xs.registerConverter(dcConverter);
    }

    /**
     * Default Constructor
     */
    public XML() {

    }

    /**
     * Initializes the Converter with the target
     *
     * @param target the target object to convert
     */
    public XML(Object target) {
        this.target = target;
    }

    /**
     * Renders the XML to the given Writer
     *
     * @param out the Writer
     * @throws org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
     *
     */
    public void render(Writer out) throws ConverterException {
        ConverterUtil.getXStream(getClass()).toXML(target, out);
    }

    /**
     * Renders the XML to the HttpServletResponse, also setting the the Content-Type (text/xml)
     *
     * @param response the HttpServletResponse
     * @throws ConverterException
     */
    public void render(HttpServletResponse response) throws ConverterException {
        response.setContentType("text/xml");
        try {
            Writer writer = response.getWriter();
            String encoding = response.getCharacterEncoding();
            writer.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>");
            render(writer);
        } catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    /**
     * Parses the given XML
     *
     * @param source a String containing some XML
     * @return a groovy.util.XmlSlurper
     * @throws ConverterException
     */
    public static Object parse(String source) throws ConverterException {
        try {
            return new XmlSlurper().parse(source);
        } catch (Exception e) {
            throw new ConverterException("Error parsing XML", e);
        }
    }

    /**
     * Parses the given XML
     *
     * @param is       an InputStream to read from
     * @param encoding the Character Encoding to use
     * @return a groovy.util.XmlSlurper
     * @throws ConverterException
     */
    public static Object parse(InputStream is, String encoding) throws ConverterException {
        try {
            InputStreamReader reader = new InputStreamReader(is, encoding);
            return new XmlSlurper().parse(reader);
        } catch (Exception e) {
            throw new ConverterException("Error parsing XML", e);
        }
    }

    /**
     * Parses the give XML (read from the POST Body of the Request)
     *
     * @param request an HttpServletRequest
     * @return a groovy.util.XmlSlurper
     * @throws ConverterException
     */
    public static Object parse(HttpServletRequest request) throws ConverterException {
        String encoding = request.getCharacterEncoding();
        if (encoding == null)
            encoding = Converter.DEFAULT_REQUEST_ENCODING;
        try {
            return parse(request.getInputStream(), encoding);
        } catch (IOException e) {
            throw new ConverterException("Error parsing XML", e);
        }
    }

    /**
     * @param target the target to convert
     * @see org.codehaus.groovy.grails.web.converters.Converter
     */
    public void setTarget(Object target) {
        this.target = target;
    }

}
