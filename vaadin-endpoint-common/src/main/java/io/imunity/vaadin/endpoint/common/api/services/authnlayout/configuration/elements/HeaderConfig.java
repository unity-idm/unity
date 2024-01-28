/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import com.google.common.base.Objects;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
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
	public int hashCode()
	{
		return Objects.hashCode(headerText);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final HeaderConfig other = (HeaderConfig) obj;

		return Objects.equal(this.headerText, other.headerText);		
	}

	public static class Parser implements AuthnElementParser<HeaderConfig>
	{
		private final MessageSource msg;
		private final Supplier<String> idGenerator;
		
		public Parser(MessageSource msg, Supplier<String> idGenerator)
		{
			this.msg = msg;
			this.idGenerator = idGenerator;
		}
		
		@Override
		public Optional<HeaderConfig> getConfigurationElement(
				VaadinEndpointProperties properties, String specEntry)
		{
			if (!specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER))
				return Optional.empty();

			String key = specEntry.substring(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER.length());
			I18nString message = key.isEmpty() ? new I18nString()
					: SeparatorConfig.Parser
							.resolveSeparatorMessage(key.substring(1), properties, msg);
			return Optional.of(new HeaderConfig(message));
		}
		
		@Override
		public PropertiesRepresentation toProperties(HeaderConfig element)
		{
			String key = AuthnOptionsColumns.SPECIAL_ENTRY_HEADER;
			Properties raw = new Properties();
			if (element.headerText != null && !element.headerText.isEmpty())
			{
				String id = idGenerator.get();
				element.headerText.toProperties(raw,

						VaadinEndpointProperties.PREFIX
								+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX + id + "."
								+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT,
						msg);
				
				key += "_" + id;
			}
			return new PropertiesRepresentation(key, raw);
		}
	}
}