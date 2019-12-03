/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration;

import java.util.List;

import pl.edu.icm.unity.types.I18nString;

/**
 * Stores information about authentication layout configuration
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutConfiguration
{
	public final List<AuthnLayoutColumnConfiguration> columns;
	public final List<I18nString> separators;

	public AuthnLayoutConfiguration(List<AuthnLayoutColumnConfiguration> columns, List<I18nString> separators)
	{
		this.columns = columns;
		this.separators = separators;
	}
}
