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
package org.codehaus.groovy.grails.web.plugins;

import org.codehaus.groovy.grails.plugins.support.GrailsPluginUtils
import org.codehaus.groovy.grails.web.errors.GrailsExceptionResolver;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsUrlHandlerMapping;
import org.codehaus.groovy.grails.web.servlet.mvc.SimpleGrailsController;
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver;
import org.codehaus.groovy.grails.beans.factory.UrlMappingFactoryBean;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.aop.framework.ProxyFactoryBean;

import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.codehaus.groovy.grails.commons.spring.*
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.*
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.context.request.RequestContextHolder as RCH
import org.springframework.web.context.WebApplicationContext;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.metaclass.*
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.web.servlet.*
import org.springframework.validation.Errors
import org.codehaus.groovy.grails.web.pages.GroovyPage
import org.codehaus.groovy.grails.web.metaclass.TagLibMetaClass
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * A plug-in that handles the configuration of controllers for Grails 
 * 
 * @author Graeme Rocher
 * @since 0.4
 */
class ControllersGrailsPlugin {

	def watchedResources = ["**/grails-app/controllers/*Controller.groovy",
	                        "**/grails-app/taglib/*TagLib.groovy"]
	
	def version = GrailsPluginUtils.getGrailsVersion()
	def dependsOn = [i18n:version]
	
	def doWithSpring = {
		exceptionHandler(GrailsExceptionResolver) {
			exceptionMappings = ['java.lang.Exception':'/error']
		}
		multipartResolver(CommonsMultipartResolver)
		def urlMappings = [:]
		grailsUrlMappings(UrlMappingFactoryBean) {
			mappings = urlMappings
		}
		simpleGrailsController(SimpleGrailsController.class) {
			grailsApplication = ref("grailsApplication", true)			
		}
		
	    
		jspViewResolver(GrailsViewResolver) {
			viewClass = org.springframework.web.servlet.view.JstlView.class
			prefix = GrailsApplicationAttributes.PATH_TO_VIEWS
		    suffix = ".jsp"
		}
		if(application.controllers) {
			def handlerInterceptors = []
				                           
			grailsUrlHandlerMapping(GrailsUrlHandlerMapping) {
				interceptors = handlerInterceptors
				mappings =  grailsUrlMappings				                
			}
			handlerMappingTargetSource(HotSwappableTargetSource, grailsUrlHandlerMapping)
			handlerMapping(ProxyFactoryBean) {
				targetSource = handlerMappingTargetSource
				proxyInterfaces = [org.springframework.web.servlet.HandlerMapping]
			}
					
		}
		
		// Go through all the controllers and configure them in spring with AOP proxies for auto-updates and
		// mappings in the urlMappings bean
		application.controllers.each { controller ->
			log.debug "Configuring controller $controller.fullName"
			if(controller.available) {
				configureAOPProxyBean.delegate = delegate
				configureAOPProxyBean(controller, "getController", org.codehaus.groovy.grails.commons.GrailsControllerClass.class, false)				
				controller.URIs.each { uri ->
					if(!urlMappings.containsKey(uri)) 
						urlMappings[uri] = "simpleGrailsController"
				}
			}
		}
		
		// Now go through tag libraries and configure them in spring too. With AOP proxies and so on
		application.grailsTabLibClasses.each { taglib ->
			configureAOPProxyBean.delegate = delegate
			configureAOPProxyBean(taglib, "getGrailsTagLibClass", org.codehaus.groovy.grails.commons.GrailsTagLibClass.class, true)
		}
	}
	
	def configureAOPProxyBean = { grailsClass, factoryMethod, proxyClass, singleton ->
		"${grailsClass.fullName}Class"(MethodInvokingFactoryBean) {
			targetObject = ref("grailsApplication", true)
			targetMethod = factoryMethod
			arguments = [grailsClass.fullName]
		}
		"${grailsClass.fullName}TargetSource"(HotSwappableTargetSource, ref("${grailsClass.fullName}Class"))
		
		"${grailsClass.fullName}Proxy"(ProxyFactoryBean) {
			targetSource = ref("${grailsClass.fullName}TargetSource")
			proxyInterfaces = [proxyClass]
		}
		"${grailsClass.fullName}"("${grailsClass.fullName}Proxy":"newInstance") { bean ->			
			bean.singleton = singleton
			bean.autowire = "byName"
		}		
			
	}
	
