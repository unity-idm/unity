/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formfill;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Dialog allowing to fill a registration form. It takes an editor component as argument.
 * @author K. Benedyczak
 */
public class RegistrationRequestEditorDialog extends AbstractDialog
{
	private RegistrationRequestEditor editor;
	private Callback callback;
	
	public RegistrationRequestEditorDialog(UnityMessageSource msg, String caption, 
			RegistrationRequestEditor editor, Callback callback)
	{
		super(msg, caption);
		this.editor = editor;
		this.callback = callback;
		setWidth(80, Unit.PERCENTAGE);
		setHeight(85, Unit.PERCENTAGE);
	}

	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_LEFT);
		vl.setHeight(100, Unit.PERCENTAGE);
		return vl;
	}

	@Override
	protected void onConfirm()
	{
		try
		{
			RegistrationRequest request = editor.getRequest();
			if (callback.newRequest(request))
				close();
		} catch (FormValidationException e) 
		{
			if (e.getMessage() == null || e.getMessage().equals(""))
				ErrorPopup.showError(msg.getMessage("Generic.formError"), 
						msg.getMessage("Generic.formErrorHint"));
			else
				ErrorPopup.showError(msg.getMessage("Generic.formError"), e);
			return;
		}
	}
	
	public interface Callback
	{
		public boolean newRequest(RegistrationRequest request);
	}
}
