/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

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
	
	public static class Parser implements AuthnElementParser<SeparatorConfig>
	{
		private final UnityMessageSource msg;
		private final Supplier<String> idGenerator;
		
		public Parser(UnityMessageSource msg, Supplier<String> idGenerator)
		{
			this.msg = msg;
			this.idGenerator = idGenerator;
		}

		@Override
		public Optional<SeparatorConfig> getConfigurationElement(
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
		
		@Override
		public PropertiesRepresentation toProperties(SeparatorConfig element)
		{
			String key = AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR;
			Properties raw = new Properties();
			if (element.separatorText != null && !element.separatorText.isEmpty())
			{
				String id = idGenerator.get();
				element.separatorText.toProperties(raw,

						VaadinEndpointProperties.PREFIX
								+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX + id + "."
								+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT,
						msg);
				key += "_" + id;
			}
			return new PropertiesRepresentation(key, raw);

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