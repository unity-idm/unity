/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.AuthnOptionsColumns;

public class HeaderConfig implements AuthnElementConfiguration
{
	public final I18nString headerText;

	public HeaderConfig(I18nString headerText)
	{
		this.headerText = headerText;
	}

	@Override
	public PropertiesRepresentation toProperties(UnityMessageSource msg)
	{
		String key = AuthnOptionsColumns.SPECIAL_ENTRY_HEADER;
		Properties raw = new Properties();
		if (headerText != null && !headerText.isEmpty())
		{
			String id = RandomStringUtils.randomAlphabetic(6).toUpperCase();
			headerText.toProperties(raw,

					VaadinEndpointProperties.PREFIX
							+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX + id + "."
							+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT,
					msg);
			
			key += "_" + id;
		}
		return new PropertiesRepresentation(key, raw);

	}

	public static class HeaderConfigFactory implements AuthnElementConfigurationFactory
	{

		@Override
		public Optional<AuthnElementConfiguration> getConfigurationElement(UnityMessageSource msg,
				VaadinEndpointProperties properties, String specEntry)
		{
			if (!specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER))
			{
				return Optional.empty();
			}

			String key = specEntry.substring(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER.length());
			I18nString message = key.isEmpty() ? new I18nString()
					: SeparatorConfig.SeparatorConfigFactory
							.resolveSeparatorMessage(key.substring(1), properties, msg);
			return Optional.of(new HeaderConfig(message));
		}
	}
}