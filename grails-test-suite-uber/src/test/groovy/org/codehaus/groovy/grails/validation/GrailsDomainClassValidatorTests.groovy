package org.codehaus.groovy.grails.validation

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.plugins.GrailsPlugin
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.springframework.validation.BindException

class GrailsDomainClassValidatorTests extends AbstractGrailsMockTests {

    void testCascadingValidation() {
        def bookClass = ga.getDomainClass("Book")
        def authorClass = ga.getDomainClass("Author")
        def addressClass = ga.getDomainClass("Address")

        def bookMetaClass = new ExpandoMetaClass(bookClass.clazz)
        def authorMetaClass = new ExpandoMetaClass(authorClass.clazz)
        def errorsProp = null
        def setter = { Object obj -> errorsProp = obj }

        bookMetaClass.setErrors = setter
        authorMetaClass.setErrors = setter
        bookMetaClass.initialize()
        authorMetaClass.initialize()

        def book = bookClass.newInstance()
        book.metaClass = bookMetaClass

        def bookValidator = new GrailsDomainClassValidator()
        def authorValidator = new GrailsDomainClassValidator()

        bookValidator.domainClass = bookClass
        bookValidator.messageSource = createMessageSource()
        bookValidator.grailsApplication = ga
        authorValidator.domainClass = authorClass
        authorValidator.messageSource = createMessageSource()
        authorValidator.grailsApplication = ga
        authorClass.validator = authorValidator

        def errors = new BindException(book, book.class.name)

        bookValidator.validate(book, errors, true)

        assert errors.hasErrors()

        book.title = "Foo"
        def author = authorClass.newInstance()
        author.metaClass = authorMetaClass
        book.author = author

        errors = new BindException(book, book.class.name)
        bookValidator.validate(book, errors, true)

        // it should validate here because even though the author properties are not set, validation doesn't cascade
        // because a Book belongs to an Author and the same way persistence doesn't cascade so too validation doesn't

        assert !errors.hasErrors()

        book.author.name = "Bar"
        book.author.address = addressClass.newInstance()
        errors = new BindException(book, book.class.name)
        bookValidator.validate(book, errors, true)

        assert !errors.hasErrors()

        book.author.address.location = "UK"
        book.author.address.author = book.author
        errors = new BindException(book, book.class.name)
        bookValidator.validate(book, errors, true)

        assert !errors.hasErrors()

        def book2 = bookClass.newInstance()
        book2.metaClass = bookMetaClass

        author.books.add(book2)

        errors = new BindException(author, author.class.name)
        authorValidator.validate(author, errors, true)
        assert errors.hasErrors()

        book2.title = "Bar 2"
        book2.author = author

        errors = new BindException(author, author.class.name)
        authorValidator.validate(author, errors, true)
        assert !errors.hasErrors()
    }

    protected void onSetUp() {
        PluginManagerHolder.pluginManager = new MockGrailsPluginManager()
        PluginManagerHolder.pluginManager.registerMockPlugin([getName: { -> 'hibernate' }] as GrailsPlugin)

        gcl.parseClass('''
class Book {
    Long id
    Long version
    String title
    Author author
    static belongsTo = Author
    static constraints = {
       title(blank:false, size:1..255)
       author(nullable:false)
    }
}
class Author {
    Long id
    Long version
    String name
    Address address
    Set books = new HashSet()
    static hasMany = [books:Book]
    static constraints = {
        address(nullable:false)
        name(size:1..255, blank:false)
    }
}
class Address {
    Long id
    Long version
    Author author
    String location
    static constraints = {
       author(nullable:false)
       location(blank:false)
    }
}
        ''')
    }

    protected void onTearDown() {
        PluginManagerHolder.pluginManager = null
    }
}
