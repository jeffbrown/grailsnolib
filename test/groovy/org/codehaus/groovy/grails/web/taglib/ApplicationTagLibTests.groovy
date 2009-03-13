package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.commons.ApplicationHolder
import javax.servlet.http.Cookie
import org.springframework.mock.web.MockHttpServletResponse
import javax.servlet.http.HttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib

class ApplicationTagLibTests extends AbstractGrailsTagTests {

    void testResourceTag() {
        request.contextPath = '/test'
        def template = '${resource(file:"images/foo.jpg")}' 

        assertOutputEquals '/test/images/foo.jpg', template

        template = '${resource(dir:"images",file:"foo.jpg")}'

        assertOutputEquals '/test/images/foo.jpg', template   
    }

    void testUseJessionIdWithCreateLink() {
        def response = new JsessionIdMockHttpServletResponse()
        ApplicationTagLib.metaClass.getResponse = {-> response}      
        def tagLibBean = appCtx.getBean("org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib")
        ga.config.grails.views.enable.jsessionid=true
        tagLibBean.afterPropertiesSet()
        assertTrue( tagLibBean.@useJsessionId )

        def template = '<g:createLink controller="foo" action="test" />'

        assertOutputEquals "/foo/test;jsessionid=test", template

        ga.config.grails.views.enable.jsessionid=false
        tagLibBean.afterPropertiesSet()
        assertFalse( tagLibBean.@useJsessionId )



        assertOutputEquals "/foo/test", template

    }


  void testObtainCookieValue() {
        def cookie = new Cookie("foo", "bar")
        request.cookies = [cookie] as Cookie[]

        def template = '<g:cookie name="foo" />'

        assertOutputEquals "bar", template

        template = '${cookie(name:"foo")}'

        assertOutputEquals "bar", template

    }

    void testObtainHeaderValue() {
        request.addHeader "FOO", "BAR"
        def template = '<g:header name="FOO" />'

        assertOutputEquals "BAR", template

        template = '${header(name:"FOO")}'

        assertOutputEquals "BAR", template

    }


    void testClonedUrlFromVariable() {
        def template = '''<g:set var="urlMap" value="${[controller: 'test', action: 'justdoit']}"/>${urlMap.controller},${urlMap.action}<g:link url="${urlMap}">test</g:link>${urlMap.controller},${urlMap.action}'''

        assertOutputEquals('test,justdoit<a href="/test/justdoit">test</a>test,justdoit', template)
    }

    void testLinkWithMultipleParameters() {
        def template = '<g:link controller="foo" action="action" params="[test: \'test\', test2: \'test2\']">test</g:link>'

        assertOutputEquals('<a href="/foo/action?test=test&amp;test2=test2">test</a>', template)
    }

    void testLinkWithFragment() {
        def template = '<g:link controller="foo" action="bar" fragment="test">link</g:link>'
        profile("link rendering") {
            assertOutputEquals('<a href="/foo/bar#test">link</a>', template)
        }
    }

    void testCreateLinkWithFlowExecutionKeyAndEvent() {
        request.flowExecutionKey = '12345'

        def template = '<g:createLink controller="foo" action="bar" event="boo" />'

        assertOutputEquals('/foo/bar?execution=12345&_eventId=boo', template)

    }

    void testLinkWithFlowExecutionKeyAndEvent() {
        request.flowExecutionKey = '12345'

        def template = '<g:link controller="foo" action="bar" event="boo" >link</g:link>'

        assertOutputEquals('<a href="/foo/bar?execution=12345&amp;_eventId=boo">link</a>', template)

    }


    void testSetTag() {
        def template = '<g:set var="one" value="two" />one: ${one}'

        assertOutputEquals('one: two', template)	
	}
	
	void testSetTagWithBody() {
        def template = '<g:set var="one">two</g:set>one: ${one}'

        assertOutputEquals('one: two', template)		
	}

	void testSetTagWithMap() {
        def template = '<g:set var="e" value="${c.a}"/>${e?.b}'

        assertOutputEquals('null', template, [c:[:]])
        assertOutputEquals('foo', template, [c:[a:[b:'foo']]])

    }

    
	void testIteration() {
        def template = '''<g:set var="counter" value="${1}" />
<g:each in="${[10,11,12]}" var="myVal"><g:set var="counter" value="${myVal+counter}" />${counter}</g:each>'''

        printCompiledSource template 
        assertOutputEquals('112234', template, [:], { it.toString().trim() })
    }
	
	void testMetaTag() {
        def template = '<g:meta name="app.version"/>'

        assertOutputEquals('0.9.9.1', template)
	}

    void testCreateLinkToWithDirAndLeadingSlash() {
        def template = '<g:createLinkTo dir="/images" file="foo.jpg" />'

        assertOutputEquals "/images/foo.jpg", template
    }

