/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.registrations;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Dialog allowing to edit a registration form. It takes an editor component as argument, so can be easily used to display 
 * edit dialog for an existing form or form creation dialog.
 * @author K. Benedyczak
 */
public class RegistrationFormEditDialog extends AbstractDialog
{

	private RegistrationFormEditor editor;
	private Callback callback;
	
	public RegistrationFormEditDialog(UnityMessageSource msg, String caption, Callback callback, 
			RegistrationFormEditor attributeEditor)
	{
		super(msg, caption);
		this.editor = attributeEditor;
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
			RegistrationForm form = editor.getForm();
			if (callback.newForm(form, editor.isIgnoreRequests()))
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
		public boolean newForm(RegistrationForm form, boolean update);
	}
}
