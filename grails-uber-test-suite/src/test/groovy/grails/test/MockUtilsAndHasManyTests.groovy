package grails.test

import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.ApplicationHolder

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class MockUtilsAndHasManyTests extends GroovyTestCase {

    @Override
    protected void setUp() {
        ApplicationHolder.application = null
    }

    @Override
    protected void tearDown() {
        ApplicationHolder.application = null
    }

    void testMockDomainWithHasMany() {
        def test = new MagazineTests()
        test.setUp()
        test.testSomething()
    }
}

class MagazineTests extends GrailsUnitTestCase {

    @Override
    protected void tearDown() {
        ApplicationHolder.application = null
    }

    void testSomething() {
        mockDomain(Magazine)
        def magazine1 = new Magazine(articles: [new Article()]) // throws NPE in 1.2-M2, no exception in 1.1.1

        assertEquals 1, magazine1.articles.size()
    }
}

@Entity
class Magazine {
    static hasMany = [articles: Article]
}

@Entity
class Article {
    static belongsTo = [magazine: Magazine]
}
