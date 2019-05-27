/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.google.common.net.InetAddresses;

/**
 * Extract real client's IP basing on HTTP request and configuration. Supports proxied requests,
 * with X-Forwarded-For header. 
 */
class ClientIPDiscovery
{
	private static final String XFF_HEADER = "X-Forwarded-For";
	private int proxyCount;
	private boolean allowNotProxiedTraffic;
	private Pattern PORT_PATTERN = Pattern.compile("^([^:]+):[0-9]{1,5}$");

	ClientIPDiscovery(int proxyCount, boolean allowNotProxiedTraffic)
	{
		this.proxyCount = proxyCount;
		this.allowNotProxiedTraffic = allowNotProxiedTraffic;
	}
	
	String getClientIP(HttpServletRequest request)
	{
		String clientIP = proxyCount > 0 ? getProxiedClientIP(request) : getDirectClientIP(request);
		//sanity check - eliminate chances for log injection in case of misconfigured setups
		String strippedIP = stripPortIfPresent(clientIP);
		InetAddresses.forString(strippedIP);
		return strippedIP;
	}

	private String getDirectClientIP(HttpServletRequest request)
	{
		return request.getRemoteAddr();
	}

	private String stripPortIfPresent(String obtainedAddress)
	{
		Matcher matcher = PORT_PATTERN.matcher(obtainedAddress);
		if (matcher.matches() && matcher.groupCount() == 1)
			return matcher.group(1);
		return obtainedAddress;
	}
	
	private String getProxiedClientIP(HttpServletRequest request)
	{
		String xff = request.getHeader(XFF_HEADER);
		if (xff == null)
		{
			if (allowNotProxiedTraffic)
				return getDirectClientIP(request);
			throw new IllegalArgumentException(XFF_HEADER + " not found while configuration requires "
					+ "a proxy.");
		}
		String[] xffArray = xff.split(",");
		if (xffArray.length < proxyCount)
			throw new IllegalArgumentException(XFF_HEADER + " has only " + xffArray.length + 
					" elements while we are configured to be behind " + proxyCount + " proxy(ies). "
							+ "Check proxy configuration. Header: " + xff);
		return xffArray[xffArray.length-proxyCount].trim();
	}
}
