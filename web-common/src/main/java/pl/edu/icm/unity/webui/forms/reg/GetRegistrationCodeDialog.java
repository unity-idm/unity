/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;

/**
 * Asks user for a registration code.
 *
 * @author Krzysztof Benedyczak
 */
public class GetRegistrationCodeDialog extends AbstractDialog
{
	private TextField codeTextField;
	private GetRegistrationCodeDialog.Callback callback;
	private Binder<CodeBean> binder;
	private String information;
	private String codeCaption;

	public GetRegistrationCodeDialog(UnityMessageSource msg, GetRegistrationCodeDialog.Callback callback,
			String title, String information, String codeCaption)
	{
		super(msg, title);
		this.callback = callback;
		this.information = information;
		this.codeCaption = codeCaption;
		setSize(65, 40);
	}
	
	@Override
	protected Component getContents() throws Exception
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(new Label(information));
		FormLayout sub = new FormLayout();
		
		codeTextField = new TextField(codeCaption);
		codeTextField.setWidth("70%");
		binder = new Binder<>(CodeBean.class);
		binder.forField(codeTextField)
			.asRequired(msg.getMessage("fieldRequired"))
			.bind("code");
		binder.setBean(new CodeBean());
		
		sub.addComponent(codeTextField);
		main.addComponent(sub);
		return main;
	}
	
	@Override
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
	
	@Override
	protected void onCancel()
	{
		callback.onCancel();
		super.onCancel();
	}

	public interface Callback
	{
		public void onCodeGiven(String code);
		public void onCancel();
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