/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;

import pl.edu.icm.unity.base.message.MessageSource;

import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

/**
 * Helper for creating standard actions buttons
 * 
 * @author P.Piernik
 *
 */
public class StandardButtonsHelper
{
	public static Button buildButton(String caption, Images icon, ClickListener clickListener)
	{
		Button button = new Button();
		button.setIcon(icon.getResource());
		button.setCaption(caption);
		button.addClickListener(clickListener);
		return button;
	}

	public static Button buildActionButton(String caption, Images icon, ClickListener clickListener)
	{
		Button button = buildButton(caption, icon, clickListener);
		button.addStyleName("u-button-action");
		return button;
	}

	public static HorizontalLayout buildTopButtonsBar(Button... buttons)
	{
		return buildButtonsBar(Alignment.MIDDLE_RIGHT, false, buttons);
	}

	public static Button build4AddAction(MessageSource msg, ClickListener clickListener)
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
		button.addStyleName(Styles.gridLinkButton.toString());
		button.addStyleName(Styles.vBorderLess.toString());
		button.addClickListener(clickListener);
		layout.addComponent(button);
		layout.setComponentAlignment(button, Alignment.TOP_LEFT);
		return layout;
	}

	public static HorizontalLayout buildConfirmButtonsBar(MessageSource msg, String confirm, Runnable onConfirm,
			Runnable onCancel)
	{
		return buildButtonsBar(Alignment.MIDDLE_LEFT, true,
				buildCancelButton(msg.getMessage("cancel"), onCancel),
				buildConfirmButton(confirm, onConfirm));
	}
	
	public static HorizontalLayout buildConfirmNewButtonsBar(MessageSource msg, Runnable onConfirm,
			Runnable onCancel)
	{
		return buildConfirmNewButtonsBar(msg, onConfirm, onCancel, Alignment.MIDDLE_LEFT);
	}
	
	public static HorizontalLayout buildConfirmNewButtonsBar(MessageSource msg, Runnable onConfirm,
			Runnable onCancel, Alignment alligment)
	{
		return buildButtonsBar(alligment, true,
				buildCancelButton(msg.getMessage("cancel"), onCancel),
				buildConfirmButton(msg.getMessage("create"), onConfirm));
	}

	public static HorizontalLayout buildConfirmEditButtonsBar(MessageSource msg, Runnable onConfirm,
			Runnable onCancel)
	{
		return buildConfirmEditButtonsBar(msg, onConfirm, onCancel, Alignment.MIDDLE_LEFT);
	}
	
	public static HorizontalLayout buildConfirmEditButtonsBar(MessageSource msg, Runnable onConfirm,
			Runnable onCancel, Alignment alligment)
	{
		return buildButtonsBar(alligment, true,
				buildCancelButton(msg.getMessage("cancel"), onCancel),
				buildConfirmButton(msg.getMessage("update"), onConfirm));
	}

	public static HorizontalLayout buildShowButtonsBar(MessageSource msg, Runnable onCancel)
	{
		return buildButtonsBar(Alignment.MIDDLE_LEFT, true,
				buildCancelButton(msg.getMessage("close"), onCancel));
	}

	public static HorizontalLayout buildConfirmButtonsBar(String confirmCaption, String cancelCaption,
			Runnable onConfirm, Runnable onCancel)
	{
		if (cancelCaption != null)
		{
			return buildButtonsBar(Alignment.MIDDLE_LEFT, true, buildCancelButton(cancelCaption, onCancel),
					buildConfirmButton(confirmCaption, onConfirm));
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
