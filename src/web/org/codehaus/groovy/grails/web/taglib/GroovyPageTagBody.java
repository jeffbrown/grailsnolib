/* Copyright 2004-2005 Graeme Rocher
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

import groovy.lang.Binding;
import groovy.lang.Closure;
import org.codehaus.groovy.grails.web.pages.GroovyPage;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * A closure that represents the body of a tag and captures its output returning the result when invoked
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Apr 19, 2007
 *        Time: 2:21:38 PM
 */
public class GroovyPageTagBody extends Closure {
    private Closure bodyClosure;
    private GrailsWebRequest webRequest;
    private Binding binding;
    private static final String BLANK_STRING = "";

    public GroovyPageTagBody(Object owner, GrailsWebRequest webRequest,Closure bodyClosure) {
        super(owner);

        if(bodyClosure == null) throw new IllegalStateException("Argument [bodyClosure] cannot be null!");
        if(webRequest == null) throw new IllegalStateException("Argument [webRequest] cannot be null!");

        this.bodyClosure = bodyClosure;
        this.webRequest = webRequest;
        binding = null;
        if(owner instanceof GroovyPage)
            binding = ((GroovyPage) owner).getBinding();

    }


    private Object captureClosureOutput(Object args) {
        Writer originalOut = webRequest.getOut();

        try {
            final GroovyPageTagWriter capturedOut = createWriter();




            if(args!=null) {
                if(args instanceof Map) {
                    Map delegate = binding.getVariables();

                    final Map argMap = (Map) args;
                    delegate.putAll(argMap);

                    try {
                        bodyClosure.call(args);
                    } finally {
                        for (Iterator i = argMap.keySet().iterator(); i.hasNext();) {
                            Object o =  i.next();
                            delegate.remove(o);                            
                        }
                    }
                }
                else {
                    bodyClosure.call(args);
                }
            }
            else {
                bodyClosure.call();
            }
            String output = capturedOut.getValue();
            if(output == null) return BLANK_STRING;

            return output;
        } finally {
            if(binding!=null) {
                binding.setVariable(GroovyPage.OUT, originalOut);
            }
            webRequest.setOut(originalOut);
        }
    }

    private GroovyPageTagWriter createWriter() {

        StringWriter capturedOut = new StringWriter();
        GroovyPageTagWriter out = new GroovyPageTagWriter(capturedOut);
        if(binding!=null) {
            binding.setVariable(GroovyPage.OUT, out);
        }
        webRequest.setOut(out);

        return out;
    }

    public Object doCall() {
        return captureClosureOutput(null);
    }

    public Object doCall(Object arguments) {
        return captureClosureOutput(arguments);
    }

    public Object call() {
        return captureClosureOutput(null);
    }

    public Object call(Object[] args) {
        return captureClosureOutput(args);
    }

    public Object call(Object arguments) {
        return captureClosureOutput(arguments);
    }
}
