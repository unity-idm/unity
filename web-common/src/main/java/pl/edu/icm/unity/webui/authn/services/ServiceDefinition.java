/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.authn.services;

import pl.edu.icm.unity.types.endpoint.Endpoint.EndpointState;

public interface ServiceDefinition
{
	String getName();
	EndpointState getState();
	String getType();
	String getBinding();
	
}
