/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.endpoint;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.types.DescribedObjectImpl;

/**
 * Defines a static description of an endpoint. It is provided by an endpoint implementation. 
 * @author K. Benedyczak
 */
public class EndpointTypeDescription extends DescribedObjectImpl
{
	private Set<String> supportedBindings;
	private Map<String,String> paths;
	
	public EndpointTypeDescription()
	{
	}

	public EndpointTypeDescription(String name, String description, Set<String> supportedBindings, 
			Map<String,String> paths)
	{
		super(name, description);
		setSupportedBindings(supportedBindings);
		setPaths(paths);
	}

	public void setSupportedBindings(Set<String> supportedBindings)
	{
		this.supportedBindings = Collections.unmodifiableSet(supportedBindings);
	}
	
	public void setPaths(Map<String,String> paths)
	{
		this.paths=Collections.unmodifiableMap(paths);
	}

	/**
	 * @return supported binding ids as CXF(WS) or Vaadin(WWW)
	 */
	public Set<String> getSupportedBindings()
	{
		return supportedBindings;
	}
	
	public Map<String,String> getPaths()
	{
		return paths;
	}
	
}
