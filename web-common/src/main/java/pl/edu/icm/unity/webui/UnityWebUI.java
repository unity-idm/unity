/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * In principle all UI should implement this interface, to be injected with 
 * information about the actual endpoint instance to which the UI is attached.
 * @author K. Benedyczak
 */
public interface UnityWebUI
{
	public void configure(EndpointDescription description, 
			List<Map<String, BindingAuthn>> authenticators);
}