	def doWithWebDescriptor = { webXml ->
		def controllers = [] as HashSet
		def webflows = []
		def basedir = System.getProperty("base.dir")
		def grailsEnv = System.getProperty("grails.env")
		
		// first for all the watched resources for this controller that are controllers
		// create a servlet-mapping element that maps to the Grails dispatch servlet
	    plugin.watchedResources.each {
	        def match = it.filename =~ /(\w+)(Controller.groovy$)/
	        if(match) {
	            def controllerName = match[0][1]
	            controllerName = GCU.getPropertyName(controllerName)	            
	            controllers << controllerName
	        }
		}
		def mappingElement = webXml.'servlet-mapping'		
		controllers.each { c ->
			mappingElement + {
				'servlet-mapping' {
					'servlet-name'("grails")
					'url-pattern'("/${c}/*")
				}
			}
		}
		
		def filters = webXml.filter
		def filterMappings = webXml.'filter-mapping'
		
		def lastFilter = filters[filters.size()-1]
		def lastFilterMapping = filterMappings[filterMappings.size()-1]
		def charEncodingFilter = filterMappings.find { it.'filter-name'.text() == 'charEncodingFilter'}
		                                       
		// add the Grails web request filter
		lastFilter + {
			filter {
				'filter-name'('grailsWebRequest')
				'filter-class'('org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequestFilter')
			}
			if(grailsEnv == "development") {
				filter {
					'filter-name'('reloadFilter')
					'filter-class'('org.codehaus.groovy.grails.web.servlet.filter.GrailsReloadServletFilter')
				}				
			}			
		}        
		def grailsWebRequestFilter = {
			'filter-mapping' {						
				'filter-name'('grailsWebRequest')
				'url-pattern'("/*")						
			}			
		}		                    
		if(charEncodingFilter) {
			charEncodingFilter + grailsWebRequestFilter
		}                                              
		else {
			lastFilterMapping + grailsWebRequestFilter
		}
		// if we're in development environment first add a the reload filter
		// to the web.xml by finding the last filter and appending it after		
		if(grailsEnv == "development") {
			
			// now map each controller request to the filter
			controllers.each { c ->
				lastFilterMapping + {
					'filter-mapping' {						
						'filter-name'('reloadFilter')
						'url-pattern'("/${c}/*")						
					}
				}			
			}
			// now find the GSP servlet and allow viewing generated source in
			// development mode
			def gspServlet = webXml.servlet.find { it.'servlet-name'?.text() == 'gsp' }
			gspServlet.'servlet-class' + {
				'init-param' {
					description """
		              Allows developers to view the intermediade source code, when they pass
		                a spillGroovy argument in the URL.					
							"""					
					'param-name'('showSource')
					'param-value'(1)
				}
			}
		}   	   

	}
	
	/**
	 * This creates the difference dynamic methods and properties on the controllers. Most methods
	 * are implemented by looking up the current request from the RequestContextHolder (RCH) 
	 */
	 
	def registerCommonObjects(metaClass) {
	   	def paramsObject = {->			
			RCH.currentRequestAttributes().params	
		}
	    def flashObject = {->
				RCH.currentRequestAttributes().flashScope
		}
	   	def sessionObject = {->
			RCH.currentRequestAttributes().session			
		}
	   	def requestObject = {->
			RCH.currentRequestAttributes().currentRequest
		}
	   	def responseObject = {->
			RCH.currentRequestAttributes().currentResponse
		}
	   	def servletContextObject = {->
				RCH.currentRequestAttributes().servletContext		   
		}
	   	def grailsAttrsObject = {->
				RCH.currentRequestAttributes().attributes
		}
		
		   // the params object
		   metaClass.getParams = paramsObject
		   // the flash object
		   metaClass.getFlash = flashObject
		   // the session object
			metaClass.getSession = sessionObject
		   // the request object
			metaClass.getRequest = requestObject
		   // the servlet context
		   metaClass.getServletContext = servletContextObject
		   // the response object
			metaClass.getResponse = responseObject
		   // The GrailsApplicationAttributes object
		   metaClass.getGrailsAttributes = grailsAttrsObject
		   // The GrailsApplication object
		   metaClass.getGrailsApplication = {-> RCH.currentRequestAttributes().attributes.grailsApplication }
		
	}


