/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;

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
		setSize(65, 40);
	}
	
	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(new Label(msg.getMessage("GetRegistrationCodeDialog.information")));
		FormLayout sub = new FormLayout();
		code = new TextField(msg.getMessage("GetRegistrationCodeDialog.code"));
		code.setRequiredIndicatorVisible(true);
		code.setWidth(80, Unit.PERCENTAGE);
		sub.addComponent(code);
		main.addComponent(sub);
		return main;
	}
	
	@Override
	protected void onConfirm()
	{
		if (code.isEmpty())
		{
			code.setComponentError(
					new UserError(msg.getMessage("fieldRequired")));
			return;
		}
		
		code.setComponentError(null);
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