/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.FormLayout;

/**
 * Form layout with fixed width captions
 * @author P.Piernik
 *
 */
public class FormLayoutWithFixedCaptionWidth extends FormLayout
{
	public FormLayoutWithFixedCaptionWidth()
	{
		this(Styles.fixedWidthCaptions);
	}

	public FormLayoutWithFixedCaptionWidth(Styles style)
	{
		addStyleName(style.toString());
		setMargin(true);
	}
	
	public static FormLayoutWithFixedCaptionWidth withShortCaptions()
	{
		return new FormLayoutWithFixedCaptionWidth(Styles.fixedWidthShortCaptions);
	}
	
	public static FormLayoutWithFixedCaptionWidth withMediumCaptions()
	{
		return new FormLayoutWithFixedCaptionWidth(Styles.fixedWidthMediumCaptions);
	}

}
