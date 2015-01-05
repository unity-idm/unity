/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import pl.edu.icm.unity.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.authn.CancelHandler;

/**
 * In principle all UI should implement this interface, to be injected with 
 * information about the actual endpoint instance to which the UI is attached.
 * @author K. Benedyczak
 */
public interface UnityWebUI
{
	public void configure(EndpointDescription description, 
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration);
	
	/**
	 * Method invoked only if the endpoint supports cancellation of authentication. Some of the endpoints
	 * might not support it if are intended to be invoked directly - then cancel button won't be displayed.
	 * This method is either not invoked, or invoked just after {@link #configure(EndpointDescription, List)}.  
	 * @param handler
	 */
	public void setCancelHandler(CancelHandler handler);

	/**
	 * Method invoked only for SandboxUI and AuthenticationUI.
	 * @param sandboxRouter
	 */
	public void setSandboxRouter(SandboxAuthnRouter sandboxRouter);
}