	def doWithDynamicMethods = { ctx ->
	   	
		// add common objects and out variable for tag libraries
		def registry = InvokerHelper.getInstance().getMetaRegistry()
	   	application.tagLibs.each { taglib ->
	   		def metaClass = taglib.metaClass
	   		registerCommonObjects(metaClass)
				   		
	   		metaClass.throwTagError = { String message ->
	   			throw new org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException(message)
	   		}
	   		metaClass.getOut = {->
	   			RCH.currentRequestAttributes().out
	   		}
	   		metaClass.setOut = { Writer newOut ->
	   			RCH.currentRequestAttributes().out = newOut 
	   		}
	   		
	   		def adaptedMetaClass = new TagLibMetaClass(taglib.metaClass)
	   		
	   		registry.setMetaClass(taglib.clazz, adaptedMetaClass)
	   		ctx.getBean(taglib.fullName).metaClass = adaptedMetaClass
	   	}
		// add commons objects and dynamic methods like render and redirect to controllers
		application.controllers.each { controller ->
		   def metaClass = controller.metaClass
			registerCommonObjects(metaClass)
		   
			metaClass.getActionUri = {-> "/$controllerName/$actionName".toString()	}
			metaClass.getControllerUri = {-> "/$controllerName".toString()	}
		    metaClass.getTemplateUri = { String name ->
		    	def webRequest = RCH.currentRequestAttributes()
		    	webRequest.attributes.getTemplateUri(name, webRequest.currentRequest)
		    }
		    metaClass.getViewUri = { String name ->
		    	def webRequest = RCH.currentRequestAttributes()
		    	webRequest.attributes.getViewUri(name, webRequest.currentRequest)		    	
		    }
			metaClass.getActionName = {->
				RCH.currentRequestAttributes().actionName
			}
			metaClass.getControllerName = {->
				RCH.currentRequestAttributes().controllerName
			}
			
		    metaClass.log = LogFactory.getLog(controller.fullName)
		   		   
			metaClass.setErrors = { Errors errors ->
				RCH.currentRequestAttributes().setAttribute( GrailsApplicationAttributes.ERRORS, errors, 0)
			}
		    metaClass.getErrors = {->
		   		RCH.currentRequestAttributes().getAttribute(GrailsApplicationAttributes.ERRORS, 0)
		    }
			metaClass.setModelAndView = { ModelAndView mav ->
				RCH.currentRequestAttributes().setAttribute( GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0)
			}
		    metaClass.getModelAndView = {->
	   			RCH.currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)		    
		    }
		    metaClass.getChainModel = {->
		    	RCH.currentRequestAttributes().flashScope["chainModel"]
		    }
			metaClass.hasErrors = {->
				errors?.hasErrors() ? true : false
			}

			def redirect = new RedirectDynamicMethod()
			def chain = new ChainDynamicMethod()
			def render = new RenderDynamicMethod()
			def bind = new BindDynamicMethod()
			// the redirect dynamic method
			metaClass.redirect = { Map args ->
				redirect.invoke(delegate,args)
			} 
		    metaClass.chain = { Map args ->
		    	chain.invoke(delegate, args)
		    }
		    // the render method
		    metaClass.render = { String txt ->
				render.invoke(delegate, [txt] as Object[])
		    }
		    metaClass.render = { Map args ->
				render.invoke(delegate, [args] as Object[])
	    	}	
		    metaClass.render = { Closure c ->
				render.invoke(delegate, [c] as Object[])
	    	}		   
		    metaClass.render = { Map args, Closure c ->
				render.invoke(delegate, [args, c] as Object[])
		    }		   
		    // the bindData method
		    metaClass.bindData = { Object target, Object args ->
		    	bind.invoke(delegate, [target, args] as Object[])
		    }
		    metaClass.bindData = { Object target, Object args, List disallowed ->
		    	bind.invoke(delegate, [target, args, disallowed] as Object[])
		    }
		}
	}

	def doWithApplicationContext = { ctx ->
        def registry = InvokerHelper.getInstance().getMetaRegistry()

        application.domainClasses.each { domainClass ->
            def metaClass = registry.getMetaClass(domainClass.getClazz())

            if(metaClass instanceof DynamicMethodsMetaClass) {
                   metaClass.dynamicMethods.addDynamicConstructor(new DataBindingDynamicConstructor())
                   metaClass.dynamicMethods.addDynamicProperty(new SetPropertiesDynamicProperty())                                       	
            }
        }
	}
	
	def onChange = { event ->
		if(GCU.isControllerClass(event.source)) {
			log.debug("Controller ${event.source} changed. Reloading...")
			def context = event.ctx
			if(!context) {
				log.debug("Application context not found. Can't reload")
				return
			}
			boolean isNew = application.getController(event.source?.name) ? false : true
										
			def controllerClass = application.addControllerClass(event.source)
			
			def mappings = new Properties()
			application.controllers.each { c ->
				c.URIs.each { uri ->
				  mappings[uri] = SimpleGrailsController.APPLICATION_CONTEXT_ID
				}
			}
			
			def urlMappingsTargetSource = context.getBean(GrailsUrlHandlerMapping.APPLICATION_CONTEXT_TARGET_SOURCE)
			def urlMappings = new GrailsUrlHandlerMapping(applicationContext:context)
			urlMappings.mappings = mappings
			
            def interceptorNames = context.getBeanNamesForType(HandlerInterceptor.class)
            def webRequestInterceptors = context.getBeanNamesForType( WebRequestInterceptor.class)			
		
            HandlerInterceptor[] interceptors = new HandlerInterceptor[interceptorNames.size()+webRequestInterceptors.size()]
                
			def j = 0                                                                       
			for(i in 0..<interceptorNames.size()) {
				interceptors[i] = context.getBean(interceptorNames[i])
				j = i+1
			}
			for(i in 0..<webRequestInterceptors.size()) {
				j = i+j
				interceptors[j] = new WebRequestHandlerInterceptorAdapter(context.getBean(webRequestInterceptors[i]))
			}
         
			log.debug("Re-adding ${interceptors.length} interceptors to mapping")
			
			urlMappings.interceptors = interceptors
			urlMappings.initApplicationContext()
			
			urlMappingsTargetSource.swap(urlMappings)
			
			def controllerTargetSource = context.getBean("${controllerClass.fullName}TargetSource")
			controllerTargetSource.swap(controllerClass)
			
			if(isNew) {
				log.info "Re-generating web.xml file..."
				def webTemplateXml = resolver.getResource("/WEB-INF/web.template.xml")
				def webXml = resolver.getResource("/WEB-INF/web.xml")?.getFile()
				webXml?.withWriter { w ->
					manager.doWebDescriptor(webTemplateXml, w)
				}				
			}
			
		}
		else if(GCU.isTagLibClass(event.source)) {
			boolean isNew = application.getGrailsTagLibClass(event.source?.name) ? false : true
			def taglibClass = application.addTagLibClass(event.source)
			if(taglibClass) {
				if(isNew) {
					GrailsRuntimeConfigurator.registerTagLibrary(taglibClass, source.ctx)
				}
				else {
					// replace tag library bean
					def beanName = taglibClass.fullName
					def beans = beans {
						"$beanName"(taglibClass.getClazz()) { bean ->
							bean.autowire =  true
						}					
					}
					if(event.ctx) {
						event.ctx.registerBeanDefinition(beanName, beans.getBeanDefinition(beanName))
					}
				}
			}
		}
	}
}