/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.server;

import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Stores in thread local state related to the HTTP request being served by the thread.
 */
public class HTTPRequestContext
{
	private static ThreadLocal<HTTPRequestContext> threadLocal = new ThreadLocal<>();
	private String clientIP;
	
	public HTTPRequestContext(String clientIP)
	{
		this.clientIP = clientIP;
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
}
