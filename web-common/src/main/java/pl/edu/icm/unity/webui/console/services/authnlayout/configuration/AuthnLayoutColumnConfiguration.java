/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.configuration;

import java.util.List;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;

/**
 * Stores information about authentication layout column configuration
 * 
 * @author P.Piernik
 *
 */
public class AuthnLayoutColumnConfiguration
{
	public final I18nString title;
	public final int width;
	public final List<AuthnElementConfiguration> contents;

	public AuthnLayoutColumnConfiguration(I18nString title, int width, List<AuthnElementConfiguration> contents)
	{
		this.title = title;
		this.width = width;
		this.contents = contents;
	}
}
