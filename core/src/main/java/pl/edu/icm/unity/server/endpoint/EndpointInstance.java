/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import java.util.List;

import pl.edu.icm.unity.types.JsonSerializable;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Generic endpoint instance
 * @author K. Benedyczak
 */
public interface EndpointInstance extends JsonSerializable
{
	public EndpointDescription getDescription();
	
	public void setAuthenticators(List<AuthenticatorSet> authenticatorsInfo, List<BindingAuthn> authenticators);
	
	public void setDescription(String description);
	
	public void destroy();
}
