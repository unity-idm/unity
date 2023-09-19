/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.IOException;

import org.apache.log4j.MDC;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import pl.edu.icm.unity.engine.api.utils.MDCKeys;

/**
 * Sets client IP bound to the request into thread local variable.
 */
class ClientIPSettingHandler extends Handler.Wrapper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, ClientIPSettingHandler.class);
	private final ClientIPDiscovery ipDiscovery;
	private final IPValidator ipValidator;
	private final String endpointId;
	
	ClientIPSettingHandler(ClientIPDiscovery ipDiscovery, IPValidator ipValidator, String endpointId)
	{
		this.ipDiscovery = ipDiscovery;
		this.ipValidator = ipValidator;
		this.endpointId = endpointId;
	}
	
	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception
	{
		MDC.put(MDCKeys.ENDPOINT.key, endpointId);
		try
		{
			String remoteAddr = Request.getRemoteAddr(request);
			log.trace("Will establish client's address. Peer's address: {} forwarded-for: {}", 
					remoteAddr, request.getHeaders().get(HttpHeader.X_FORWARDED_FOR));

			String clientIP = getClientIP(request);
			validateAddress(request);

			if (HTTPRequestContext.getCurrent() != null)
				log.warn("Overriding old client's IP {} to {}, immediate client IP is {}",
						HTTPRequestContext.getCurrent().getClientIP(), clientIP, remoteAddr);
			else
				log.trace("Setting client's IP to {}, immediate client IP is {}", 
						clientIP, remoteAddr);

			log.debug("Handling client {} request to URL {}", clientIP, getFullRequestURL(request));
			MDC.put(MDCKeys.CLIENT_IP.key, clientIP);
			
			HTTPRequestContext.setCurrent(new HTTPRequestContext(clientIP, 
					request.getHeaders().get("User-Agent")));

			try
			{
				return getHandler().handle(request, response, callback);
			} finally
			{
				HTTPRequestContext.setCurrent(null);
			}
		} finally
		{
			MDC.remove(MDCKeys.ENDPOINT.key);
			MDC.remove(MDCKeys.CLIENT_IP.key);
		}
	}

	private String getFullRequestURL(Request httpRequest)
	{
		HttpURI httpURI = httpRequest.getHttpURI();
		return httpURI.asString();
	}
	
	private String getClientIP(Request httpRequest) throws IOException
	{
		try
		{
			return ipDiscovery.getClientIP(httpRequest);
		} catch (Exception e)
		{
			log.error("Can not establish client IP", e);
			throw new IOException("Illegal client IP");
		}
	}
	
	private void validateAddress(Request request) throws IOException
	{
		try
		{
			ipValidator.validateIPAddress(ipDiscovery.getImmediateClientIPNoCheck(request));
		} catch (Exception e)
		{
			log.error("Not allowed client IP", e);
			throw new IOException("Illegal client IP");
		}
	}	
}
