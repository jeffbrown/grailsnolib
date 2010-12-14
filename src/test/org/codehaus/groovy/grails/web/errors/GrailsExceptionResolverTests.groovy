package org.codehaus.groovy.grails.web.errors

import grails.util.GrailsWebUtil

import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator
import org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingsHolder
import org.codehaus.groovy.grails.web.mapping.UrlMappingsHolder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.multipart.commons.CommonsMultipartResolver
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.view.InternalResourceView

/**
 * Test case for {@link GrailsExceptionResolver}.
 */
class GrailsExceptionResolverTests extends GroovyTestCase {

    @Override
    protected void tearDown() {
        RequestContextHolder.setRequestAttributes null
    }

    void testGetRootCause() {
        def ex = new Exception()
        assertEquals ex, GrailsExceptionResolver.getRootCause(ex)

        def root = new Exception("root")
        ex = new RuntimeException(root)
        assertEquals root, GrailsExceptionResolver.getRootCause(ex)

        ex = new IllegalStateException(ex)
        assertEquals root, GrailsExceptionResolver.getRootCause(ex)

        shouldFail(NullPointerException) {
            GrailsExceptionResolver.getRootCause(null)
        }
    }

    void testResolveExceptionToView() {
        def resolver = new GrailsExceptionResolver()
        def mockContext = new MockServletContext()
        def mappings = new DefaultUrlMappingEvaluator(mockContext).evaluateMappings {
            "500"(view:"myView")
        }

        def urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        def mockCtx = new MockApplicationContext()
        def webRequest = GrailsWebUtil.bindMockWebRequest()

        mockCtx.registerMockBean UrlMappingsHolder.BEAN_ID, urlMappingsHolder
        mockCtx.registerMockBean "viewResolver", new DummyViewResolver()
        mockContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCtx

        resolver.servletContext = mockContext
        resolver.exceptionMappings = ['java.lang.Exception': '/error'] as Properties

        def ex = new Exception()
        def request = webRequest.currentRequest
        def response = webRequest.currentResponse
        def handler = new Object()
        def modelAndView = resolver.resolveException( request, response, handler, ex )

        assertNotNull "should have returned a ModelAndView", modelAndView
        assertEquals "/myView", modelAndView.view.url
    }

    void testResolveExceptionToController() {
        def resolver = new GrailsExceptionResolver()
        def mockContext = new MockServletContext()
        def mappings = new DefaultUrlMappingEvaluator(mockContext).evaluateMappings {
            "500"(controller:"foo", action:"bar")
        }

        def urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        def mockCtx = new MockApplicationContext()
        def webRequest = GrailsWebUtil.bindMockWebRequest()

        mockCtx.registerMockBean UrlMappingsHolder.BEAN_ID, urlMappingsHolder
        mockCtx.registerMockBean "viewResolver", new DummyViewResolver()
        mockCtx.registerMockBean GrailsApplication.APPLICATION_ID, new DefaultGrailsApplication()
        mockCtx.registerMockBean "multipartResolver", new CommonsMultipartResolver()
        mockContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCtx

        resolver.servletContext = mockContext
        resolver.exceptionMappings = ['java.lang.Exception': '/error'] as Properties

        def ex = new Exception()
        def request = webRequest.currentRequest
        MockHttpServletResponse response = webRequest.currentResponse
        def handler = new Object()
        def modelAndView = resolver.resolveException( request, response, handler, ex )

        assertNotNull "should have returned a ModelAndView", modelAndView
        assertTrue modelAndView.empty

        assertEquals "/grails/foo/bar.dispatch",response.getForwardedUrl()
    }

    void testResolveExceptionToControllerWhenResponseCommitted() {
        def resolver = new GrailsExceptionResolver()
        def mockContext = new MockServletContext()
        def mappings = new DefaultUrlMappingEvaluator(mockContext).evaluateMappings {
            "500"(controller:"foo", action:"bar")
        }

        def urlMappingsHolder = new DefaultUrlMappingsHolder(mappings)
        def mockCtx = new MockApplicationContext()
        def webRequest = GrailsWebUtil.bindMockWebRequest()

        mockCtx.registerMockBean UrlMappingsHolder.BEAN_ID, urlMappingsHolder
        mockCtx.registerMockBean "viewResolver", new DummyViewResolver()
        mockCtx.registerMockBean GrailsApplication.APPLICATION_ID, new DefaultGrailsApplication()
        mockCtx.registerMockBean "multipartResolver", new CommonsMultipartResolver()
        mockContext.setAttribute WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, mockCtx

        resolver.servletContext = mockContext
        resolver.exceptionMappings = ['java.lang.Exception': '/error'] as Properties

        def ex = new Exception()
        def request = webRequest.currentRequest
        MockHttpServletResponse response = webRequest.currentResponse
        def handler = new Object()
        response.setCommitted(true)
        def modelAndView = resolver.resolveException( request, response, handler, ex )

        assertNotNull "should have returned a ModelAndView", modelAndView
        assertFalse modelAndView.empty
    }
	
	void testLogRequest() {
		def request = new MockHttpServletRequest()
		request.setRequestURI("/execute/me")
		request.setMethod "GET"
		request.addParameter "foo", "bar"
		request.addParameter "one", "two"
		
		def msg = GrailsExceptionResolver.getRequestLogMessage(request)
		
		assertEquals '''Exception occurred when processing request: [GET] /execute/me - parameters:
foo: bar
one: two
Stacktrace follows:''' , msg
	}
}

class DummyViewResolver implements ViewResolver {
    View resolveViewName(String viewName, Locale locale) {
        new InternalResourceView(viewName)
    }
}
