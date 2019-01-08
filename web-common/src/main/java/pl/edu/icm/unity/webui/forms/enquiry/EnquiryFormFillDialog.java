/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.enquiry;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;

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
	private EnquiryType type;
	
	public EnquiryFormFillDialog(UnityMessageSource msg, String caption, 
			EnquiryResponseEditor editor, Callback callback, EnquiryType type)
	{
		super(msg, caption, msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), 
				type == EnquiryType.REQUESTED_MANDATORY ? msg.getMessage("MainHeader.logout") :
					msg.getMessage("cancel"));
		this.editor = editor;
		this.callback = callback;
		this.type = type;
		setSizeMode(SizeMode.LARGE);
	}
	
	@Override
	protected AbstractOrderedLayout getButtonsBar()
	{
		AbstractOrderedLayout ret = super.getButtonsBar();
		if (type == EnquiryType.REQUESTED_OPTIONAL)
		{
			Button ignore = new Button(msg.getMessage("EnquiryFormFillDialog.ignore"), 
					event-> {
						callback.ignored();
						close();
					});
			ret.addComponent(ignore);
		}
		return ret;
	}
	
	@Override
	public void show()
	{
		super.show();
	}
	
	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addComponent(editor);
		vl.setComponentAlignment(editor, Alignment.TOP_CENTER);
		vl.setHeight(100, Unit.PERCENTAGE);
		vl.setMargin(false);
		vl.setSpacing(false);
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
		EnquiryResponse request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
		if (request == null)
			return;
		try
		{
			callback.newRequest(request);
			close();
		} catch (Exception e)
		{
			if (e instanceof IllegalFormContentsException)
				editor.markErrorsFromException((IllegalFormContentsException) e);
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
		}
	}
	
	public interface Callback
	{
		void newRequest(EnquiryResponse request) throws WrongArgumentException;
		void cancelled();
		void ignored();
	}
}
