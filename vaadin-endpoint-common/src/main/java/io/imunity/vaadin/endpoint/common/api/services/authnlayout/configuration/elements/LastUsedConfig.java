/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements;

import java.util.Optional;

import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.AuthnOptionsColumns;

public class LastUsedConfig implements AuthnElementConfiguration
{
	public static class Parser implements AuthnElementParser<LastUsedConfig>
	{
		@Override
		public Optional<LastUsedConfig> getConfigurationElement(VaadinEndpointProperties properties, String specEntry)
		{
			if (!specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_LAST_USED))
			{
				return Optional.empty();
			}
			
			return Optional.of(new LastUsedConfig());
		}
		
		@Override
		public PropertiesRepresentation toProperties(LastUsedConfig config)
		{
			return new PropertiesRepresentation(AuthnOptionsColumns.SPECIAL_ENTRY_LAST_USED);
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