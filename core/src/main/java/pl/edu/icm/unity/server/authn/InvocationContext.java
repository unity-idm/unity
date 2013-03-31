/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.io.Serializable;
import java.util.Locale;

import pl.edu.icm.unity.exceptions.RuntimeEngineException;

/**
 * Stores thread-local information about the current request metadata in thread local variable.
 * The thread-local variable should be set up by the binding authentication code. 
 * <p>
 * The data stored includes authenticated user's identity and the selected locale. 
 * @author K. Benedyczak
 */
public class InvocationContext implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static ThreadLocal<InvocationContext> threadLocal = new ThreadLocal<InvocationContext>();

	private AuthenticatedEntity authenticatedEntity;
	private Locale locale;
	
	public static void setCurrent(InvocationContext context)
	{
		threadLocal.set(context);
	}
	
	public static InvocationContext getCurrent()
	{
		InvocationContext ret = threadLocal.get();
		if (ret == null)
			throw new RuntimeEngineException("The current call has no invocation context set");
		return ret;
	}

	public AuthenticatedEntity getAuthenticatedEntity()
	{
		return authenticatedEntity;
	}

	public void setAuthenticatedEntity(AuthenticatedEntity authenticatedEntity)
	{
		this.authenticatedEntity = authenticatedEntity;
	}

	/**
	 * @return the locale
	 */
	public Locale getLocale()
	{
		return locale;
	}
	
	public void setLocale(Locale locale)
	{
		this.locale = locale;
	}
}
