package org.codehaus.groovy.grails.web.taglib

import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler

public class NamespacedNamedUrlMappingTests extends AbstractGrailsTagTests {
    void onInit() {
        def mappingClass = gcl.parseClass('''
class TestUrlMappings {
	static mappings = {
	  "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }

	  name productListing: "/products/list" {
	      controller = "product"
	      action = "list"
      }

      name productDetail: "/showProduct/$productName" {
          controller = "product"
          action = "show"
      }
    }
}
        ''')

        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, mappingClass)
    }

    void testNoParameters() {
        def template = '<link:productListing>Product Listing</link:productListing>'
        assertOutputEquals '<a href="/products/list">Product Listing</a>', template 
    }

    void testAttributesForParameters() {
        def template = '<link:productDetail productName="Scotch">Scotch Details</link:productDetail>'
        assertOutputEquals '<a href="/showProduct/Scotch">Scotch Details</a>', template 
    }

}