package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.TimestampType;

/**
 * @author Burt Beckwith
 */
class IllegalVersionTypeTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class StringVersion {
    Long id
    String version
    String name
}

class BooleanVersion {
    Long id
    Boolean version
    String name
}
        
class DateVersion {
    Long id
    Date version
    String name
}

class TimestampVersion {
    Long id
    java.sql.Timestamp version
    String name
}

class LongVersion {
    Long id
    long version
    String name
}

class IntVersion {
    Long id
    int version
    String name
}
'''
    }

    void testStringVersion() {
        def clazz = ga.getDomainClass('StringVersion').clazz
        assertNull sessionFactory.getEntityPersister(clazz.name).versionType
    }

    void testBooleanVersion() {
        def clazz = ga.getDomainClass('BooleanVersion').clazz
        assertNull sessionFactory.getEntityPersister(clazz.name).versionType
    }

    void testLongVersion() {
        def clazz = ga.getDomainClass('LongVersion').clazz
        assertTrue sessionFactory.getEntityPersister(clazz.name).versionType instanceof LongType
    }

    void testIntVersion() {
        def clazz = ga.getDomainClass('IntVersion').clazz
        assertTrue sessionFactory.getEntityPersister(clazz.name).versionType instanceof IntegerType
    }

    void testDateVersion() {
        def clazz = ga.getDomainClass('DateVersion').clazz
        assertTrue sessionFactory.getEntityPersister(clazz.name).versionType instanceof TimestampType
    }

    void testTimestampVersion() {
        def clazz = ga.getDomainClass('TimestampVersion').clazz
        assertTrue sessionFactory.getEntityPersister(clazz.name).versionType instanceof TimestampType
    }
}
