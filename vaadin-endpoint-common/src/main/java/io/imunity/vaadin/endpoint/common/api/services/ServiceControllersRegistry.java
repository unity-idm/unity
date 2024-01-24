/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a simple registry of available
 * {@link ServiceController}s.
 * 
 * @author P.Piernik
 *
 */
@Component("ServiceControllersRegistry")
public class ServiceControllersRegistry extends TypesRegistryBase<ServiceController>
{
	@Autowired
	public ServiceControllersRegistry(Optional<List<ServiceController>> typeElements)
	{
		super(typeElements.orElseGet(ArrayList::new));
	}

	@Override
	protected String getId(ServiceController from)
	{
		return from.getSupportedEndpointType();
	}
}
