/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.idp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a simple registry of available
 * {@link IdpServiceController}s.
 * 
 * @author P.Piernik
 *
 */
@Component("IdpServiceControllersRegistry")
public class IdpServiceControllersRegistry extends TypesRegistryBase<IdpServiceController>
{
	@Autowired
	public IdpServiceControllersRegistry(Optional<List<IdpServiceController>> typeElements)
	{
		super(typeElements.orElseGet(ArrayList::new));
	}

	@Override
	protected String getId(IdpServiceController from)
	{
		return from.getSupportedEndpointType();
	}
}
