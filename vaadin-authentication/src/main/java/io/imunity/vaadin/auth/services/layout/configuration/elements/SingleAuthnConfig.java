/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.services.layout.configuration.elements;

import com.google.common.base.Objects;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;

import java.util.Optional;

public class SingleAuthnConfig implements AuthnElementConfiguration
{
	public final String authnOption;

	public SingleAuthnConfig(String authnOption)
	{
		this.authnOption = authnOption;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hashCode(authnOption);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final SingleAuthnConfig other = (SingleAuthnConfig) obj;

		return Objects.equal(this.authnOption, other.authnOption);		
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