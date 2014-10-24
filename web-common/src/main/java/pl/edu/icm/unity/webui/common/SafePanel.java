/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.google.common.html.HtmlEscapers;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;


/**
 * Wraps Vaadin {@link Panel} class in that way that captions have the HTML escaped.
 * 
 * Always use this class in Unity, instead the plain {@link Panel}.
 * 
 * @author K. Benedyczak
 */
public class SafePanel extends Panel
{
	public SafePanel()
	{
		super();
	}

	public SafePanel(Component content)
	{
		super(content);
	}

	public SafePanel(String caption, Component content)
	{
		super(caption, content);
	}

	public SafePanel(String caption)
	{
		super(caption);
	}

	@Override
	public final void setCaption(String caption)
	{
		super.setCaption(HtmlEscapers.htmlEscaper().escape(caption));
	}
}
