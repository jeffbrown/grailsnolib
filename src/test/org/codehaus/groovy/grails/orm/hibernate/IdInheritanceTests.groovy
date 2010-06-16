package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class IdInheritanceTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

abstract class GrandParent {}

@Entity
abstract class Parent extends GrandParent {
  String toString() {
    return "${id}"
  }
}

@Entity
class Child extends Parent {

  static transients = ['superId']

  Long getSuperId() {
    return super.id
  }
}
'''
    }


  void testDirectAccess() {
    def child = ga.getDomainClass("Child").newInstance()
    child.save('flush': true)

    assert 1 == child.id
  }

  void testInheritedMethodAccess() {
    def child = ga.getDomainClass("Child").newInstance()
    child.save('flush': true)

    assert "1" == child.toString()
  }
}
