/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

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
}