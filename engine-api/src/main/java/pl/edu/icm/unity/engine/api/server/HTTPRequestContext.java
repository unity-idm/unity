/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.server;

import pl.edu.icm.unity.base.exceptions.InternalException;

/**
 * Stores in thread local state related to the HTTP request being served by the thread.
 */
public class HTTPRequestContext
{
	private static ThreadLocal<HTTPRequestContext> threadLocal = new ThreadLocal<>();
	private final String clientIP;
	private final String userAgent;
	
	public HTTPRequestContext(String clientIP, String userAgent)
	{
		this.clientIP = clientIP;
		this.userAgent = userAgent;
	}

	public static void setCurrent(HTTPRequestContext context)
	{
		threadLocal.set(context);
	}
	
	public static HTTPRequestContext getCurrent() throws InternalException
	{
		return threadLocal.get();
	}

	public String getClientIP()
	{
		return clientIP;
	}

	public String getUserAgent()
	{
		return userAgent;
	}
}
