/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfigurationFactory;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.ExpandConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.GridConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.HeaderConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.LastUsedConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.PropertiesRepresentation;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.RegistrationConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SeparatorConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SingleAuthnConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnElement;

/**
 * Maps {@link ColumnElement} to properties and revert
 * 
 * @author P.Piernik
 *
 */
@Component
public class AuthnLayoutPropertiesParser
{

	private UnityMessageSource msg;
	private List<AuthnElementConfigurationFactory> configFactories;

	@Autowired
	public AuthnLayoutPropertiesParser(UnityMessageSource msg)
	{
		this.msg = msg;
		this.configFactories = new ArrayList<>();
		configFactories.add(new HeaderConfig.HeaderConfigFactory());
		configFactories.add(new SeparatorConfig.SeparatorConfigFactory());
		configFactories.add(new GridConfig.GridConfigFactory());
		configFactories.add(new ExpandConfig.ExpandConfigFactory());
		configFactories.add(new RegistrationConfig.RegistrationConfigFactory());
		configFactories.add(new LastUsedConfig.LastUsedConfigFactory());
		configFactories.add(new SingleAuthnConfig.SingleAuthnFactory());
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

			if (c.title != null)
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

			raw.put(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS,
					getColumnContentAsPropertiesValue(c.contents, String.valueOf(columnIt), raw));

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
				getColumnContentAsPropertiesValue(retUserLayoutConfiguration, "1", raw));
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
			UnityMessageSource msg)
	{

		return getColumnElements(properties,
				properties.getValue(prefix + VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS));
	}

	private List<AuthnElementConfiguration> getColumnElements(VaadinEndpointProperties properties, String content)
	{
		List<AuthnElementConfiguration> elements = new ArrayList<>();
		String[] specSplit = content.trim().split("[ ]+");

		for (String specEntry : specSplit)
		{

			for (AuthnElementConfigurationFactory factory : configFactories)
			{
				Optional<AuthnElementConfiguration> config = factory.getConfigurationElement(msg,
						properties, specEntry);
				if (config.isPresent())
				{
					elements.add(config.get());
				}
			}
		}

		return elements;
	}

	private String getColumnContentAsPropertiesValue(List<AuthnElementConfiguration> columnContent, String colIt, Properties raw)
	{
		List<String> elementsRep = new ArrayList<>();

		for (AuthnElementConfiguration element : columnContent)
		{
			PropertiesRepresentation pr = element.toProperties(msg);
			elementsRep.add(pr.key);
			raw.putAll(pr.propertiesValues);
		}

		return String.join(" ", elementsRep);
	}
}
