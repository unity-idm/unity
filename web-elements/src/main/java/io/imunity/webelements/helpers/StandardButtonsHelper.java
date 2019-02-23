/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.helpers;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.MarginInfo;
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
 * 
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

	public static HorizontalLayout buildTopButtonsBar(Button... buttons)
	{
		return buildButtonsBar(Alignment.MIDDLE_RIGHT, false, buttons);
	}

	public static Button build4AddAction(UnityMessageSource msg, ClickListener clickListener)
	{
		return buildActionButton(msg.getMessage("addNew"), Images.add, clickListener);
	}

	public static Component buildLinkButton(String caption, ClickListener clickListener)
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

	public static HorizontalLayout buildConfirmNewButtonsBar(UnityMessageSource msg, Runnable onConfirm,
			Runnable onCancel)
	{
		return buildButtonsBar(Alignment.MIDDLE_LEFT, true,
				buildConfirmButton(msg.getMessage("add"), onConfirm),
				buildCancelButton(msg.getMessage("cancel"), onCancel));
	}

	public static HorizontalLayout buildConfirmEditButtonsBar(UnityMessageSource msg, Runnable onConfirm,
			Runnable onCancel)
	{
		return buildButtonsBar(Alignment.MIDDLE_LEFT, true,
				buildConfirmButton(msg.getMessage("update"), onConfirm),
				buildCancelButton(msg.getMessage("cancel"), onCancel));
	}

	public static HorizontalLayout buildShowButtonsBar(UnityMessageSource msg, Runnable onCancel)
	{
		return buildButtonsBar(Alignment.MIDDLE_LEFT, true,
				buildCancelButton(msg.getMessage("close"), onCancel));
	}

	public static HorizontalLayout buildConfirmButtonsBar(String confirmCaption, String cancelCaption,
			Runnable onConfirm, Runnable onCancel)
	{
		if (cancelCaption != null)
		{
			return buildButtonsBar(Alignment.MIDDLE_LEFT, true,
					buildConfirmButton(confirmCaption, onConfirm),
					buildCancelButton(cancelCaption, onCancel));
		} else
		{
			return buildButtonsBar(Alignment.MIDDLE_LEFT, true,
					buildConfirmButton(confirmCaption, onConfirm));
		}
	}

	private static HorizontalLayout buildButtonsBar(Alignment position, boolean vertMargin, Button... buttons)
	{
		HorizontalLayout buttonsBar = new HorizontalLayout();
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(new MarginInfo(vertMargin, false));
		buttonsBar.setMargin(new MarginInfo(vertMargin, false));
		wrapper.addComponents(buttons);
		buttonsBar.addComponent(wrapper);
		buttonsBar.setComponentAlignment(wrapper, position);
		buttonsBar.setWidth(100, Unit.PERCENTAGE);
		return buttonsBar;
	}

	public static Button buildConfirmButton(String confirmCaption, Runnable onConfirm)
	{
		Button confirm = new Button(confirmCaption, e -> onConfirm.run());
		confirm.addStyleName("u-button-form");
		confirm.addStyleName("u-button-action");
		return confirm;
	}

	public static Button buildCancelButton(String cancelCaption, Runnable onCancel)
	{
		Button confirm = new Button(cancelCaption, e -> onCancel.run());
		confirm.addStyleName("u-button-form");
		return confirm;
	}
}
