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
package org.codehaus.groovy.grails.web.mapping;

import org.codehaus.groovy.grails.web.mapping.exceptions.UrlMappingException;
import org.codehaus.groovy.grails.validation.ConstrainedProperty;
import org.apache.commons.lang.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;
import java.util.Map;
import java.util.HashMap;

/**
 * <p>A UrlMapping implementation that takes a Grails URL pattern and turns it into a regex matcher so that
 * URLs can be matched and information captured from the match.</p>
 *
 * <p>A Grails URL pattern is not a regex, but is an extension to the form defined by Apache Ant and used by
 * Spring AntPathMatcher. Unlike regular Ant paths Grails URL patterns allow for capturing groups in the form:</p>
 *
 * <code>/blog/(*)/**</code>
 *
 * <p>The parenthesis define a capturing group. This implementation transforms regular Ant paths into regular expressions
 * that are able to use capturing groups</p>
 * 
 *
 * @see org.springframework.util.AntPathMatcher
 *
 * @author Graeme Rocher
 * @since 0.5
 *
 *        <p/>
 *        Created: Feb 28, 2007
 *        Time: 6:12:52 PM
 */
public class RegexUrlMapping implements UrlMapping {

    private Pattern[] patterns;
    private ConstrainedProperty[] constraints = new ConstrainedProperty[0];
    private String controllerName;
    private String actionName;
    private UrlMappingData urlData;
    
    private static final String WILDCARD = "*";
    private static final String CAPTURED_WILDCARD = "(*)";
    private static final String SLASH = "/";

    /*
    /*
     * @see #RegexUrlMapping(String, String, String, java.util.List)
     */

    public RegexUrlMapping(UrlMappingData data, String controllerName, ConstrainedProperty[] constraints) {
        this(data,controllerName, null, constraints);
    }

    /**
     * Constructs a new RegexUrlMapping for the given pattern, controller name, action name and constraints.
     *
     * @param data An instance of the UrlMappingData class that holds necessary information of the URL mapping
     * @param controllerName The name of the controller the URL maps to (required)
     * @param actionName The name of the action the URL maps to
     * @param constraints A list of ConstrainedProperty instances that relate to tokens in the URL
     *
     * @see org.codehaus.groovy.grails.validation.ConstrainedProperty
     */
    public RegexUrlMapping(UrlMappingData data, String controllerName, String actionName, ConstrainedProperty[] constraints) {
        if(data == null) throw new IllegalArgumentException("Argument [pattern] cannot be null");
        if(StringUtils.isBlank(controllerName)) throw new IllegalArgumentException("Argument [controllerName] cannot be null or blank");

        this.controllerName = controllerName;
        this.actionName = actionName;
        String[] urls = data.getLogicalUrls();
        this.urlData = data;
        this.patterns = new Pattern[urls.length];

        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];
            Pattern pattern = convertToRegex(url);

            if(pattern == null) throw new IllegalStateException("Cannot use null pattern in regular expression mapping for url ["+data.getUrlPattern()+"]");
            this.patterns[i] = pattern;

        }
        if(constraints != null) {
            this.constraints = constraints;
        }

    }

    /**
     * Converst a Grails URL provides via the UrlMappingData interface to a regular expression
     *
     * @param url The URL to convert
     * @return A regex Pattern objet
     */
    protected Pattern convertToRegex(String url) {
        Pattern regex;
        String pattern = null;
        try {
            pattern = url.replaceAll("\\*", "[^/]+");
            regex = Pattern.compile(pattern);

        } catch (PatternSyntaxException pse) {
            throw new UrlMappingException("Error evaluating mapping for pattern ["+pattern+"] from Grails URL mappings: " + pse.getMessage(), pse);
        }

        return regex;
    }

    /**
     * Matches the given URI and returns a DefaultUrlMappingInfo instance or null
     *
     *
     * @param uri The URI to match
     * @return A UrlMappingInfo instance or null
     *
     * @see org.codehaus.groovy.grails.web.mapping.UrlMappingInfo
     */
    public UrlMappingInfo match(String uri) {

        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = patterns[i];
            Matcher m = pattern.matcher(uri);
            if(m.find()) {
                  UrlMappingInfo urlInfo = createUrlMappingInfo(uri,m);
                  if(urlInfo!=null) {
                      return urlInfo;
                  }
            }

        }
        return null;
    }

    public UrlMappingData getUrlData() {
        return this.urlData;
    }

    private UrlMappingInfo createUrlMappingInfo(String uri, Matcher m) {
        Map params = new HashMap();
        Errors errors = new MapBindingResult(params, "urlMapping");
        String lastGroup = null;
        for (int i = 0; i < m.groupCount(); i++) {
            lastGroup = m.group(i+1);
            if(constraints.length > i) {
                ConstrainedProperty cp = constraints[i];
                cp.validate(this,lastGroup, errors);

                if(errors.hasErrors()) return null;
                else {
                    params.put(cp.getPropertyName(), lastGroup);
                }
            }
        }

        if(lastGroup!= null) {
            String remainingUri = uri.substring(uri.lastIndexOf(lastGroup)+lastGroup.length());
            if(remainingUri.length() > 0) {
                if(remainingUri.startsWith(SLASH))remainingUri = remainingUri.substring(1);
                String[] tokens = remainingUri.split(SLASH);
                for (int i = 0; i < tokens.length; i=i+2) {
                    String token = tokens[i];
                    if((i+1) < tokens.length) {
                        params.put(token, tokens[i+1]);
                    }

                }
            }
        }

        return new DefaultUrlMappingInfo(this.controllerName, this.actionName, params);
    }

    public String[] getLogicalMappings() {
        return this.urlData.getLogicalUrls(); 
    }

    /**
     * Compares this UrlMapping instance with the specified UrlMapping instance and deals with URL mapping precedence rules.
     *
     * @param o An instance of the UrlMapping interface
     * @return 1 if this UrlMapping should match before the specified UrlMapping. 0 if they are equal or -1 if this UrlMapping should match after the given UrlMapping
     */
    public int compareTo(Object o) {
        if(!(o instanceof UrlMapping)) throw new IllegalArgumentException("Cannot compare with Object ["+o+"]. It is not an instance of UrlMapping!");

        UrlMapping other = (UrlMapping)o;

        String[] otherTokens = other
                                   .getUrlData()
                                   .getTokens();


        String[] tokens = getUrlData().getTokens();
        
        if(tokens.length < otherTokens.length) {
            return -1;
        }
        else if(tokens.length > otherTokens.length) {
            return 1;
        }

        int result = 0;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if(otherTokens.length > i) {
                String otherToken = otherTokens[i];
                if(isWildcard(token) && !isWildcard(otherToken)) {
                    result = -1;
                    break;
                }
                else if(!isWildcard(token) && isWildcard(otherToken)) {
                    result = 1;
                    break;
                }                
            }
            else {
                break;
            }
        }
        return result;
    }

    private boolean isWildcard(String token) {
        return WILDCARD.equals(token) || CAPTURED_WILDCARD.equals(token);
    }
}
