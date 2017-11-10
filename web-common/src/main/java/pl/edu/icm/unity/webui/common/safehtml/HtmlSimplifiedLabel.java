/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.safehtml;

import com.vaadin.v7.shared.ui.label.ContentMode;
import com.vaadin.v7.ui.Label;

/**
 * <b>IMPORTANT!</b> Use this class ONLY to show data which is at least partially trusted. 
 * For instance it is fine to use this class to display text entered by a privileged user, but shouldn't be used
 * to present text entered by an ordinary or unauthenticated user (as when using a registration form).
 * @author K. Benedyczak
 */
public class HtmlSimplifiedLabel extends Label
{
	public HtmlSimplifiedLabel()
	{
		setContentMode(ContentMode.HTML);
	}

	public HtmlSimplifiedLabel(String value)
	{
		this();
		setValue(value);
	}
	
	@Override
	public final void setValue(String value)
	{
		super.setValue(HtmlEscapers.simpleEscape(value));
	}
	
}
