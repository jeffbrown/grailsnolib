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

/**
 * Gant script that runs the Grails unit tests
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import grails.util.GrailsUtil as GU;  
import grails.util.GrailsWebUtil as GWU
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.support.*
import java.lang.reflect.Modifier;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;        
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator as GRC;
import org.apache.tools.ant.taskdefs.optional.junit.*                        
import org.springframework.mock.web.*       
import org.springframework.core.io.* 
import org.springframework.web.context.request.RequestContextHolder;
import org.codehaus.groovy.grails.plugins.*

Ant.property(environment:"env")
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"  
grailsApp = null   
appCtx = null
result = new TestResult()
compilationFailures = []

includeTargets << new File ( "${grailsHome}/scripts/Package.groovy" )

task ('default': "Run a Grails applications unit tests") {
  depends( classpath, checkVersion, configureProxy, packagePlugins )
  grailsEnv = "test"                                                

	
  packageApp()
  testApp()
}

testDir = "${basedir}/test/reports"

def processResults = { 
	if(result) { 
		if(result.errorCount() > 0 || result.failureCount() > 0 || compilationFailures.size > 0) {
        	event("StatusFinal", ["Tests failed: ${result.errorCount()} errors, ${result.failureCount()} failures, ${compilationFailures.size} compilation errors. View reports in $testDir"])
			exit(1)
		}
		else {
        	event("StatusFinal", ["Tests passed. View reports in $testDir"])
			exit(0)
		}			       

	}  
	else {
        event("StatusFinal", ["Tests passed. View reports in $testDir"])
		exit(0)
	}	
}

task(testApp:"The test app implementation task") {

	Ant.mkdir(dir:testDir)
	Ant.mkdir(dir:"${testDir}/html")
	Ant.mkdir(dir:"${testDir}/plain")  
	

  	compileTests()  

	try {
		runUnitTests() 
		runIntegrationTests()
		produceReports()		
	}   
	finally {
		processResults()
	}
}
task(compileTests:"Compiles the test cases") {            
	def destDir = "${basedir}/test/classes"	
	Ant.mkdir(dir:destDir)
	try {                         
	    Ant.groovyc(destdir:destDir,
	            classpathref:"grails.classpath",
			    resourcePattern:"file:${basedir}/**/grails-app/**/*.groovy",
				compilerClasspath.curry(true))						
	}   
	catch(Exception e) {
	   event("StatusFinal", ["Compilation Error: ${e.message}"])
	   exit(1)
	}

	def rootLoader = getClass().classLoader.rootLoader
	rootLoader?.addURL(new File(destDir).toURL())
}    

task(produceReports:"Outputs aggregated xml and html reports") {
	Ant.junitreport {
		fileset(dir:testDir) {
			include(name:"TEST-*.xml")
		}
		report(format:"frames", todir:"${testDir}/html")
	}	
}
    
 
def populateTestSuite = { suite, testFiles, classLoader, ctx ->
	for(r in testFiles) {
	    try {
		    def c = classLoader.loadClass(r.filename - ".groovy") 
            if(TestCase.isAssignableFrom(c) && !Modifier.isAbstract(c.modifiers)) {
                suite.addTest(new GrailsTestSuite(ctx.beanFactory, c))
            }                                                  
			else {
				event("StatusUpdate", ["Test ${r.filename} is not a valid test case. It does not implement junit.framework.TestCase or is abstract!"])
			}
		} catch( Exception e ) {
            compilationFailures << r.file.name
			event("StatusFinal", ["Error loading test: ${e.message}"])
			exit(1)
        }
	}
}   
def runTests = { suite, TestResult result, Closure callback  ->
	for(test in suite.tests()) {
		new File("${testDir}/TEST-${test.name}.xml").withOutputStream { xmlOut ->
			new File("${testDir}/plain/TEST-${test.name}.txt").withOutputStream { plainOut ->
				def xmlOutput = new XMLJUnitResultFormatter(output:xmlOut)
				def plainOutput = new PlainJUnitResultFormatter(output:plainOut)
				def junitTest = new JUnitTest(test.name)


				plainOutput.startTestSuite(junitTest)
				xmlOutput.startTestSuite(junitTest)
                println "Running test ${test.name}..."
				for(i in 0..<test.testCount()) {
                    def thisTest = new TestResult()
                    thisTest.addListener(xmlOutput)
                    thisTest.addListener(plainOutput)
					def t = test.testAt(i)
					def start = System.currentTimeMillis()
					callback(test, {
                        print "                    ${t.name}..."   
						test.runTest(t, thisTest)					
					})
                    junitTest.setCounts(thisTest.runCount(), thisTest.failureCount(),
                                         thisTest.errorCount());
                    junitTest.setRunTime(System.currentTimeMillis() - start)
                    
					if(thisTest.errorCount() > 0 || thisTest.failureCount() > 0) {
						println "FAILURE"
						thisTest.errors().each { result.addError(t, it.thrownException())  }
						thisTest.failures().each { result.addFailure(t, it.thrownException()) }
					}
					else { println "SUCCESS"}

				}
				plainOutput.endTestSuite(junitTest)
				xmlOutput.endTestSuite(junitTest)				
			}
		}
	}
	
}   
task(runUnitTests:"Run Grails' unit tests under the test/unit directory") {         
   try {    
        def classLoader = getClass().classLoader	 

		classLoader.rootLoader.addURL(new File("${basedir}/grails-app/conf/hibernate").toURL())
		def beans = new grails.spring.BeanBuilder().beans {
			resourceHolder(org.codehaus.groovy.grails.commons.spring.GrailsResourceHolder) {
				resources = "file:${basedir}/**/grails-app/**/*.groovy"
			}
			grailsResourceLoader(org.codehaus.groovy.grails.commons.GrailsResourceLoaderFactoryBean) {
				grailsResourceHolder = resourceHolder
			}
			grailsApplication(org.codehaus.groovy.grails.commons.DefaultGrailsApplication.class, ref("grailsResourceLoader"))
		}
	                                                    
		appCtx = beans.createApplicationContext()
		def ctx = appCtx
		ctx.servletContext = new MockServletContext()
		grailsApp = ctx.grailsApplication  

		
        pluginManager = new DefaultGrailsPluginManager(pluginResources as Resource[], grailsApp)

           	PluginManagerHolder.setPluginManager(pluginManager)
           	pluginManager.loadPlugins()
			pluginManager.doArtefactConfiguration()
			grailsApp.initialise()                 		
				
           pluginManager.getGrailsPlugin("core")?.doWithDynamicMethods(appCtx)
           pluginManager.getGrailsPlugin("logging")?.doWithDynamicMethods(appCtx)

	                   
        def testFiles = resolveTestResources { "test/unit/${it}.groovy" }
            testFiles = testFiles.findAll { it.exists() }
            if(testFiles.size() == 0) {
                event("StatusUpdate", [ "No tests found in test/unit to execute"])
                return
            }

            classLoader.rootLoader.addURL(new File("test/unit").toURL())
            def suite = new TestSuite()
            populateTestSuite(suite, testFiles, classLoader, ctx)
			if(suite.testCount() > 0) {      
				int testCases = suite.countTestCases()
	            println "-------------------------------------------------------"
	            println "Running ${testCases} Unit Test${testCases > 1 ? 's' : ''}..."

	            def start = new Date()
	            runTests(suite,result) { test, invocation ->
	                    invocation()
	            }
	            def end = new Date()

	            event("StatusUpdate", [ "Unit Tests Completed in ${end.time-start.time}ms" ])
	            println "-------------------------------------------------------"				
			}
        }
        catch(Exception e) {
            event("StatusFinal", ["Error running unit tests: ${e.toString()}"])
            e.printStackTrace()
        }
}

