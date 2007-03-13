/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.metaclass;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethodInvocation;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethods;
import org.codehaus.groovy.grails.commons.metaclass.CreateDynamicMethod;
import org.codehaus.groovy.grails.commons.metaclass.WeakGenericDynamicProperty;
import org.codehaus.groovy.grails.orm.hibernate.metaclass.*;
import org.codehaus.groovy.grails.orm.support.TransactionManagerAware;
import org.codehaus.groovy.grails.validation.metaclass.ConstraintsDynamicProperty;
import org.hibernate.SessionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.Errors;

import java.beans.IntrospectionException;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 *
 *
 * @author Steven Devijver
 * @since Aug 7, 2005
 */
public class DomainClassMethods extends AbstractDynamicMethods implements TransactionManagerAware {

    public static final String HAS_ERRORS_METHOD = "hasErrors";
    public static final Pattern HAS_ERRORS_METHOD_PATTERN = Pattern.compile('^'+HAS_ERRORS_METHOD+'$');

    public static final String ERRORS_PROPERTY = "errors";

    public DomainClassMethods(GrailsApplication application, Class theClass, SessionFactory sessionFactory, ClassLoader classLoader)
            throws IntrospectionException {
        super(theClass);
        // dynamic methods
        addDynamicMethodInvocation(new IdentDynamicMethod(application));
        addDynamicMethodInvocation(new SavePersistentMethod(sessionFactory, classLoader,application));
        addDynamicMethodInvocation(new MergePersistentMethod(sessionFactory, classLoader,application));
        addDynamicMethodInvocation(new DiscardPersistentMethod(sessionFactory, classLoader));
        addDynamicMethodInvocation(new DeletePersistentMethod(sessionFactory, classLoader));
        addDynamicMethodInvocation(new RefreshPersistentMethod(sessionFactory, classLoader));
        addDynamicMethodInvocation(new ValidatePersistentMethod(sessionFactory, classLoader, application ));
        addDynamicMethodInvocation(new AbstractDynamicMethodInvocation(HAS_ERRORS_METHOD_PATTERN) {
            public Object invoke(Object target, String methodName, Object[] arguments) {
                Errors errors = (Errors)getDynamicProperty(ERRORS_PROPERTY).get(target);
                if(errors == null || !errors.hasErrors()) {
                    return Boolean.FALSE;
                }
                else {
                    return Boolean.TRUE;
                }
            }
        });

        // static methods
        addStaticMethodInvocation(new FindAllPersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new FindAllByPersistentMethod(application,sessionFactory, classLoader));
        addStaticMethodInvocation(new FindByPersistentMethod(application,sessionFactory, classLoader));
        addStaticMethodInvocation(new CountByPersistentMethod(application,sessionFactory, classLoader));
        addStaticMethodInvocation(new FindPersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new ListOrderByPersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new ListPersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new FindWherePersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new FindAllWherePersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new GetPersistentMethod(application,sessionFactory, classLoader));
        addStaticMethodInvocation(new GetAllPersistentMethod(application,sessionFactory, classLoader));
        addStaticMethodInvocation(new ExistsPersistentMethod(application,sessionFactory, classLoader));
        addStaticMethodInvocation(new CountPersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new CreateCriteriaPersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new ExecuteQueryPersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new ExecuteUpdatePersistentMethod(sessionFactory, classLoader));
        addStaticMethodInvocation(new CreateDynamicMethod());
        addStaticMethodInvocation(new WithCriteriaDynamicPersistentMethod(sessionFactory,classLoader));
        addStaticMethodInvocation(new WithTransactionPersistentMethod(sessionFactory,classLoader));

        // add dynamic properties
        addDynamicProperty( new ConstraintsDynamicProperty(application) );
        addDynamicProperty( new WeakGenericDynamicProperty(ERRORS_PROPERTY, Errors.class,null,false) );

    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        for (Iterator i = staticMethodInvocations.iterator(); i.hasNext();) {
            Object method = i.next();
            if(method instanceof TransactionManagerAware) {
                ((TransactionManagerAware)method).setTransactionManager(transactionManager);
            }
        }
    }

}
