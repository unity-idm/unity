/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Implemented by a class which allows to create an endpoint instance ready for hot deployment.
 * @author K. Benedyczak
 */
public interface EndpointFactory
{
	public EndpointTypeDescription getDescription();
	public EndpointInstance newInstance();
}
