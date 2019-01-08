/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

/**
 * Label which is 100% wide -> is wrapped.
 * @author K. Benedyczak
 */
public class Label100 extends Label
{

	public Label100()
	{
		super();
		configure();
	}

	public Label100(String text, ContentMode contentMode)
	{
		super(text, contentMode);
		configure();
	}

	public Label100(String text)
	{
		super(text);
		configure();
	}

	private void configure()
	{
		setWidth(100, Unit.PERCENTAGE);
	}
}
