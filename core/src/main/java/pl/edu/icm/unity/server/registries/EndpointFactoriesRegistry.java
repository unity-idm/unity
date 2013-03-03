/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;


/**
 * Registry of available {@link EndpointFactory}ies
 * @author K. Benedyczak
 */
@Component
public class EndpointFactoriesRegistry
{
	private Map<String, EndpointFactory> factoriesById = new HashMap<String, EndpointFactory>();
	private List<EndpointTypeDescription> factoriesDescriptions = new ArrayList<EndpointTypeDescription>();
	
	@Autowired
	public EndpointFactoriesRegistry(List<EndpointFactory> endpointFactories)
	{
		for (EndpointFactory factory: endpointFactories)
		{
			factoriesById.put(factory.getDescription().getId(), factory);
			factoriesDescriptions.add(factory.getDescription());
		}
	}
	
	public List<EndpointTypeDescription> getDescriptions()
	{
		List<EndpointTypeDescription> ret = new ArrayList<EndpointTypeDescription>(factoriesDescriptions.size());
		ret.addAll(factoriesDescriptions);
		return ret;
	}
	
	public EndpointFactory getById(String id)
	{
		return factoriesById.get(id);
	}
}
