/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.authproxy;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.proxy.ProxyServlet;

/**
 * Proxy servlet basing on Jetty's implementation. Additional feature is to inject headers with authN information. 
 */
class AuthnInjectingProxyServlet extends ProxyServlet.Transparent 
{
	private static final String AUTH_USER_HEADER = "X-UAP-UserId";
	
	protected void copyRequestHeaders(HttpServletRequest clientRequest, Request proxyRequest)
	{
		super.copyRequestHeaders(clientRequest, proxyRequest);
		
		UserAttributes attribute = (UserAttributes) clientRequest.getAttribute(
				AuthenticationCheckingFilter.USER_INFO_ATTR);
		if (attribute != null)
		{
			String userId = attribute.getAttributes().get("sub").get(0);
			proxyRequest.header(AUTH_USER_HEADER, userId);
		}
	}
}
