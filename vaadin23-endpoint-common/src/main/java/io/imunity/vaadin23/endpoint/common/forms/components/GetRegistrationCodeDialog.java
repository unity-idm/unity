/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.endpoint.common.forms.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import pl.edu.icm.unity.MessageSource;


public class GetRegistrationCodeDialog extends ConfirmDialog
{
	private final MessageSource msg;
	private final Callback callback;
	private Binder<CodeBean> binder;
	private final String information;
	private final String codeCaption;

	public GetRegistrationCodeDialog(MessageSource msg, Callback callback,
	                                 String title, String information, String codeCaption)
	{
		this.callback = callback;
		this.information = information;
		this.codeCaption = codeCaption;
		this.msg = msg;
		setHeader(title);
		setCancelable(true);
		addCancelListener(event -> onCancel());
		addConfirmListener(event -> onConfirm());
		add(getContents());
	}
	
	protected Component getContents()
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.add(new Label(information));
		FormLayout sub = new FormLayout();

		TextField codeTextField = new TextField(codeCaption);
		codeTextField.setWidth("70%");
		binder = new Binder<>(CodeBean.class);
		binder.forField(codeTextField)
			.asRequired(msg.getMessage("fieldRequired"))
			.bind("code");
		binder.setBean(new CodeBean());
		
		sub.add(codeTextField);
		main.add(sub);
		return main;
	}
	
	protected void onConfirm()
	{
		if (!binder.isValid())
		{
			binder.validate();			
			return;
		}
		callback.onCodeGiven(binder.getBean().getCode());
		close();
	}
	
	protected void onCancel()
	{
		callback.onCancel();
	}

	public interface Callback
	{
		void onCodeGiven(String code);
		void onCancel();
	}
	
	public static class CodeBean
	{
		String code;
		public void setCode(String code)
		{
			this.code = code;
		}
		public String getCode()
		{
			return code;
		}
	}
}