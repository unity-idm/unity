/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formfill;

import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;

/**
 * Dialog allowing to fill an enquiry or registration form. Intended to be used from the AdminUI.
 * It takes an editor component as argument. Dialog uses 3 buttons: submit request, submit and accept, cancel.
 * 
 * @author K. Benedyczak
 */
public class AdminFormFillDialog<T extends BaseRegistrationInput> extends AbstractDialog
{
	private BaseRequestEditor<T> editor;
	private Callback<T> callback;
	private Button submitAndAccept;
	
	public AdminFormFillDialog(UnityMessageSource msg, String caption, 
			BaseRequestEditor<T> editor, Callback<T> callback)
	{
		super(msg, caption, msg.getMessage("UserFormFillDialog.submitRequest"), 
				msg.getMessage("cancel"));
		submitAndAccept = new Button(msg.getMessage("UserFormFillDialog.submitAndAccept"), this);
		this.editor = editor;
		this.callback = callback;
		setSizeMode(SizeMode.LARGE);
	}

	@Override
	protected AbstractOrderedLayout getButtonsBar()
	{
		AbstractOrderedLayout ret = super.getButtonsBar();
		ret.addComponent(submitAndAccept, 0);
		return ret;
	}
	
	@Override
	protected Component getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setMargin(false);
		vl.setSpacing(false);
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
			T request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
			if (request == null)
				return;
			callback.newRequest(request, autoAccept);
			close();
		} catch (Exception e) 
		{
			if (e instanceof IllegalFormContentsException)
				editor.markErrorsFromException((IllegalFormContentsException) e);
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
			return;
		}
	}
	
	public interface Callback<T>
	{
		void newRequest(T request, boolean autoAccept) throws WrongArgumentException;
		void cancelled();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getSource() == submitAndAccept)
			onSubmitAndAccept();
		super.buttonClick(event);
	}
}
