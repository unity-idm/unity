/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;

public class EnquiryFormEditDialog extends ConfirmDialog
{
	public EnquiryFormEditDialog(MessageSource msg, String caption, Callback callback,
			EnquiryFormEditor editor)
	{
	}

	public interface Callback
	{
		boolean newForm(EnquiryForm form, boolean ignoreRequest);
	}
}
