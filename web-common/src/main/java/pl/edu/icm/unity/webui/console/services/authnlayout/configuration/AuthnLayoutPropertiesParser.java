/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRIDS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_CONTENTS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_ROWS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.AuthnOptionsColumns;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.ExpandConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.GridConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.HeaderConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.LastUsedConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.RegistrationConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SeparatorConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SingleAuthnConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements.GridAuthnColumnState;

/**
 * Maps {@link ColumnElement} to properties and revert
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutPropertiesParser
{
	public static AuthnLayoutConfiguration fromProperties(VaadinEndpointProperties properties,
			UnityMessageSource msg)
	{
		List<AuthnLayoutColumnConfiguration> columns = new ArrayList<>();
		List<I18nString> separators = new ArrayList<>();

		Iterator<String> columnKeys = properties
				.getStructuredListKeys(VaadinEndpointProperties.AUTHN_COLUMNS_PFX).iterator();

		while (columnKeys.hasNext())
		{
			String columnKey = columnKeys.next();
			AuthnLayoutColumnConfiguration lcolumn = getColumn(columnKey, properties, msg);
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

	private static AuthnLayoutColumnConfiguration getColumn(String prefix, VaadinEndpointProperties properties,
			UnityMessageSource msg)
	{

		I18nString ptitle = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				prefix + VaadinEndpointProperties.AUTHN_COLUMN_TITLE);
		Double pwidth = properties.getDoubleValue(prefix + VaadinEndpointProperties.AUTHN_COLUMN_WIDTH);

		return new AuthnLayoutColumnConfiguration(ptitle, pwidth.intValue(),
				getColumnElements(prefix, properties, msg));

	}

	private static List<AuthnElementConfiguration> getColumnElements(String prefix,
			VaadinEndpointProperties properties, UnityMessageSource msg)
	{

		return getColumnElements(properties, msg,
				properties.getValue(prefix + VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS));
	}

	public static List<AuthnElementConfiguration> getReturingUserColumnElementsFromProperties(
			VaadinEndpointProperties properties, UnityMessageSource msg)
	{

		return getColumnElements(properties, msg,
				properties.getValue(VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT));

	}

	public static List<AuthnElementConfiguration> getColumnElements(VaadinEndpointProperties properties,
			UnityMessageSource msg, String content)
	{
		List<AuthnElementConfiguration> elements = new ArrayList<>();
		String[] specSplit = content.trim().split("[ ]+");

		for (String specEntry : specSplit)
		{
			if (specEntry.isEmpty())
				continue;

			if (specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR))
			{
				elements.add(new SeparatorConfig(getOptionsSeparator(specEntry, properties, msg)));

			} else if (specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER))
			{

				elements.add(new HeaderConfig(getOptionHeader(specEntry, properties, msg)));

			} else if (specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_REGISTER))
			{
				elements.add(new RegistrationConfig());

			} else if (specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_LAST_USED))
			{
				elements.add(new LastUsedConfig());

			} else if (specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_EXPAND))
			{
				elements.add(new ExpandConfig());

			} else if (specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_GRID))
			{
				elements.add(new GridConfig(getGrid(specEntry, properties)));

			} else
			{
				elements.add(new SingleAuthnConfig(specEntry));
			}
		}

		return elements;
	}

	public static Properties returningUserColumnElementToProperties(UnityMessageSource msg,
			List<AuthnElementConfiguration> retUserLayoutConfiguration)
	{
		Properties raw = new Properties();
		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT,
				getColumnContent(msg, retUserLayoutConfiguration, "1", raw));
		return raw;
	}

	private static GridAuthnColumnState getGrid(String specEntry, VaadinEndpointProperties properties)
	{
		String key = specEntry.substring(AuthnOptionsColumns.SPECIAL_ENTRY_GRID.length());
		if (key.length() == 0)
			return null;
		String contents = properties.getValue(AUTHN_GRIDS_PFX + key + "." + AUTHN_GRID_CONTENTS);
		if (contents == null)
			return null;
		int height = properties.getIntValue(AUTHN_GRIDS_PFX + key + "." + AUTHN_GRID_ROWS);

		return new GridAuthnColumnState(contents, height);
	}

	private static I18nString getOptionsSeparator(String specEntry, VaadinEndpointProperties properties,
			UnityMessageSource msg)
	{
		String key = specEntry.substring(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR.length());
		I18nString message = key.isEmpty() ? new I18nString()
				: resolveSeparatorMessage(key.substring(1), properties, msg);
		return message;
	}

	private static I18nString getOptionHeader(String specEntry, VaadinEndpointProperties properties,
			UnityMessageSource msg)
	{
		String key = specEntry.substring(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER.length());
		I18nString message = key.isEmpty() ? new I18nString()
				: resolveSeparatorMessage(key.substring(1), properties, msg);
		return message;
	}

	private static I18nString resolveSeparatorMessage(String key, VaadinEndpointProperties properties,
			UnityMessageSource msg)
	{
		I18nString value = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				AUTHN_OPTION_LABEL_PFX + key + "." + AUTHN_OPTION_LABEL_TEXT);
		return value == null ? new I18nString() : value;
	}

	public static Properties toProperties(UnityMessageSource msg, AuthnLayoutConfiguration content)
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
					getColumnContent(msg, c.contents, String.valueOf(columnIt), raw));

			columnIt++;
		}

		return raw;
	}

	public static String getColumnContent(UnityMessageSource msg, List<AuthnElementConfiguration> columnContent,
			String colIt, Properties raw)
	{
		List<String> elementsRep = new ArrayList<>();

		int sepIt = 1;
		int gridIt = 1;

		for (AuthnElementConfiguration element : columnContent)
		{
			if (element instanceof HeaderConfig)
			{
				HeaderConfig he = (HeaderConfig) element;
				if (he.value != null && !he.value.isEmpty())
				{
					String key = saveSeparator((I18nString) he.value, getSepId(colIt, sepIt++), raw,
							msg);
					elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER + "_" + key);
				} else
				{
					elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER);
				}
			}

			else if (element instanceof SeparatorConfig)
			{
				SeparatorConfig sep = (SeparatorConfig) element;
				if (sep.value != null && !sep.value.isEmpty())
				{
					String key = saveSeparator((I18nString) sep.value, getSepId(colIt, sepIt++),
							raw, msg);
					elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR + "_" + key);
				} else
				{
					elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR);
				}

			} else if (element instanceof LastUsedConfig)
			{
				elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_LAST_USED);
			} else if (element instanceof RegistrationConfig)
			{
				elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_REGISTER);
			} else if (element instanceof SingleAuthnConfig)
			{
				SingleAuthnConfig se = (SingleAuthnConfig) element;
				elementsRep.add((String) se.value);

			} else if (element instanceof GridConfig)
			{
				GridConfig grid = (GridConfig) element;
				String key = saveGrid((GridAuthnColumnState) grid.value, getGridId(colIt, gridIt++),
						raw, msg);
				elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_GRID + key);
			} else if (element instanceof ExpandConfig)
			{
				elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_EXPAND);
			}

		}

		return String.join(" ", elementsRep);
	}

	private static String saveGrid(GridAuthnColumnState grid, String key, Properties raw, UnityMessageSource msg)
	{
		if (grid != null)
		{

			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_GRIDS_PFX + key + "."
					+ VaadinEndpointProperties.AUTHN_GRID_CONTENTS, grid.content);

			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_GRIDS_PFX + key + "."
					+ VaadinEndpointProperties.AUTHN_GRID_ROWS, String.valueOf(grid.rows));
		}

		return key;
	}

	private static String getGridId(String col, int sep)
	{
		return "C" + col + "G" + sep;
	}

	private static String getSepId(String col, int sep)
	{
		return "C" + col + "S" + sep;
	}

	private static String saveSeparator(I18nString separator, String key, Properties raw, UnityMessageSource msg)
	{

		if (separator != null)
		{
			separator.toProperties(raw,

					VaadinEndpointProperties.PREFIX
							+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX + key + "."
							+ VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT,
					msg);
		} else
		{
			raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX + key
					+ "." + VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT, "");
		}

		return key;
	}
}
