/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.AuthnElementParser;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.ExpandConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.GridConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.HeaderConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.LastUsedConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.PropertiesRepresentation;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.RegistrationConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.SeparatorConfig;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.SingleAuthnConfig;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;


/**
 * Maps {@link ColumnComponent} to properties and vice versa
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutPropertiesParser
{
	public static final Class<? extends AuthnElementConfiguration> DEFAULT_CONFIG_PARSER = SingleAuthnConfig.class;
	private final MessageSource msg;
	private final Map<Class<? extends AuthnElementConfiguration>, AuthnElementParser<?>> configFactories;

	public AuthnLayoutPropertiesParser(MessageSource msg)
	{
		this.msg = msg;
		this.configFactories = ImmutableMap
				.<Class<? extends AuthnElementConfiguration>, AuthnElementParser<?>> builder()
				.put(HeaderConfig.class, new HeaderConfig.Parser(msg, new SubsequentIdGenerator()))
				.put(SeparatorConfig.class,
						new SeparatorConfig.Parser(msg, new SubsequentIdGenerator()))
				.put(GridConfig.class, new GridConfig.Parser(new SubsequentIdGenerator()))
				.put(ExpandConfig.class, new ExpandConfig.Parser())
				.put(RegistrationConfig.class, new RegistrationConfig.Parser())
				.put(LastUsedConfig.class, new LastUsedConfig.Parser())
				.put(DEFAULT_CONFIG_PARSER, new SingleAuthnConfig.Parser()).build();
	}

	public AuthnLayoutConfiguration fromProperties(VaadinEndpointProperties properties)
	{
		List<AuthnLayoutColumnConfiguration> columns = new ArrayList<>();
		List<I18nString> separators = new ArrayList<>();

		Iterator<String> columnKeys = properties
				.getStructuredListKeys(VaadinEndpointProperties.AUTHN_COLUMNS_PFX).iterator();

		while (columnKeys.hasNext())
		{
			String columnKey = columnKeys.next();
			AuthnLayoutColumnConfiguration lcolumn = getColumn(columnKey, properties);
			columns.add(lcolumn);

			if (columnKeys.hasNext())
			{
				I18nString sepVal = properties.getLocalizedStringWithoutFallbackToDefault(msg,
						columnKey + VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR);

				if (sepVal == null)
				{
					sepVal = new I18nString();
				}
				separators.add(sepVal);
			}
		}

		if (columns.isEmpty())
		{
			columns.add(new AuthnLayoutColumnConfiguration(new I18nString(), 15, Lists.newArrayList()));
		}

		return new AuthnLayoutConfiguration(columns, separators);

	}

	public Properties toProperties(AuthnLayoutConfiguration content)
	{
		Properties raw = new Properties();

		int columnIt = 1;

		for (AuthnLayoutColumnConfiguration c : content.columns)
		{
			String columnKey = VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_COLUMNS_PFX
					+ columnIt + ".";

			if (c.title != null && !c.title.isEmpty())
			{
				c.title.toProperties(raw, columnKey + VaadinEndpointProperties.AUTHN_COLUMN_TITLE, msg);
			}

			raw.put(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_WIDTH, String.valueOf(c.width));

			if (content.separators.size() > content.columns.indexOf(c))
			{
				I18nString sepV = content.separators.get(content.columns.indexOf(c));
				if (sepV != null && !sepV.isEmpty())
				{
					sepV.toProperties(raw,
							columnKey + VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR,
							msg);
				}
			}

			String columnContent = getColumnContentAsPropertiesValue(c.contents, raw);
			raw.put(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS, columnContent);

			columnIt++;
		}

		return raw;
	}

	public List<AuthnElementConfiguration> getReturingUserColumnElementsFromProperties(
			VaadinEndpointProperties properties)
	{

		return getColumnElements(properties,
				properties.getValue(VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT));

	}

	public Properties returningUserColumnElementToProperties(
			List<AuthnElementConfiguration> retUserLayoutConfiguration)
	{
		Properties raw = new Properties();
		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT,
				getColumnContentAsPropertiesValue(retUserLayoutConfiguration, raw));
		return raw;
	}

	private AuthnLayoutColumnConfiguration getColumn(String prefix, VaadinEndpointProperties properties)
	{

		I18nString ptitle = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				prefix + VaadinEndpointProperties.AUTHN_COLUMN_TITLE);
		Double pwidth = properties.getDoubleValue(prefix + VaadinEndpointProperties.AUTHN_COLUMN_WIDTH);

		return new AuthnLayoutColumnConfiguration(ptitle, pwidth.intValue(),
				getColumnElements(prefix, properties, msg));

	}

	private List<AuthnElementConfiguration> getColumnElements(String prefix, VaadinEndpointProperties properties,
			MessageSource msg)
	{

		return getColumnElements(properties,
				properties.getValue(prefix + VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS));
	}

	private List<AuthnElementConfiguration> getColumnElements(VaadinEndpointProperties properties, String content)
	{
		List<AuthnElementConfiguration> elements = new ArrayList<>();
		String[] specSplit = content.trim().split("[ ]+");
		boolean parsed;
		for (String specEntry : specSplit)
		{
			parsed = false;
			if (specEntry.isEmpty())
				continue;
			Optional<? extends AuthnElementConfiguration> config;
			for (AuthnElementParser<?> factory : configFactories.entrySet().stream()
					.filter(p -> !p.getKey().equals(DEFAULT_CONFIG_PARSER)).map(e -> e.getValue())
					.collect(Collectors.toList()))
			{
				config = factory.getConfigurationElement(properties, specEntry);
				if (config.isPresent())
				{
					elements.add(config.get());
					parsed = true;
					break;
				}
			}

			if (!parsed)
			{
				config = configFactories.get(DEFAULT_CONFIG_PARSER).getConfigurationElement(properties,
						specEntry);
				if (config.isPresent())
				{
					elements.add(config.get());
				}
			}
		}
		return elements;
	}

	private String getColumnContentAsPropertiesValue(List<AuthnElementConfiguration> columnContent, Properties raw)
	{
		List<String> elementsRep = new ArrayList<>();

		for (AuthnElementConfiguration element : columnContent)
		{
			@SuppressWarnings("unchecked")
			AuthnElementParser<AuthnElementConfiguration> authnElementParser = (AuthnElementParser<AuthnElementConfiguration>) configFactories
					.get(element.getClass());
			PropertiesRepresentation pr = authnElementParser.toProperties(element);
			elementsRep.add(pr.key);
			raw.putAll(pr.propertiesValues);
		}
		return String.join(" ", elementsRep);
	}

	private static class SubsequentIdGenerator implements Supplier<String>
	{
		private int value;

		@Override
		public String get()
		{
			return String.valueOf(++value);
		}
	}
}
