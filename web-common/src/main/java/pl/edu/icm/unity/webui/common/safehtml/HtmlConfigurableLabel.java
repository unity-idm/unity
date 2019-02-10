/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.safehtml;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * This component is a {@link Label} which can behave in two variants depending on configuration.
 * If system property {@link UnityServerConfiguration#SYSTEM_ALLOW_FULL_HTML} is set to 'true', then this
 * label behaves as a regular {@link Label} with HTML mode. Otherwise this object behaves exactly 
 * as {@link HtmlSimplifiedLabel} - i.e. only basic HTML formatting tags are allowed, the rest is escaped.
 * 
 * @author K. Benedyczak
 */
public class HtmlConfigurableLabel extends Label
{
	public HtmlConfigurableLabel()
	{
		setContentMode(ContentMode.HTML);
		addStyleName("wrap-line");
	}

	public HtmlConfigurableLabel(String value)
	{
		this();
		setValue(value);
	}
	
	@Override
	public final void setValue(String value)
	{
		super.setValue(conditionallyEscape(value));
	}
	
	public static String conditionallyEscape(String value)
	{
		boolean allowFullHtml = "true".equals(System.getProperty(UnityServerConfiguration.SYSTEM_ALLOW_FULL_HTML));
		return allowFullHtml ? value : HtmlEscapers.simpleEscape(value);
	}
}
