/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.auth.services.layout.configuration.elements;

import io.imunity.vaadin.auth.AuthnOptionsColumns;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;

import java.util.Optional;

public class ExpandConfig implements AuthnElementConfiguration
{
	public static class Parser implements AuthnElementParser<ExpandConfig>
	{
		@Override
		public Optional<ExpandConfig> getConfigurationElement(VaadinEndpointProperties properties, String specEntry)
		{
			if (!specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_EXPAND))
			{
				return Optional.empty();
			}
			
			return Optional.of(new ExpandConfig());
		}
		
		@Override
		public PropertiesRepresentation toProperties(ExpandConfig config)
		{
			return new PropertiesRepresentation(AuthnOptionsColumns.SPECIAL_ENTRY_EXPAND);
		}
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		return true;	
	}
}