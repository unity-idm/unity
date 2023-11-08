/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;

public class RegistrationFormEditDialog extends ConfirmDialog
{

	public RegistrationFormEditDialog(MessageSource msg, String caption, Callback callback,
			RegistrationFormEditor attributeEditor)
	{
	}

	public interface Callback
	{
		boolean newForm(RegistrationForm form, boolean ignoreRequests);
	}
}
