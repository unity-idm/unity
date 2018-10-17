/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;

/**
 * Instances are capable of performing automated proxy authentication: 
 * one of authentication options is triggered automatically, without loading the UI.
 * <p>
 * This feature is possible with external authenticators, which are redirect based and so 
 * does not require any user input on Unity. Such authenticator should be also configured 
 * in a way that there is only one authN option available.
 * 
 * @author K. Benedyczak
 */
public interface ProxyAuthenticationCapable extends BindingAuthn
{
	/**
	 * @return true if the request was handled by the filter, false if the filter's chain
	 * should be processed in regular way 
	 */
	boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse, String endpointPath) throws IOException;
}
