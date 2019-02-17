/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.helpers;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Helper for creating standard actions buttons
 * @author P.Piernik
 *
 */
public class StandardButtonsHelper
{
	public static Button buildActionButton(String caption, Images icon, ClickListener clickListener)
	{
		Button button = new Button();
		button.setIcon(icon.getResource());
		button.setCaption(caption);
		button.addStyleName("u-button-action");
		button.addClickListener(clickListener);
		return button;
	}

	public static Button build4AddAction(UnityMessageSource msg, ClickListener clickListener)
	{
		return buildActionButton(msg.getMessage("add"), Images.add, clickListener);
	}
		
	public static HorizontalLayout buildButtonsBar(Button... buttons)
	{
		HorizontalLayout buttonsBar = new HorizontalLayout();
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);	
		buttonsBar.setMargin(false);
		wrapper.addComponents(buttons);
		buttonsBar.addComponent(wrapper);
		buttonsBar.setComponentAlignment(wrapper, Alignment.MIDDLE_RIGHT);
		buttonsBar.setWidth(100, Unit.PERCENTAGE);
		return buttonsBar;
	}
	
	public static Component getLinkButton(String caption, ClickListener clickListener)
	{
		HorizontalLayout layout = new HorizontalLayout();
		layout.setSpacing(false);
		layout.setMargin(false);
		layout.setWidth(100, Unit.PERCENTAGE);
		Button button = new Button();
		button.setCaption(caption);
		button.addStyleName(Styles.vButtonLink.toString());
		button.addStyleName(Styles.vBorderLess.toString());
		button.addClickListener(clickListener);
		layout.addComponent(button);
		layout.setComponentAlignment(button, Alignment.TOP_LEFT);
		return layout;
	}
}
