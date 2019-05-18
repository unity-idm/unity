/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

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
		setSizeMode(SizeMode.LARGE);
	}

	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(false);
		vl.setMargin(false);
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
			
			if (!preCheckForm(form))
				return;
			
			if (callback.newForm(form, editor.isIgnoreRequestsAndInvitations()))
				close();
		} catch (FormValidationException e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
	}
	
	private boolean preCheckForm(RegistrationForm form)
	{
		if (form.isPubliclyAvailable() && form.containsAutomaticAndMandatoryParams() && form.isLocalSignupEnabled())
		{
			ConfirmDialog warning = new ConfirmDialog(msg, 
					msg.getMessage("RegistrationFormEditDialog.publiclAndRemoteWarning"), 
					() -> {
						if (callback.newForm(form, editor.isIgnoreRequestsAndInvitations()))
							RegistrationFormEditDialog.this.close();
					});
			warning.show();
			return false;
		}
		return true;
	}
	
	public interface Callback
	{
		public boolean newForm(RegistrationForm form, boolean ignoreRequests);
	}
}
