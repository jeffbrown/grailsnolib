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
package grails.util;

import java.lang.reflect.Modifier;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.GrailsTestSuite;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 
 * 
 * @author Steven Devijver
 * @since Aug 8, 2005
 */
public class RunTests {

	private static Log log = LogFactory.getLog(RunTests.class);
	
	public static void main(String[] args) {
		ConfigurableApplicationContext appCtx = (ConfigurableApplicationContext)GrailsUtil.bootstrapGrailsFromClassPath();
		GrailsApplication application = (GrailsApplication)appCtx.getBean(GrailsApplication.APPLICATION_ID);
		
		Class[] allClasses = application.getAllClasses();
        log.debug("Going through ["+allClasses.length+"] classes");
        TestSuite s = new TestSuite();
		for (int i = 0; i < allClasses.length; i++) {
			Class clazz = allClasses[i];
			if (TestCase.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
				log.debug("Adding test [" + clazz.getName() + "]");
				s.addTest(new GrailsTestSuite(appCtx.getBeanFactory(), clazz));
			} else {
				log.debug("[" + clazz.getName() + "] is not a test case.");
			}
		}
		TestRunner.run(s);
	}
}
