/*
 * Copyright 2004-2006 Graeme Rocher
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
package org.codehaus.groovy.grails.plugins;

import java.io.File;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration;
import org.codehaus.groovy.grails.plugins.exceptions.PluginException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * @author Graeme Rocher
 * @since 0.4
 *
 */

public class MockGrailsPluginManager implements GrailsPluginManager {

	Map plugins = new HashMap();
	
	public void doPostProcessing(ApplicationContext applicationContext) {
		// do nothing
	}

	public void doRuntimeConfiguration(RuntimeSpringConfiguration springConfig) {
		// do nothing	
	}

	public GrailsPlugin getGrailsPlugin(String name) {
		return (GrailsPlugin)this.plugins.get(name);
	}

	public GrailsPlugin getGrailsPlugin(String name, BigDecimal version) {
		return (GrailsPlugin)this.plugins.get(name);
	}

	public boolean hasGrailsPlugin(String name) {
		return this.plugins.containsKey(name);
	}
	
	public void registerMockPlugin(GrailsPlugin plugin) {
		this.plugins.put(plugin.getName(), plugin);
	}

	public void loadPlugins() throws PluginException {
		// do nothing
	}

	public void doRuntimeConfiguration(String string, RuntimeSpringConfiguration springConfig) {
		// do nothing		
	}

	public void checkForChanges() {
		// do nothing		
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// do nothing
	}

	public void doWebDescriptor(Resource descriptor, Writer target) {
		// do nothing
	}

	public void doWebDescriptor(File descriptor, Writer target) {
		// do nothing
	}

	public boolean isInitialised() {
		return true;
	}

	public void setApplication(GrailsApplication application) {

	}

}
