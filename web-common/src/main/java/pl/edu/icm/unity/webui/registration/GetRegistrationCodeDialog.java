/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.webui.registration;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Asks user for a registration code.
 *
 * @author Krzysztof Benedyczak
 */
class GetRegistrationCodeDialog extends AbstractDialog
{
	private TextField code;
	private GetRegistrationCodeDialog.Callback callback;

	public GetRegistrationCodeDialog(UnityMessageSource msg, GetRegistrationCodeDialog.Callback callback)
	{
		super(msg, msg.getMessage("GetRegistrationCodeDialog.title"));
		this.callback = callback;
		setSizeMode(SizeMode.SMALL);
	}
	
	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.addComponent(new Label(msg.getMessage("GetRegistrationCodeDialog.information")));
		FormLayout sub = new FormLayout();
		code = new RequiredTextField(msg.getMessage("GetRegistrationCodeDialog.code"), msg);
		code.setValidationVisible(false);
		code.setColumns(Styles.WIDE_TEXT_FIELD);
		sub.addComponent(code);
		main.addComponent(sub);
		return main;
	}
	
	@Override
	protected void onConfirm()
	{
		code.setValidationVisible(true);
		try
		{
			code.validate();
		} catch (InvalidValueException e)
		{
			return;
		}
		callback.onCodeGiven(code.getValue());
		close();
	}
	
	@Override
	protected void onCancel()
	{
		callback.onCancel();
		super.onCancel();
	}

	public interface Callback
	{
		public void onCodeGiven(String code);
		public void onCancel();
	}
}