/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.endpoint;

import java.util.Set;

import pl.edu.icm.unity.types.DescribedObject;

/**
 * Defines a static description of an endpoint. It is provided by an endpoint implementation. 
 * @author K. Benedyczak
 */
public interface EndpointTypeDescription extends DescribedObject
{
	/**
	 * @return supported binding ids as CXF(WS) or Vaadin(WWW)
	 */
	public Set<String> getSupportedBindings();
}
