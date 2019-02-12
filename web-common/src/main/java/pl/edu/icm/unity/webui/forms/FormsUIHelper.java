/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.forms;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;

import pl.edu.icm.unity.webui.common.Styles;

/**
 * Provides common components used in registration and enquiry forms uis
 * @author P.Piernik
 *
 */
public class FormsUIHelper
{
	public static Button createOKButton(String caption,ClickListener clickListener)
	{
		Button okButton = new Button(caption);
		okButton.addStyleName(Styles.vButtonPrimary.toString());
		okButton.addStyleName("u-reg-submit");
		okButton.addClickListener(clickListener);
		okButton.setWidth(100, Unit.PERCENTAGE);
		okButton.setClickShortcut(KeyCode.ENTER);
		return okButton;
	}

	public static Button createCancelButton(String caption,ClickListener clickListener)
	{
		Button cancelButton = new Button(caption);
		cancelButton.addClickListener(clickListener);
		cancelButton.addStyleName("u-reg-cancel");
		cancelButton.setWidth(100, Unit.PERCENTAGE);
		return cancelButton;
	}
}
