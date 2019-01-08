/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.IdentityTaV;


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

	private static ThreadLocal<InvocationContext> threadLocal = new ThreadLocal<>();

	private LoginSession loginSession;
	private Locale locale;
	private IdentityTaV tlsIdentity;
	private AuthenticationRealm realm;
	private String currentURLUsed;
	private List<AuthenticationFlow> endpointFlows;

	/**
	 * @param tlsIdentity TLS client-authenticated identity (of X500 type) or null if there is no TLS 
	 * client connection context or it is not client authenticated.
	 */
	public InvocationContext(IdentityTaV tlsIdentity, AuthenticationRealm realm, List<AuthenticationFlow> endpointFlows)
	{
		this.endpointFlows = endpointFlows;
		setTlsIdentity(tlsIdentity);
		this.realm = realm;
	}
	
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
	
	public static boolean hasCurrent()
	{
		return threadLocal.get() != null;
	}
	
	/**
	 * @return current authentication realm's name or null if undefined/unknown.
	 */
	public static String safeGetRealm()
	{
		try
		{
			InvocationContext context = InvocationContext.getCurrent();
			AuthenticationRealm realm = context.getRealm();
			return realm != null ? realm.getName() : null;
		} catch (InternalException e)
		{
			//OK
		}
		return null;
	}
	
	public AuthenticationRealm getRealm()
	{
		return realm;
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

	/**
	 * @return the TLS authenticated identity if available or null
	 */
	public IdentityTaV getTlsIdentity()
	{
		return tlsIdentity;
	}

	/**
	 * Sets a TLS authenticated identity.
	 * @param tlsIdentity
	 */
	public void setTlsIdentity(IdentityTaV tlsIdentity)
	{
		this.tlsIdentity = tlsIdentity;
	}

	public List<AuthenticationFlow> getEndpointFlows()
	{
		return endpointFlows;
	}

	/**
	 * 
	 * @return the current URL which was used to trigger the current processing. Can be null. This is 
	 * set on best effort basis and is not suitable for authorization or other sensitive operations. Always
	 * check if not null.
	 */
	public String getCurrentURLUsed()
	{
		return currentURLUsed;
	}

	public void setCurrentURLUsed(String currentURLUsed)
	{
		this.currentURLUsed = currentURLUsed;
	}
}
