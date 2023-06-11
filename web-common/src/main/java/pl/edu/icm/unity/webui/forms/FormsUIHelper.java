/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.forms;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;

import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException.OccupiedIdentityUsedInRequest;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Provides common components used in registration and enquiry forms uis
 * @author P.Piernik
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
	
	public static void handleFormSubmissionError(Exception e, MessageSource msg, BaseRequestEditor<?> editor)
	{
		if (e instanceof IllegalFormContentsException)
		{
			editor.markErrorsFromException((IllegalFormContentsException) e);
			if (e instanceof OccupiedIdentityUsedInRequest)
			{
				String identity = ((OccupiedIdentityUsedInRequest) e).occupiedIdentity.getValue();
				NotificationPopup.showError(msg.getMessage("FormRequest.occupiedIdentity", identity), "");
			} else
			{
				NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			}
		} else
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
		}
	}
}
