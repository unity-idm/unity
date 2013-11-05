/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.FormValidationException;

/**
 * Dialog allowing to fill a registration form. It takes an editor component as argument.
 * Dialog uses 3 buttons: submit request, submit and accept, cancel.
 * @author K. Benedyczak
 */
public class RegistrationRequestEditorDialog extends AbstractDialog
{
	private RegistrationRequestEditor editor;
	private Callback callback;
	private Button submitAndAccept;
	private boolean addAutoAccept;
	
	public RegistrationRequestEditorDialog(UnityMessageSource msg, String caption, 
			RegistrationRequestEditor editor, boolean addAutoAccept, Callback callback)
	{
		super(msg, caption, msg.getMessage("RegistrationRequestEditorDialog.submitRequest"), 
				msg.getMessage("cancel"));
		submitAndAccept = new Button(msg.getMessage("RegistrationRequestEditorDialog.submitAndAccept"), this);
		this.editor = editor;
		this.callback = callback;
		this.addAutoAccept = addAutoAccept;
		setWidth(80, Unit.PERCENTAGE);
		setHeight(85, Unit.PERCENTAGE);
	}

	@Override
	protected AbstractOrderedLayout getButtonsBar()
	{
		AbstractOrderedLayout ret = super.getButtonsBar();
		if (addAutoAccept)
			ret.addComponent(submitAndAccept, 0);
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
	protected void onConfirm()
	{
		onConfirm(false);
	}
	
	private void onSubmitAndAccept()
	{
		onConfirm(true);
	}
	
	private void onConfirm(boolean autoAccept)
	{
		try
		{
			RegistrationRequest request = editor.getRequest();
			if (callback.newRequest(request, autoAccept))
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
		public boolean newRequest(RegistrationRequest request, boolean autoAccept);
	}
	
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == submitAndAccept)
			onSubmitAndAccept();
		super.buttonClick(event);
	}
}
