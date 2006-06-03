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
package org.codehaus.groovy.grails.commons;

import java.util.Properties;

import org.codehaus.groovy.grails.exceptions.DataSourceRequiredPropertyMissingException;

/**
 * 
 * 
 * @author Steven Devijver
 * @author Graeme Rocher
 *
 * @since Aug 6, 2005
 */
public class DefaultGrailsDataSource extends AbstractInjectableGrailsClass
		implements GrailsDataSource {

	public static final String DATA_SOURCE = "DataSource";
	private static final String DRIVER_CLASS_NAME = "driverClassName";
	private static final String URL = "url";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String POOLING = "pooling";
	private static final String DB_CREATE = "dbCreate";
	private static final String CONFIG_CLASS = "configClass";
	private static final String LOG_SQL = "logSql";
	private static final String DIALECT = "dialect";

    private boolean pooled = true;
	private String driverClassName = null;
	private String url = null;
	private String username = null;
	private String password = null;
	private String dbCreate = null;
    private Class configClass = null;
    private boolean sqlLogging = false;
    private Class dialect = null;

    public DefaultGrailsDataSource(Class clazz) {
        super(clazz, DATA_SOURCE);

        configClass = (Class)getPropertyValue(CONFIG_CLASS,Class.class);

        if (getPropertyValue(POOLING, boolean.class) != null) {
            this.pooled = getPropertyValue(POOLING, boolean.class).equals(Boolean.TRUE) ? true : false;
        }
        if (getPropertyValue(LOG_SQL, boolean.class) != null) {
            this.sqlLogging = getPropertyValue(LOG_SQL, boolean.class).equals(Boolean.TRUE) ? true : false;
        }        
        if(getPropertyValue(DIALECT,Class.class) != null) {
        	this.dialect =  (Class)getPropertyValue(DIALECT, Class.class); 
        }
        
        if (getPropertyValue(DB_CREATE, String.class) != null) {
            String _dbCreate = (String)getPropertyValue(DB_CREATE, String.class);
            if(_dbCreate.equals( "create-drop" ) || _dbCreate.equals("create") || _dbCreate.equals("update"))
                this.dbCreate = _dbCreate;
        }

        if (getPropertyValue(DRIVER_CLASS_NAME, String.class) != null) {
            this.driverClassName = (String)getPropertyValue(DRIVER_CLASS_NAME, String.class);
        } else {
            throw new DataSourceRequiredPropertyMissingException("Required property [" + DRIVER_CLASS_NAME + "] is missing on [" + getFullName() + "]!");
        }

        if (getPropertyValue(URL, String.class) != null) {
            this.url = (String)getPropertyValue(URL, String.class);
        } else {
            throw new DataSourceRequiredPropertyMissingException("Required property [" + URL + "] is missing on [" + getFullName() + "]!");
        }

        if (getPropertyValue(USERNAME, String.class) != null) {
            this.username = (String)getPropertyValue(USERNAME, String.class);
        } else {
            throw new DataSourceRequiredPropertyMissingException("Required property [" + USERNAME + "] is missing on [" + getFullName() + "]!");
        }

        if (getPropertyValue(PASSWORD, String.class) != null) {
            this.password = (String)getPropertyValue(PASSWORD, String.class);
        } else {
            throw new DataSourceRequiredPropertyMissingException("Required property [" + PASSWORD + "] is missing on [" + getFullName() + "]!");
        }
    }

	public boolean isPooled() {
		return this.pooled;
	}

	public String getDriverClassName() {
		return this.driverClassName;
	}

	public String getUrl() {
		return this.url;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public Properties getOtherProperties() {
		return null;
	}

	public String getDbCreate() {
		return dbCreate;
	}

    public Class getConfigurationClass() {
        return this.configClass;
    }

	public Class getDialect() {
		return this.dialect;
	}

	public boolean isLoggingSql() {
		return this.sqlLogging;
	}

}
