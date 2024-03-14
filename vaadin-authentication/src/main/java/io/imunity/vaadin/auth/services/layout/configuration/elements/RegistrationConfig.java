/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.configuration.elements;

import io.imunity.vaadin.auth.AuthnOptionsColumns;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;

import java.util.Optional;

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