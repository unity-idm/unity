/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.services;

import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;


/**
 * 
 * @author P.Piernik
 *
 */
class EndpointEntry
{	
	public final Endpoint endpoint;
	public final EndpointTypeDescription type;
	
	EndpointEntry(Endpoint endpoint, EndpointTypeDescription type)
	{
		this.endpoint = endpoint;
		this.type = type;
	}
}
