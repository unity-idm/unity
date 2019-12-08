/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

import java.util.Optional;

import pl.edu.icm.unity.webui.VaadinEndpointProperties;

public class SingleAuthnConfig implements AuthnElementConfiguration
{
	public final String authnOption;

	public SingleAuthnConfig(String authnOption)
	{
		this.authnOption = authnOption;
	}

	public static class Parser implements AuthnElementParser<SingleAuthnConfig>
	{
		@Override
		public Optional<SingleAuthnConfig> getConfigurationElement(
				VaadinEndpointProperties properties, String specEntry)
		{
			if (specEntry.isEmpty())
				return Optional.empty();
			return Optional.of(new SingleAuthnConfig(specEntry));
		}
		

		@Override
		public PropertiesRepresentation toProperties(SingleAuthnConfig config)
		{
			return new PropertiesRepresentation(config.authnOption);
		}
	}
}