/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;

import com.google.common.net.InetAddresses;

/**
 * Extract real client's IP basing on HTTP request and configuration. Supports proxied requests,
 * with X-Forwarded-For header. 
 */
class ClientIPDiscovery
{
	private int proxyCount;
	private boolean allowNotProxiedTraffic;
	private Pattern PORT_PATTERN = Pattern.compile("^([^:]+):[0-9]{1,5}$");

	ClientIPDiscovery(int proxyCount, boolean allowNotProxiedTraffic)
	{
		this.proxyCount = proxyCount;
		this.allowNotProxiedTraffic = allowNotProxiedTraffic;
	}
	
	String getClientIP(Request request)
	{
		String clientIP = proxyCount > 0 ? getProxiedClientIP(request) : getDirectClientIP(request);
		String bracketsStripped = stripBracketsIfPresent(clientIP);
		String strippedIP = stripPortIfPresent(bracketsStripped);
		//sanity check - eliminate chances for log injection in case of misconfigured setups
		InetAddresses.forString(strippedIP);
		return strippedIP;
	}

	String getImmediateClientIPNoCheck(Request request)
	{
		return stripBracketsIfPresent(getDirectClientIP(request));
	}
	
	private String stripBracketsIfPresent(String clientIP) 
	{
		return (clientIP.charAt(0) == '[' && clientIP.charAt(clientIP.length()-1) == ']') ? 
				clientIP.substring(1, clientIP.length()-1) : clientIP;
	}

	private String getDirectClientIP(Request request)
	{
		return Request.getRemoteAddr(request);
	}

	private String stripPortIfPresent(String obtainedAddress)
	{
		Matcher matcher = PORT_PATTERN.matcher(obtainedAddress);
		if (matcher.matches() && matcher.groupCount() == 1)
			return matcher.group(1);
		return obtainedAddress;
	}
	
	private String getProxiedClientIP(Request request)
	{
		String xff = request.getHeaders().get(HttpHeader.X_FORWARDED_FOR);
		if (xff == null)
		{
			if (allowNotProxiedTraffic)
				return getDirectClientIP(request);
			throw new IllegalArgumentException(HttpHeader.X_FORWARDED_FOR + " not found while configuration requires "
					+ "a proxy.");
		}
		String[] xffArray = xff.split(",");
		if (xffArray.length < proxyCount)
			throw new IllegalArgumentException(HttpHeader.X_FORWARDED_FOR + " has only " + xffArray.length + 
					" elements while we are configured to be behind " + proxyCount + " proxy(ies). "
							+ "Check proxy configuration. Header: " + xff);
		return xffArray[xffArray.length-proxyCount].trim();
	}
}
