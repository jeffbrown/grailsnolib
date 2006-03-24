/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import groovy.lang.MissingMethodException;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Example;
import org.springframework.orm.hibernate3.HibernateCallback;
/**
 * <p>The "find" persistent static method allows searching for instances using either an example instance or an HQL 
 * query. This method returns the first result of the query. A GrailsQueryException is thrown if the query is not a valid query for the domain class.
 * 
 * <p>Examples in Groovy:
 * <code>
 * 		// retrieve the first 10 accounts ordered by account number
 * 		def a = Account.find("from Account as a order by a.number asc", 10)
 * 
 * 		// with query parameters
 * 		def a  = Account.find("from Account as a where a.number = ? and a.branch = ?", [38479, "London"]) 
 * 
 * 		// query by example
 * 		def a = new Account()
 * 		a.number = 495749357
 * 		def a = Account.find(a)
 * 
 * </code>
 * 
 * @author Graeme Rocher
 * @since 31-Aug-2005
 *
 */
public class FindPersistentMethod extends AbstractStaticPersistentMethod {

	private static final String METHOD_PATTERN = "^find$";

	public FindPersistentMethod(SessionFactory sessionFactory, ClassLoader classLoader) {
		super(sessionFactory, classLoader, Pattern.compile(METHOD_PATTERN));
	}

	protected Object doInvokeInternal(final Class clazz, String methodName,
			final Object[] arguments) {
		
		if(arguments.length == 0)
			throw new MissingMethodException(methodName,clazz,arguments);
		
		final Object arg = arguments[0];
		
		// if the arg is an instance of the class find by example
		if(arg instanceof String) {
			final String query = (String)arg;
			if(!query.matches( "from "+clazz.getName()+".*" )) {
				throw new GrailsQueryException("Invalid query ["+query+"] for domain class ["+clazz+"]");
			}			
			return super.getHibernateTemplate().execute( new HibernateCallback() {

				public Object doInHibernate(Session session) throws HibernateException, SQLException {										
					Query q = session.createQuery(query);
					Object[] queryArgs = null;
					int max = -1;
					if(arguments.length > 1) {
						if(arguments[1] instanceof List) {
							queryArgs = ((List)arguments[1]).toArray();
						}
						else if(arguments[1].getClass().isArray()) {
							queryArgs = (Object[])arguments[1];
						}
					}					
					max = retrieveMaxValue(arguments);
					if(queryArgs != null) {					
						for (int i = 0; i < queryArgs.length; i++) {
							q.setParameter(i, queryArgs[i]);
						}
					}
					if(max > -1) {
						q.setMaxResults(max);
					}
					List results = q.list();
					if(results.size() > 0)
						return results.get(0);
					return null;
				}

				private int retrieveMaxValue(Object[] arguments) {
					int max = -1;
					if(arguments.length > 1) {
						if(arguments[1] instanceof Integer) {
							max = ((Integer)arguments[1]).intValue();
						}
						if(arguments.length > 2) {
							if(arguments[2] instanceof Integer) {
								max = ((Integer)arguments[2]).intValue();
							}							
						}
					}
					
					return max;
				}
				
			});						
		}
		if(clazz.isAssignableFrom( arg.getClass() )) {			
			return super.getHibernateTemplate().executeFind( new HibernateCallback() {

				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					
					Example example = Example.create(arg)
							.ignoreCase();
					
					Criteria crit = session.createCriteria(clazz);
					crit.add(example);
					
					List results = crit.list();
					if(results.size() > 0)
						return results.get(0);
					return null;					
				}
				
			});			
		}

		throw new MissingMethodException(methodName,clazz,arguments);
	}

}
