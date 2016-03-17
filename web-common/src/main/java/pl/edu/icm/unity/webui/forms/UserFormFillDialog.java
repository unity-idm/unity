/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

/**
 * Dialog allowing to fill a registration or enquiry form. It takes an editor component as argument.
 * Dialog uses 2 buttons: submit request, cancel.
 * 
 * @author K. Benedyczak
 */
public class UserFormFillDialog<T extends BaseRegistrationInput> extends AbstractDialog
{
	private BaseRequestEditor<T> editor;
	private Callback<T> callback;
	
	public UserFormFillDialog(UnityMessageSource msg, String caption, 
			BaseRequestEditor<T> editor, Callback<T> callback)
	{
		super(msg, caption, msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), 
				msg.getMessage("cancel"));
		this.editor = editor;
		this.callback = callback;
		setSizeMode(SizeMode.LARGE);
	}

	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_CENTER);
		vl.setHeight(100, Unit.PERCENTAGE);
		return vl;
	}

	@Override
	protected void onCancel()
	{
		callback.cancelled();
		super.onCancel();
	}
	
	@Override
	protected void onConfirm()
	{
		try
		{
			T request = editor.getRequest();
			if (callback.newRequest(request))
				close();
		} catch (FormValidationException e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
	}
	
	public interface Callback<T>
	{
		boolean newRequest(T request);
		void cancelled();
	}
}
