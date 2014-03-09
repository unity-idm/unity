/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.internal.LoginSession;


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

	private LoginSession loginSession;
	private Locale locale;
	private Set<String> authenticatedIdentities = new LinkedHashSet<>();
	
	public static void setCurrent(InvocationContext context)
	{
		threadLocal.set(context);
	}
	
	public static InvocationContext getCurrent() throws InternalException
	{
		InvocationContext ret = threadLocal.get();
		if (ret == null)
			throw new InternalException("The current call has no invocation context set");
		return ret;
	}

	public LoginSession getLoginSession()
	{
		return loginSession;
	}

	public void setLoginSession(LoginSession loginSession)
	{
		this.loginSession = loginSession;
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

	public Set<String> getAuthenticatedIdentities()
	{
		return authenticatedIdentities;
	}

	public void addAuthenticatedIdentities(Collection<String> identity)
	{
		this.authenticatedIdentities.addAll(identity);
	}
}
