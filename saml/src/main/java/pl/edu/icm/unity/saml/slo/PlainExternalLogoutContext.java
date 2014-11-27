/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 21.11.2014
 * Author: K. Benedyczak <golbi@icm.edu.pl>
 */

package pl.edu.icm.unity.saml.slo;

import pl.edu.icm.unity.server.api.internal.LoginSession;

/**
 * SAML Context for single logout protocol. This context is used to track state during logout, 
 * where Unity is session authority when handling a logout initiated by other means then the SAML protocol. Therefore 
 * it preserves the state needed to produce the final redirect after the SAML part is finished.  
 * 
 * @author K. Benedyczak
 */
public class PlainExternalLogoutContext extends AbstractSAMLLogoutContext
{
	private String requestersRelayState;
	private String returnUrl;
	
	public PlainExternalLogoutContext(String localIssuer, String requesterRelayState, 
			String retunUrl, LoginSession loginSession)
	{
		super(localIssuer, loginSession);
		this.requestersRelayState = requesterRelayState;
		this.returnUrl = retunUrl;
	}

	/**
	 * @return null or the relay state which was provided by a session participant 
	 * which requested logout from Unity.
	 */
	public String getRequestersRelayState()
	{
		return requestersRelayState;
	}

	public String getReturnUrl()
	{
		return returnUrl;
	}
}
