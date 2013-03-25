/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.exceptions.RuntimeEngineException;

/**
 * Stores information about the authenticated user in thread local variable.
 * 
 * The thread-local variable should be set up by the binding authentication code. 
 * @author K. Benedyczak
 */
public class AuthenticationContext
{
	private static ThreadLocal<AuthenticationContext> threadLocal = new ThreadLocal<AuthenticationContext>();

	private long entityId;
	
	public AuthenticationContext(long entityId)
	{
		super();
		this.entityId = entityId;
	}

	public static void setCurrent(AuthenticationContext context)
	{
		threadLocal.set(context);
	}
	
	public static AuthenticationContext getCurrent()
	{
		AuthenticationContext ret = threadLocal.get();
		if (ret == null)
			throw new RuntimeEngineException("The current call has no authentication context set");
		return ret;
	}

	public long getEntityId()
	{
		return entityId;
	}
}
