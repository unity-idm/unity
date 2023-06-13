/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutColumnConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.AuthenticationLayoutContent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.AuthnLayoutColumn;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.components.AuthnLayoutComponentsFactory;

/**
 * Converts authentication layout UI elements to authentication layout
 * configuration elements and revert
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutConfigToUIConverter
{
	public static AuthenticationLayoutContent convertToUI(AuthnLayoutConfiguration config,
			AuthnLayoutComponentsFactory factory)
	{
		List<I18nTextField> separators = new ArrayList<>();
		for (I18nString s : config.separators)
		{
			separators.add(factory.getSeparatorField(s));
		}

		AuthenticationLayoutContent content = new AuthenticationLayoutContent(
				getColumns(config.columns, factory), separators);
		return content;
	}

	private static List<AuthnLayoutColumn> getColumns(List<AuthnLayoutColumnConfiguration> columnsC,
			AuthnLayoutComponentsFactory factory)
	{
		List<AuthnLayoutColumn> columns = new ArrayList<>();

		for (AuthnLayoutColumnConfiguration config : columnsC)
		{
			AuthnLayoutColumn c = factory.getColumn();
			c.setColumnTitle(config.title);
			c.setColumnWidth(config.width);
			c.setElements(getColumnElements(config.contents, factory));
			columns.add(c);
		}

		return columns;

	}

	public static List<ColumnComponent> getColumnElements(List<AuthnElementConfiguration> contents,
			AuthnLayoutComponentsFactory factory)
	{
		List<ColumnComponent> elements = new ArrayList<>();
		if (contents == null)
			return elements;

		for (AuthnElementConfiguration config : contents)
		{
			Optional<ColumnComponent> el = factory.getForType(config);
			if (el.isPresent())
			{
				ColumnComponent elp = el.get();
				elp.setConfigState(config);
				elements.add(elp);
			}
		}
		return elements;
	}

	public static AuthnLayoutConfiguration convertFromUI(AuthenticationLayoutContent config)
	{
		List<I18nString> separators = new ArrayList<>();
		for (I18nTextField s : config.separators)
		{
			separators.add(s.getValue());
		}

		return new AuthnLayoutConfiguration(getColumns(config.columns), separators);
	}

	private static List<AuthnLayoutColumnConfiguration> getColumns(List<AuthnLayoutColumn> columns)
	{
		List<AuthnLayoutColumnConfiguration> columnsConfigs = new ArrayList<>();

		for (AuthnLayoutColumn column : columns)
		{
			columnsConfigs.add(new AuthnLayoutColumnConfiguration(column.getColumnTitle(),
					column.getColumnWidth(), getColumnElements(column.getElements())));
		}

		return columnsConfigs;
	}

	public static List<AuthnElementConfiguration> getColumnElements(List<ColumnComponent> elements)
	{
		List<AuthnElementConfiguration> elemetsC = new ArrayList<>();

		for (ColumnComponent element : elements)
		{
			elemetsC.add(element.getConfigState());
		}

		return elemetsC;
	}
}
