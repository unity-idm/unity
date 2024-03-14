/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.safe_html;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

public class HtmlConfigurableLabel
{
	public static String conditionallyEscape(String value)
	{
		boolean allowFullHtml = "true".equals(System.getProperty(UnityServerConfiguration.SYSTEM_ALLOW_FULL_HTML));
		return allowFullHtml ? value : HtmlEscapers.simpleEscape(value);
	}
}