    void testCreateLinkToWithDirAndLeadingNoLeadingSlash() {
        def template = '<g:createLinkTo dir="images" file="foo.jpg" />'

        assertOutputEquals "/images/foo.jpg", template
    }

    void testCreateLinkToWithFileAndLeadingSlash() {
        def template = '<g:createLinkTo dir="/images" file="/foo.jpg" />'

        assertOutputEquals "/images/foo.jpg", template
    }

    void testCreateLinkTo() {
		StringWriter sw = new StringWriter();
		withTag("createLinkTo", sw) { tag ->
			def attrs = [dir:'test']
			tag.call( attrs )
			assertEquals '/test', sw.toString()

			sw.getBuffer().delete(0,sw.getBuffer().length());
			attrs = [dir:'test',file:'file']
			tag.call( attrs )
			assertEquals '/test/file', sw.toString()

			sw.getBuffer().delete(0,sw.getBuffer().length());
			attrs = [dir:'']
			tag.call( attrs )
			println sw.toString()
			assertEquals '', sw.toString()
		}
	}

    void testCreateLinkToFilesInRoot() {
		StringWriter sw = new StringWriter();
		withTag("createLinkTo", sw) { tag ->
			def attrs = [dir:'/', file:'test.gsp']
			tag.call( attrs )
			assertEquals '/test.gsp', sw.toString()
		}
    }

    void testCreateLinkToFilesInRootWithContext() {
		StringWriter sw = new StringWriter();
        request.contextPath = "/foo"
        withTag("createLinkTo", sw) { tag ->
			def attrs = [dir:'/', file:'test.gsp']
			tag.call( attrs )
			assertEquals '/foo/test.gsp', sw.toString()
		}
    }

    void testCreateLinkWithZeroId() {
        // test case for GRAILS-1123
        StringWriter sw = new StringWriter();
        withTag("createLink", sw) { tag ->
            def attrs = [action:'testAction', controller: 'testController', id:0]
            tag.call( attrs )
            assertEquals '/testController/testAction/0', sw.toString()
        }
    }

	void testCreateLinkURLEncoding() {
		StringWriter sw = new StringWriter();
		withTag("createLink", sw) { tag ->
			// test URL encoding. Params unordered to have to try one test at a time
			def attrs = [action:'testAction', controller: 'testController',
			    params:['name':'Marc Palmer']]
			tag.call( attrs )
			assertEquals '/testController/testAction?name=Marc+Palmer', sw.toString()
		}
	}

	void testCreateLinkURLEncodingWithHTMLChars() {
		StringWriter sw = new StringWriter();
		withTag("createLink", sw) { tag ->
			// test URL encoding is done but HTML encoding isn't, only want the one here.
			def attrs = [action:'testAction', controller: 'testController',
			    params:['email':'<marc@anyware.co.uk>']]
			tag.call( attrs )
			assertEquals '/testController/testAction?email=%3Cmarc%40anyware.co.uk%3E', sw.toString()
		}
	}

	void testCreateLinkWithBase() {
		StringWriter sw = new StringWriter();
		withTag("createLink", sw) { tag ->
			// test URL encoding. Params unordered to have to try one test at a time
			def attrs = [base:"http://www128.myhost.com:3495", action:'testAction', controller: 'testController']
			tag.call( attrs )
			assertEquals 'http://www128.myhost.com:3495/testController/testAction', sw.toString()
		}
	}

    void testAbsoluteWithContextPath() {
        request.contextPath = "/foo"
        def template = '<g:createLink action="testAction" controller="testController" absolute="true" />'

        assertOutputEquals 'http://localhost:8080/foo/testController/testAction', template    
    }
    
    /**
     * Tests regression of <a href="http://jira.codehaus.org/browse/GRAILS-3368">GRAILS-3368</a>.
     * The context path should not be included in the generated link
     * if "base" is set to an empty string.
     */
    void testCreateLinkWithNoContextPath() {
        StringWriter sw = new StringWriter();
        withTag("createLink", sw) { tag ->
            def attrs = [base: "", action:'testAction', controller: 'testController']
            tag.call( attrs )
            assertEquals '/testController/testAction', sw.toString()
        }
    }

	void testCreateLinkWithAbsolute() {
		StringWriter sw = new StringWriter();
		withTag("createLink", sw) { tag ->
			// test URL encoding. Params unordered to have to try one test at a time
			def attrs = [absolute:"true", action:'testAction', controller: 'testController']
			tag.call( attrs )
			assertEquals 'http://localhost:8080/testController/testAction', sw.toString()
		}
	}

}
class JsessionIdMockHttpServletResponse extends MockHttpServletResponse {

  public String encodeURL(String url) {
    return super.encodeURL("$url;jsessionid=test");
  }

}