task(runIntegrationTests:"Runs Grails' tests under the test/integration directory") {
	try {
    // allow user to specify test to run like this...
    //   grails test-app Author
    //   grails test-app AuthorController
    def testFiles = resolveTestResources { "test/integration/${it}.groovy" }

		if(testFiles.size() == 0) {
            event("StatusUpdate", [ "No tests found in test/integration to execute"])
			return
		}               
		
		

		def config = new org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator(grailsApp,appCtx)
		def ctx = config.configure(new MockServletContext())
		def app = ctx.getBean(GrailsApplication.APPLICATION_ID)
        if(app.parentContext == null) {
            app.applicationContext = ctx
        }
		def classLoader = app.classLoader
		def suite = new TestSuite()

   		populateTestSuite(suite, testFiles, classLoader, ctx)   
		if(suite.testCount() > 0) {      
			int testCases = suite.countTestCases()
            println "-------------------------------------------------------"
            println "Running ${testCases} Integration Test${testCases > 1 ? 's' : ''}..."


			def beanNames = ctx.getBeanNamesForType(PersistenceContextInterceptor)
			def interceptor = null
			if(beanNames.size() > 0)interceptor = ctx.getBean(beanNames[0])
           		

			try {
				interceptor?.init()           	
			   
				def start = new Date()			
	            runTests(suite, result) { test, invocation ->
	                name = test.name[0..-6]
					def webRequest = GWU.bindMockWebRequest(ctx)	  
				
					// @todo this is horrible and dirty, should find a better way  		
					if(name.endsWith("Controller")) {
						webRequest.controllerName = GCU.getLogicalPropertyName(name, "Controller")
					}                                                                           
				
					invocation()
					interceptor?.flush() 				
	 				RequestContextHolder.setRequestAttributes(null);
	                if( test.cleaningOrder ) {
	                    grails.util.GrailsUtil.deprecated "'cleaningOrder' property for integration tests is not in use anymore since now we make Hibernate manage the cleaning order. You can just remove it from your tests."
	                }
	                app.domainClasses.each { dc ->
	                    dc.clazz.list()*.delete()
	                }
					interceptor?.flush()
				}                     
				def end = new Date()
				println "Integration Tests Completed in ${end.time-start.time}ms"  		
				println "-------------------------------------------------------"		
			
			}                         		
			finally {
				interceptor?.destroy()
			}	
		}   
	}
	catch(Throwable e) {
        event("StatusUpdate", [ "Error executing tests ${e.message}"])
		e.printStackTrace(System.out)   
        event("StatusFinal", ["Error running tests: ${e.toString()}"])
		exit(1)
	}
}

def resolveTestResources(patternResolver) {
    def testNames = getTestNames(args)

    if (!testNames) {
      testNames = ['*']
    }
      
    def testResources = []
    testNames.each { 
      def testFiles = resolveResources(patternResolver(it)) 
      testResources.addAll(testFiles.findAll { it.exists() })
    }                     
    testResources
}

def getTestNames(testNamesString) {
  testNamesString ? testNamesString.tokenize().collect{ "${it}Tests" } : null
}
