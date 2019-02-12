/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Dialog allowing to edit an {@link EnquiryForm}. It takes an editor component as argument.
 * @author K. Benedyczak
 */
public class EnquiryFormEditDialog extends AbstractDialog
{
	private EnquiryFormEditor editor;
	private Callback callback;
	
	public EnquiryFormEditDialog(UnityMessageSource msg, String caption, Callback callback, 
			EnquiryFormEditor editor)
	{
		super(msg, caption);
		this.editor = editor;
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
			EnquiryForm form = editor.getForm();
			
			if (callback.newForm(form, editor.isIgnoreRequestsAndInvitations()))
				close();
		} catch (FormValidationException e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
	}
	
	public interface Callback
	{
		public boolean newForm(EnquiryForm form, boolean ignoreRequest);
	}
}
