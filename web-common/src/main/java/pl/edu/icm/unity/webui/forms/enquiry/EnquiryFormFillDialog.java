/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

/**
 * Dialog allowing to fill an enquiry form. It takes an editor component as argument.
 * Dialog uses 3 buttons: submit request, ignore (only if form is not mandatory) and cancel.
 * 
 * @author K. Benedyczak
 */
public class EnquiryFormFillDialog extends AbstractDialog
{
	private EnquiryResponseEditor editor;
	private Callback callback;
	private boolean mandatory;
	private Button ignore;
	
	public EnquiryFormFillDialog(UnityMessageSource msg, String caption, 
			EnquiryResponseEditor editor, Callback callback, EnquiryType type)
	{
		super(msg, caption, msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), 
				msg.getMessage("cancel"));
		this.editor = editor;
		this.callback = callback;
		this.mandatory = type == EnquiryType.REQUESTED_MANDATORY;
		setSizeMode(SizeMode.LARGE);
	}
	
	@Override
	protected AbstractOrderedLayout getButtonsBar()
	{
		AbstractOrderedLayout ret = super.getButtonsBar();
		if (!mandatory)
		{
			ignore = new Button(msg.getMessage("EnquiryFormFillDialog.ignore"), 
					event->callback.ignored());
			ret.addComponent(ignore, 0);
		}
		return ret;
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
			EnquiryResponse request = editor.getRequest();
			if (callback.newRequest(request))
				close();
		} catch (FormValidationException e) 
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
	}
	
	public interface Callback
	{
		boolean newRequest(EnquiryResponse request);
		void cancelled();
		void ignored();
	}
}
