/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

/**
 * Implementations provide a two way conversion of domain object from/to object
 * used to access DB.
 * 
 * @author K. Benedyczak
 */
public interface RDBMSObjectSerializer<APP, BEAN>
{
	BEAN toDB(APP object);
	APP fromDB(BEAN bean);
}
