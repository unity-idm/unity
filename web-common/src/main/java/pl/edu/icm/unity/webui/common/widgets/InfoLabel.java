/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.widgets;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Displays small information/help text, typically applicable for UI view or its section. 
 * Rather not useful for individual component help. 
 */
public class InfoLabel extends Label
{
	public InfoLabel(String infoText)
	{
		super(Images.info_circle.getHtml() + " " + infoText, ContentMode.HTML);
		addStyleName(Styles.textInfoTooltip.toString());
		setWidth(100, Unit.PERCENTAGE);
	}
}
