/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services.authnlayout;

import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRIDS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_CONTENTS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_ROWS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_OPTION_LABEL_TEXT;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.AuthnOptionsColumns;
import pl.edu.icm.unity.webui.authn.services.layout.elements.ExpandColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.GridAuthnColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.GridAuthnColumnState;
import pl.edu.icm.unity.webui.authn.services.layout.elements.HeaderColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.LastUsedOptionColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.RegistrationColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.SeparatorColumnElement;
import pl.edu.icm.unity.webui.authn.services.layout.elements.SingleAuthnColumnElement;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * Maps {@link ColumnElement} to properties and revert
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutPropertiesHelper
{
	public static AuthenticationLayoutContent loadFromProperties(VaadinEndpointProperties properties,
			UnityMessageSource msg, Consumer<AuthnLayoutColumn> removeListener,
			Consumer<ColumnElement> removeElementListener, Runnable dragStart, Runnable dragStop,
			Runnable valueChange, AuthenticatorSupportService authenticatorSupportService,
			Supplier<List<String>> authnOptionSupplier)
	{
		List<AuthnLayoutColumn> columns = new ArrayList<>();
		List<I18nTextField> separators = new ArrayList<>();

		Iterator<String> columnKeys = properties
				.getStructuredListKeys(VaadinEndpointProperties.AUTHN_COLUMNS_PFX).iterator();

		while (columnKeys.hasNext())
		{
			String columnKey = columnKeys.next();

			AuthnLayoutColumn lcolumn = getColumn(columnKey, properties, msg, removeListener,
					removeElementListener, dragStart, dragStop, valueChange,
					authenticatorSupportService, authnOptionSupplier);
			columns.add(lcolumn);

			if (columnKeys.hasNext())
			{
				I18nString sepVal = properties.getLocalizedStringWithoutFallbackToDefault(msg,
						columnKey + VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR);
				I18nTextField sepField = new I18nTextField(msg);

				if (sepVal != null)
				{
					sepField.setValue(sepVal);
				}
				sepField.addValueChangeListener(e -> valueChange.run());
				separators.add(sepField);
			}
		}

		if (columns.isEmpty())
		{
			columns.add(new AuthnLayoutColumn(msg, removeListener, removeElementListener, valueChange));
		}
				
		return new AuthenticationLayoutContent(columns, separators);

	}

	private static AuthnLayoutColumn getColumn(String prefix, VaadinEndpointProperties properties,
			UnityMessageSource msg, Consumer<AuthnLayoutColumn> removeListener,
			Consumer<ColumnElement> removeElementListener, Runnable dragStart, Runnable dragStop,
			Runnable valueChange, AuthenticatorSupportService authenticatorSupportService,
			Supplier<List<String>> authnOptionSupplier)
	{
		AuthnLayoutColumn lcolumn = new AuthnLayoutColumn(msg, removeListener, removeElementListener, valueChange);

		I18nString ptitle = properties.getLocalizedStringWithoutFallbackToDefault(msg,
				prefix + VaadinEndpointProperties.AUTHN_COLUMN_TITLE);
		if (ptitle != null)
		{
			lcolumn.setColumnTitle(ptitle);
		}

		Double pwidth = properties.getDoubleValue(prefix + VaadinEndpointProperties.AUTHN_COLUMN_WIDTH);
		lcolumn.setColumnWidth(pwidth.intValue());

		lcolumn.setElements(getColumnElements(prefix, properties, msg, removeElementListener, dragStart,
				dragStop, valueChange, authenticatorSupportService, authnOptionSupplier));

		return lcolumn;
	}

	private static List<ColumnElement> getColumnElements(String prefix, VaadinEndpointProperties properties,
			UnityMessageSource msg, Consumer<ColumnElement> removeElementListener, Runnable dragStart,
			Runnable dragStop, Runnable valueChange,
			AuthenticatorSupportService authenticatorSupportService,
			Supplier<List<String>> authnOptionSupplier)
	{
		List<ColumnElement> elements = new ArrayList<>();
		String content = properties.getValue(prefix + VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS);
		String[] specSplit = content.trim().split("[ ]+");

		for (String specEntry : specSplit)
		{
			if (specEntry.isEmpty())
				continue;

			if (specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR))
			{
				SeparatorColumnElement el = new SeparatorColumnElement(msg, removeElementListener,
						valueChange, dragStart, dragStop);
				el.setValue(getOptionsSeparator(specEntry, properties, msg));
				elements.add(el);

			} else if (specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER))
			{
				HeaderColumnElement el = new HeaderColumnElement(msg, removeElementListener,
						valueChange, dragStart, dragStop);
				el.setValue(getOptionHeader(specEntry, properties, msg));
				elements.add(el);

			} else if (specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_REGISTER))
			{
				RegistrationColumnElement el = new RegistrationColumnElement(msg, removeElementListener,
						dragStart, dragStop);
				elements.add(el);

			} else if (specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_LAST_USED))
			{
				LastUsedOptionColumnElement el = new LastUsedOptionColumnElement(msg,
						removeElementListener, dragStart, dragStop);
				elements.add(el);

			} else if (specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_EXPAND))
			{
				ExpandColumnElement el = new ExpandColumnElement(msg, removeElementListener, dragStart,
						dragStop);
				elements.add(el);

			} else if (specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_GRID))
			{
				GridAuthnColumnElement el = new GridAuthnColumnElement(msg, authenticatorSupportService,
						authnOptionSupplier, removeElementListener, valueChange, dragStart,
						dragStop);
				el.setValue(getGrid(specEntry, properties));
				elements.add(el);

			} else
			{
				SingleAuthnColumnElement el = new SingleAuthnColumnElement(msg,
						authenticatorSupportService, authnOptionSupplier, removeElementListener,
						valueChange, dragStart, dragStop);
				el.setValue(specEntry);
				elements.add(el);
			}
		}

		return elements;
	}

	public static List<ColumnElement> getReturingUserColumnElements(VaadinEndpointProperties properties,
			UnityMessageSource msg, Consumer<ColumnElement> removeElementListener,
			Runnable valueChangeListener, Runnable dragStart, Runnable dragStop)
	{
		List<ColumnElement> elements = new ArrayList<>();
		String content = properties.getValue(VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT);
		if (content == null || content.isEmpty())
			return elements;
		String[] specSplit = content.trim().split("[ ]+");

		for (String specEntry : specSplit)
		{
			if (specEntry.isEmpty())
				continue;

			if (specEntry.startsWith(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR))
			{
				SeparatorColumnElement el = new SeparatorColumnElement(msg, removeElementListener,
						valueChangeListener, dragStart, dragStop);
				el.setValue(getOptionsSeparator(specEntry, properties, msg));
				elements.add(el);

			} else if (specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_LAST_USED))
			{
				LastUsedOptionColumnElement el = new LastUsedOptionColumnElement(msg, null, dragStart,
						dragStop);
				elements.add(el);
			} else if (specEntry.equals(AuthnOptionsColumns.SPECIAL_ENTRY_EXPAND))
			{
				ExpandColumnElement el = new ExpandColumnElement(msg, null, dragStart, dragStop);
				elements.add(el);
			}
		}

		return elements;
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

	public static Properties toProperties(UnityMessageSource msg, AuthenticationLayoutContent content)
	{
		Properties raw = new Properties();

		int columnIt = 1;

		for (AuthnLayoutColumn c : content.columns)
		{
			String columnKey = VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_COLUMNS_PFX
					+ columnIt + ".";

			if (c.getColumnTitle() != null)
			{
				c.getColumnTitle().toProperties(raw,
						columnKey + VaadinEndpointProperties.AUTHN_COLUMN_TITLE, msg);
			}

			raw.put(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_WIDTH,
					String.valueOf(c.getColumnWidth()));

			if (content.separators.size() > content.columns.indexOf(c))
			{
				I18nString sepV = content.separators.get(content.columns.indexOf(c)).getValue();
				if (sepV != null)
				{
					sepV.toProperties(raw,
							columnKey + VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR,
							msg);

				} else
				{
					raw.put(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_SEPARATOR, "");
				}
			}

			raw.put(columnKey + VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS,
					getColumnContent(msg, c, String.valueOf(columnIt), raw));

			columnIt++;
		}

		return raw;
	}

	public static String getColumnContent(UnityMessageSource msg, AuthnLayoutColumn c, String colIt, Properties raw)
	{
		List<String> elementsRep = new ArrayList<>();

		int sepIt = 1;
		int gridIt = 1;

		for (ColumnElement element : c.getElements())
		{
			if (element instanceof HeaderColumnElement)
			{
				HeaderColumnElement he = (HeaderColumnElement) element;
				String key = saveSeparator(he.getValue(), getSepId(colIt, sepIt++), raw, msg);
				elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_HEADER + "_" + key);
			}

			else if (element instanceof SeparatorColumnElement)
			{
				SeparatorColumnElement he = (SeparatorColumnElement) element;
				String key = saveSeparator(he.getValue(), getSepId(colIt, sepIt++), raw, msg);
				elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_SEPARATOR + "_" + key);
			} else if (element instanceof LastUsedOptionColumnElement)
			{
				elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_LAST_USED);
			} else if (element instanceof RegistrationColumnElement)
			{
				elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_REGISTER);
			} else if (element instanceof SingleAuthnColumnElement)
			{

				SingleAuthnColumnElement se = (SingleAuthnColumnElement) element;
				elementsRep.add(se.getValue());
			} else if (element instanceof GridAuthnColumnElement)
			{

				GridAuthnColumnElement se = (GridAuthnColumnElement) element;
				String key = saveGrid(se.getValue(), getGridId(colIt, gridIt++), raw, msg);
				elementsRep.add(AuthnOptionsColumns.SPECIAL_ENTRY_GRID + key);
			} else if (element instanceof ExpandColumnElement)
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
