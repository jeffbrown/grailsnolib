package grails.util;

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.springframework.mock.web.MockHttpServletResponse;

import junit.framework.TestCase;

public class JSonBuilderTest extends TestCase {

	private HttpServletResponse getResponse(Writer writer) {
		final PrintWriter printer = new PrintWriter(writer);
		return new MockHttpServletResponse() {
			public PrintWriter getWriter() throws UnsupportedEncodingException {
				return printer;
			}
		};
	}	
	
	private void parse(String groovy) throws Exception {
		GroovyClassLoader cl = new GroovyClassLoader();
		Class clazz = cl.parseClass("import grails.util.*; class TestClass { List names = [\"Steven\", \"Hans\", \"Erwin\"]; @Property Closure test = { response -> new JSonBuilder(response)." + groovy + "; } }");
		GroovyObject go = (GroovyObject)clazz.newInstance();
		Closure closure = (Closure)go.getProperty("test");
		StringWriter sw = new StringWriter();
		closure.call(getResponse(sw));
		System.out.println(sw.getBuffer().toString());
	}
	
	public void testOpenRicoBuilderElement() throws Exception {
		
		parse("json(){ message('Hello World') }");
		
		parse("json{ message 'Hello World' }");
		
		parse("json{ integer 5+5 }");
		
		parse("json{ message 5.1 }");		
		
		parse("json(){ names{ for( cc in names ){ name( \"firstName\" : cc ) }  }  }");

		try {
			parse("json{ message( \"Hello World\" ){ item() } }");
			fail();
		} catch (InvokerInvocationException e) {
			// expected
		}

		try {
			parse("json{ message( \"Hello World\" ){ item(\"test\") } }");		
			fail();
		} catch (InvokerInvocationException e) {
			// expected
		}
		
	}
	
}
