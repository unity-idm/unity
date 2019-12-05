/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutColumnConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.AuthnLayoutConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.ExpandConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.GridConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.HeaderConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.LastUsedConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.RegistrationConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SeparatorConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.SingleAuthnConfig;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.AuthenticationLayoutContent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.AuthnLayoutColumn;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements.AuthnLayoutUIElementsFactory;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements.ExpandColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements.GridAuthnColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements.HeaderColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements.LastUsedOptionColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements.RegistrationColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements.SeparatorColumnElement;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.elements.SingleAuthnColumnElement;

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
			AuthnLayoutUIElementsFactory factory)
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
			AuthnLayoutUIElementsFactory factory)
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

	public static List<ColumnElement> getColumnElements(List<AuthnElementConfiguration> contents,
			AuthnLayoutUIElementsFactory factory)
	{
		List<ColumnElement> elements = new ArrayList<>();
		if (contents == null)
			return elements;

		for (AuthnElementConfiguration config : contents)
		{
			if (config instanceof HeaderConfig)
			{
				HeaderConfig hed = (HeaderConfig) config;
				HeaderColumnElement el = factory.getHeader();
				el.setValue(hed.headerText);
				elements.add(el);
			} else if (config instanceof SeparatorConfig)
			{
				SeparatorConfig sep = (SeparatorConfig) config;
				SeparatorColumnElement el = factory.getSeparator();
				el.setValue(sep.separatorText);
				elements.add(el);
			} else if (config instanceof SingleAuthnConfig)
			{
				SingleAuthnConfig sig = (SingleAuthnConfig) config;
				SingleAuthnColumnElement el = factory.getSingleAuthn();
				el.setValue(sig.authnOption);
				elements.add(el);
			} else if (config instanceof GridConfig)
			{
				GridConfig he = (GridConfig) config;
				GridAuthnColumnElement el = factory.getGrid();
				el.setValue(he);
				elements.add(el);
			} else if (config instanceof ExpandConfig)
			{
				elements.add(factory.getExpand());
			} else if (config instanceof LastUsedConfig)
			{
				elements.add(factory.getLastUsed());
			} else if (config instanceof RegistrationConfig)
			{

				elements.add(factory.getRegistration());
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

	public static List<AuthnElementConfiguration> getColumnElements(List<ColumnElement> elements)
	{
		List<AuthnElementConfiguration> elemetsC = new ArrayList<>();

		for (ColumnElement element : elements)
		{
			if (element instanceof HeaderColumnElement)
			{
				HeaderColumnElement he = (HeaderColumnElement) element;
				elemetsC.add(new HeaderConfig(he.getValue()));
			}

			else if (element instanceof SeparatorColumnElement)
			{
				SeparatorColumnElement he = (SeparatorColumnElement) element;
				elemetsC.add(new SeparatorConfig(he.getValue()));
			} else if (element instanceof LastUsedOptionColumnElement)
			{
				elemetsC.add(new LastUsedConfig());
			} else if (element instanceof RegistrationColumnElement)
			{
				elemetsC.add(new RegistrationConfig());
			} else if (element instanceof SingleAuthnColumnElement)
			{

				SingleAuthnColumnElement se = (SingleAuthnColumnElement) element;
				elemetsC.add(new SingleAuthnConfig(se.getValue()));
			} else if (element instanceof GridAuthnColumnElement)
			{

				GridAuthnColumnElement ge = (GridAuthnColumnElement) element;
				elemetsC.add(ge.getValue());
			} else if (element instanceof ExpandColumnElement)
			{
				elemetsC.add(new ExpandConfig());
			}
		}

		return elemetsC;
	}

}
