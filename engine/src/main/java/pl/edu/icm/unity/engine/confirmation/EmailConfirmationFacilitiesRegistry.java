/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;

/**
 * Maintains a simple registry of available {@link EmailConfirmationFacility}ies.
 * 
 * @author P. Piernik
 */
@Component
public class EmailConfirmationFacilitiesRegistry extends TypesRegistryBase<EmailConfirmationFacility<?>>
{
	
	@Autowired
	public EmailConfirmationFacilitiesRegistry(List<EmailConfirmationFacility<?>> typeElements)
	{
		super(typeElements);
	}

	@Override
	protected String getId(EmailConfirmationFacility<?> from)
	{
		return from.getName();
	}

}
