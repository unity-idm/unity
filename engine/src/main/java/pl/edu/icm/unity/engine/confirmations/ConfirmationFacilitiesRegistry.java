/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.registries.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link ConfirmationFacility}ies.
 * 
 * @author P. Piernik
 */
@Component
public class ConfirmationFacilitiesRegistry extends TypesRegistryBase<ConfirmationFacility<?>>
{
	
	@Autowired
	public ConfirmationFacilitiesRegistry(List<ConfirmationFacility<?>> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(ConfirmationFacility<?> from)
	{
		return from.getName();
	}

}
