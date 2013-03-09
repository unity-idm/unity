/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.endpoint;

import java.util.Collections;
import java.util.Set;

import pl.edu.icm.unity.types.DescribedObjectImpl;

/**
 * Defines a static description of an endpoint. It is provided by an endpoint implementation. 
 * @author K. Benedyczak
 */
public class EndpointTypeDescription extends DescribedObjectImpl
{
	private Set<String> supportedBindings;
	
	public EndpointTypeDescription()
	{
	}

	public EndpointTypeDescription(String name, String description, Set<String> supportedBindings)
	{
		super(name, description);
		setSupportedBindings(supportedBindings);
	}

	public void setSupportedBindings(Set<String> supportedBindings)
	{
		this.supportedBindings = Collections.unmodifiableSet(supportedBindings);
	}

	/**
	 * @return supported binding ids as CXF(WS) or Vaadin(WWW)
	 */
	public Set<String> getSupportedBindings()
	{
		return supportedBindings;
	}
	
}
