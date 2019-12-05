/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT;

import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.AuthnOptionsColumns;

public class SeparatorConfig implements AuthnElementConfiguration
{
	public final I18nString separatorText;

	public SeparatorConfig(I18nString separatorText)
	{
		this.separatorText = separatorText;
	}

	@Override
	public PropertiesRepresentation toProperties(UnityMessageSource msg)
	{
		String key = AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR;
		Properties raw = new Properties();
		if (separatorText != null && !separatorText.isEmpty())
		{
			String id = RandomStringUtils.randomAlphabetic(6).toUpperCase();
			separatorText.toProperties(raw,

					VaadinEndpointProperties.PREFIX
							+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX + id + "."
							+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT,
					msg);
			key += "_" + id;
		}
		return new PropertiesRepresentation(key, raw);

	}
	
	public static class SeparatorConfigFactory implements AuthnElementConfigurationFactory
	{

		@Override
		public Optional<AuthnElementConfiguration> getConfigurationElement(UnityMessageSource msg,
				VaadinEndpointProperties properties, String specEntry)
		{
			if (!specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR))
			{
				return Optional.empty();
			}

			String key = specEntry.substring(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR.length());
			I18nString message = key.isEmpty() ? new I18nString()
					: resolveSeparatorMessage(key.substring(1), properties, msg);
			return Optional.of(new SeparatorConfig(message));
		}

		public static I18nString resolveSeparatorMessage(String key, VaadinEndpointProperties properties,
				UnityMessageSource msg)
		{
			I18nString value = properties.getLocalizedStringWithoutFallbackToDefault(msg,
					AUTHN_OPTION_LABEL_PFX + key + "." + AUTHN_OPTION_LABEL_TEXT);
			return value == null ? new I18nString() : value;
		}
	}
}