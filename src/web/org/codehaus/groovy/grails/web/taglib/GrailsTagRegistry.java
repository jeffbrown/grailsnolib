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
package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException;
import org.codehaus.groovy.grails.web.pages.GroovyPage;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * A registry for holding all Grails tag implementations
 *
 * @author Graeme Rocher
 * @since 11-Jan-2006
 */
public class GrailsTagRegistry {
    private static GrailsTagRegistry instance;

    private static Map tagRegistry = Collections.synchronizedMap(new HashMap());

    static {
        GrailsTagRegistry tagRegistry = getInstance();
        tagRegistry.registerTag(LinkTag.TAG_NAME, LinkTag.class);
        tagRegistry.registerTag(RenderInputTag.TAG_NAME, RenderInputTag.class);
    }

    private GrailsTagRegistry() {
    }

    public static GrailsTagRegistry getInstance() {
        if(instance == null)
            instance = new GrailsTagRegistry();

        return instance;
    }

    public void registerTag(String tagName, Class tag) {
        tagRegistry.put(tagName,tag);
    }

    public boolean tagSupported(String tagName) {
        return tagRegistry.containsKey(tagName);
    }

    public GrailsTag loadTag(String tagName, ServletContext context,ServletRequest request, ServletResponse response) {
        try {
            return loadTag(tagName,context,request,response,response.getWriter());
        } catch (IOException e) {
            throw new GrailsTagException("I/O error retrieving response writer for ["+tagName+"]:" + e.getMessage(), e);
        }
    }

    public GrailsTag loadTag(String tagName, ServletContext servletContext,ServletRequest request, ServletResponse response, Writer out)
            throws GrailsTagException {
        if(tagRegistry.containsKey(tagName)) {
            Class tagClass = (Class)tagRegistry.get(tagName);

            GrailsTag tag;
            try {
                tag = (GrailsTag)tagClass.newInstance();
            } catch (InstantiationException e) {
                throw new GrailsTagException("Instantiation error loading tag ["+tagName+"]: " + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new GrailsTagException("Illegal access error loading tag ["+tagName+"]: " + e.getMessage(), e);
            }
            Map context = new HashMap();
            context.put(GroovyPage.REQUEST,request);
            context.put(GroovyPage.RESPONSE,response);
            context.put(GroovyPage.SERVLET_CONTEXT, servletContext);

            if(out != null)
                context.put(GroovyPage.OUT,out);
            tag.init(context);
            return tag;
        }
        else {
            throw new GrailsTagException("Tag ["+tagName+"] is not a a valid grails tag");
        }
    }
}
