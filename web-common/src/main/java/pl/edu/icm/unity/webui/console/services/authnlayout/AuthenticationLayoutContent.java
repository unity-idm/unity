/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.console.services.authnlayout;

import java.util.List;

import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * 
 * @author P.Piernik
 *
 */
public class AuthenticationLayoutContent
{
	public final List<AuthnLayoutColumn> columns;
	public final List<I18nTextField> separators;

	public AuthenticationLayoutContent(List<AuthnLayoutColumn> columns, List<I18nTextField> separators)
	{
		this.columns = columns;
		this.separators = separators;
	}

}