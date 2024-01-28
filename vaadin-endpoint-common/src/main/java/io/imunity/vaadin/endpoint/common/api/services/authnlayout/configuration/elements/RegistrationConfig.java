/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements;

import java.util.Optional;

import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.AuthnOptionsColumns;

public class RegistrationConfig implements AuthnElementConfiguration
{
	public static class Parser implements AuthnElementParser<RegistrationConfig>
	{
		@Override
		public Optional<RegistrationConfig> getConfigurationElement(
				VaadinEndpointProperties properties, String specEntry)
		{
			return !specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_REGISTER) ? 
					Optional.empty() : Optional.of(new RegistrationConfig());
		}
		
		@Override
		public PropertiesRepresentation toProperties(RegistrationConfig element)
		{
			return new PropertiesRepresentation(AuthnOptionsColumns.SPECIAL_ENTRY_REGISTER);
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