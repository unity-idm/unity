/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.server.HTTPRequestContext;
import ua_parser.Client;
import ua_parser.Parser;

public class LoginMachineDetailsExtractor
{
	public static LoginMachineDetails getLoginMachineDetailsFromCurrentRequest()
	{
		HTTPRequestContext httpRequestContext = HTTPRequestContext.getCurrent();
		return getLoginMachineDetails(httpRequestContext.getClientIP(), httpRequestContext.getUserAgent());
	}
	
	
	public static LoginMachineDetails getLoginMachineDetails(String clientIp, String userAgent)
	{
		Parser uap = new Parser();
		Client parsedClient = uap.parse(userAgent);
		String osName = parsedClient.os.family;
		String browser = parsedClient.userAgent.family;
		return new LoginMachineDetails(clientIp, 
				osName == null ? "unknown" : osName, 
				browser == null ? "unknown" : browser);
	}
}
