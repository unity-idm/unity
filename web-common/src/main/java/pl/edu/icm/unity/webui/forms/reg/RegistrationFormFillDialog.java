/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

/**
 * Dialog allowing to fill a registration form. It takes an editor component as argument.
 * Dialog uses 2 buttons: submit request, cancel.
 * 
 * @author K. Benedyczak
 */
public class RegistrationFormFillDialog extends AbstractDialog
{
	private RegistrationRequestEditor editor;
	private Callback callback;
	
	public RegistrationFormFillDialog(UnityMessageSource msg, String caption, 
			RegistrationRequestEditor editor, Callback callback)
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
			RegistrationRequest request = editor.getRequest();
			if (callback.newRequest(request))
				close();
		} catch (Exception e) 
		{
			if (e instanceof IllegalFormContentsException)
				editor.markErrorsFromException((IllegalFormContentsException) e);
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
	}
	
	public interface Callback
	{
		boolean newRequest(RegistrationRequest request) throws WrongArgumentException;
		void cancelled();
	}
}
