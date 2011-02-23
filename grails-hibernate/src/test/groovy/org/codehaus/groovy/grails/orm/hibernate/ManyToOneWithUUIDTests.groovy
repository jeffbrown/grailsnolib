package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author graemerocher
 */
class ManyToOneWithUUIDTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
import grails.persistence.*


@Entity
class ManyToOneWithUUIDPage extends org.codehaus.groovy.grails.orm.hibernate.AbstractPage {
    String id

    static mapping = {
        id generator:'uuid'
    }
}

@Entity
class ManyToOneWithUUIDPageElement {
    static belongsTo = [page: ManyToOneWithUUIDPage]
}
''')
    }

    void testManyToOneWithUUIDAssociation() {
        def Page = ga.getDomainClass("ManyToOneWithUUIDPage").clazz
        def page = Page.newInstance()

        assert page.save(flush:true) != null

        def PageElement = ga.getDomainClass("ManyToOneWithUUIDPageElement").clazz

        def pe = PageElement.newInstance(page:page)
        assert pe.save()


        session.clear()

        pe = PageElement.get(pe.id)

        assert pe
        assert pe.page
    }

}
abstract class AbstractPage {}
