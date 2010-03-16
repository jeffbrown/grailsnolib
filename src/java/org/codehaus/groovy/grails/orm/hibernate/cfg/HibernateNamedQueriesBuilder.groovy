/*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.cfg

import org.codehaus.groovy.grails.orm.hibernate.metaclass.*
import org.codehaus.groovy.grails.plugins.orm.hibernate.HibernatePluginSupport;

/**
 * A builder that implements the ORM named queries DSL

 * @author Jeff Brown
 *
 */

class HibernateNamedQueriesBuilder {

    private final domainClass
    private final dynamicMethods

    /**
     * @param domainClass the GrailsDomainClass defining the named queries
     * @param grailsApplication a GrailsApplication instance
     * @param ctx the main spring application context
     */
    HibernateNamedQueriesBuilder(domainClass, grailsApplication, ctx) {
		this.domainClass = domainClass

        def classLoader = grailsApplication.classLoader
        def sessionFactory = ctx.getBean('sessionFactory')

        dynamicMethods = [
                new FindAllByBooleanPropertyPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new FindAllByPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new FindByPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new FindByBooleanPropertyPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new CountByPersistentMethod(grailsApplication, sessionFactory, classLoader),
                new ListOrderByPersistentMethod(sessionFactory, classLoader)
        ]

    }

	def evaluate(Closure namedQueriesClosure) {
        def closure = namedQueriesClosure.clone()
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = this
        closure.call()
    }

    private handleMethodMissing = {String name, args ->
        def propertyName = name[0].toUpperCase() + name[1..-1]
        domainClass.metaClass.static."get${propertyName}" = {->
            // creating a new proxy each time because the proxy class has
            // some state that cannot be shared across requests (namedCriteriaParams)
            new NamedCriteriaProxy(criteriaClosure: args[0], domainClass: domainClass, dynamicMethods: dynamicMethods)
        }
    }

    def methodMissing(String name, args) {
        if (args && args[0] instanceof Closure) {
            return handleMethodMissing(name, args)
        }
        throw new MissingMethodException(name, HibernateNamedQueriesBuilder, args)
    }
}

class NamedCriteriaProxy {

    private criteriaClosure
    private domainClass
    private dynamicMethods
    private namedCriteriaParams

    def list(Object[] params, Closure additionalCriteriaClosure = null) {
        def closureClone = getPreparedCriteriaClosure()
        def listClosure = {
            closureClone.delegate = delegate
            def paramsMap
            if (params && params[-1] instanceof Map) {
                paramsMap = params[-1]
            }
            closureClone()
			if(additionalCriteriaClosure) {
				additionalCriteriaClosure = additionalCriteriaClosure.clone()
				additionalCriteriaClosure.delegate = delegate
				additionalCriteriaClosure()
			}
            if (paramsMap?.max) {
                maxResults(paramsMap.max)
            }
            if (paramsMap?.offset) {
                firstResult paramsMap.offset
            }
        }
        domainClass.clazz.withCriteria(listClosure)
    }

    def call(Object[] params) {
		if(params && params[-1] instanceof Closure) {
			def additionalCriteriaClosure = params[-1]
			params = params.length > 1 ? params[0..-2] : [:]
			list(params, additionalCriteriaClosure)
		} else {
			namedCriteriaParams = params
			this
		}
    }

    def get(id) {
        def closureClone = getPreparedCriteriaClosure()
		id = HibernatePluginSupport.convertValueToIdentifierType(domainClass, id)
        def getClosure = {
            closureClone.delegate = delegate
            closureClone()
            eq 'id', id
            uniqueResult = true
        }
        domainClass.clazz.withCriteria(getClosure)
    }

    def count(Closure additionalCriteriaClosure = null) {
        def closureClone = getPreparedCriteriaClosure()
        def countClosure = {
            closureClone.delegate = delegate
            closureClone()
			if(additionalCriteriaClosure) {
				additionalCriteriaClosure = additionalCriteriaClosure.clone()
				additionalCriteriaClosure.delegate = delegate
				additionalCriteriaClosure()
			}
            uniqueResult = true
            projections {
                rowCount()
            }
        }
        domainClass.clazz.withCriteria(countClosure)
    }

    def findWhere(params) {
        findAllWhere(params, true)
    }

    def findAllWhere(Map params, Boolean uniq = false) {
        def closureClone = getPreparedCriteriaClosure()
        def queryClosure = {
            closureClone.delegate = delegate
            closureClone()
            params.each {key, val ->
                eq key, val
            }
            if (uniq) {
                maxResults 1
                uniqueResult = true
            }
        }
        domainClass.clazz.withCriteria(queryClosure)
    }


    def methodMissing(String methodName, args) {

        def method = dynamicMethods.find {it.isMethodMatch(methodName)}

        if (method) {
            def preparedClosure = getPreparedCriteriaClosure()
            return method.invoke(domainClass.clazz, methodName, preparedClosure, args)
        }
        throw new MissingMethodException(methodName, NamedCriteriaProxy, args)
    }

    private getPreparedCriteriaClosure() {
        def closureClone = criteriaClosure.clone()
		closureClone.resolveStrategy = Closure.DELEGATE_FIRST
        if (namedCriteriaParams) {
            closureClone = closureClone.curry(namedCriteriaParams)
        }
        closureClone
    }
}
