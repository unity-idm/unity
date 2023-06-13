/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.registration;

import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.NotificationPopup;

class SubmissionErrorHandler
{
	static void handleFormSubmissionError(Exception e, MessageSource msg, RegistrationRequestEditor editor, NotificationPresenter notificationPresenter)
	{
		if (e instanceof IllegalFormContentsException)
		{
			editor.markErrorsFromException((IllegalFormContentsException) e);
			if (e instanceof IllegalFormContentsException.OccupiedIdentityUsedInRequest)
			{
				String identity = ((IllegalFormContentsException.OccupiedIdentityUsedInRequest) e).occupiedIdentity.getValue();
				notificationPresenter.showError(msg.getMessage("FormRequest.occupiedIdentity", identity), "");
			} else
			{
				notificationPresenter.showError(msg.getMessage("Generic.formError"), e.getMessage());
			}
		} else
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
		}
	}
}
