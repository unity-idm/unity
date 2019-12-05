/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.AuthnOptionsColumns;

public class SingleAuthnConfig implements AuthnElementConfiguration
{
	public final String authnOption;

	public SingleAuthnConfig(String authnOption)
	{
		this.authnOption = authnOption;
	}

	@Override
	public PropertiesRepresentation toProperties(UnityMessageSource msg)
	{
		return new PropertiesRepresentation(authnOption);
	}

	public static class SingleAuthnFactory implements AuthnElementConfigurationFactory
	{

		@Override
		public Optional<AuthnElementConfiguration> getConfigurationElement(UnityMessageSource msg,
				VaadinEndpointProperties properties, String specEntry)
		{
			if (specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR)
					|| specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER)
					||  specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_REGISTER)
					||  specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_LAST_USED)
					||  specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_EXPAND)
					||  specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_GRID))
			{
				return Optional.empty();
			}

			return Optional.of(new SingleAuthnConfig(specEntry));
		}
	}
}