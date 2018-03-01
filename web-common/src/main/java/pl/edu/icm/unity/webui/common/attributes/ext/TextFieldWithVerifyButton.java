/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.attributes.ext;

import com.vaadin.server.ErrorMessage;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 *  Simple verifiable field used by all verifiable attribute handlers. All fields behaviors can be modified from outside. 
 *  This is only simply field and button layout.
 * @author P.Piernik
 *
 */
public class TextFieldWithVerifyButton extends CustomField<String>
{
	private CheckBox adminConfirmCheckBox;
	private Button verifyButton;
	private TextField editor;
	private HorizontalLayout fieldLayout;
	private VerticalLayout main;
	private Label statusIcon;
	
	public TextFieldWithVerifyButton(boolean adminMode, boolean required,
			String verifyButtonDesc, Resource verifyButtonIcon,
			String adminConfirmCheckBoxLabel)
	{
		setRequiredIndicatorVisible(required);
		verifyButton = new Button();
		verifyButton.setIcon(verifyButtonIcon);
		verifyButton.setStyleName(Styles.vButtonLink.toString());
		verifyButton.addStyleName(Styles.vButtonBorderless.toString());
		verifyButton.addStyleName(Styles.link.toString());
		verifyButton.addStyleName(Styles.largeIcon.toString());
		verifyButton.setDescription(verifyButtonDesc);
		editor = new TextField();

		adminConfirmCheckBox = new CheckBox(adminConfirmCheckBoxLabel);
		fieldLayout = new HorizontalLayout();
		fieldLayout.setMargin(false);
		fieldLayout.setSpacing(true);
		
		statusIcon = new Label();
		statusIcon.setContentMode(ContentMode.HTML);
		statusIcon.setStyleName(Styles.largeIcon.toString());
		
		fieldLayout.addComponents(editor, statusIcon,  verifyButton);
	
		main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(fieldLayout);
		if (adminMode)
			main.addComponent(adminConfirmCheckBox);
	}

	@Override
	public String getValue()
	{
		return editor.getValue();
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	protected void doSetValue(String value)
	{
		editor.setValue(value);

	}
	
	public void setStatusIcon(String value, boolean confirmed)
	{
		statusIcon.setValue(confirmed ? Images.ok.getHtml() : Images.remove.getHtml()); 		
		statusIcon.setDescription(value);
	}
		
	public void addTextFieldValueChangeListener(ValueChangeListener<String> listener)

	{
		editor.addValueChangeListener(listener);
	}

	public void addVerifyButtonClickListener(ClickListener listener)
	{
		verifyButton.addClickListener(listener);
	}

	public void addAdminConfirmCheckBoxValueChangeListener(
			ValueChangeListener<Boolean> listener)

	{
		adminConfirmCheckBox.addValueChangeListener(listener);
	}
	
	public void setVerifyButtonVisiable(boolean visible)
	{
		verifyButton.setVisible(visible);
	}

	public void setAdminCheckBoxValue(boolean value)
	{
		adminConfirmCheckBox.setValue(value);
	}

	public boolean getAdminCheckBoxValue()
	{
		return adminConfirmCheckBox.getValue();
	}

	public void removeVerifyButton()
	{
		fieldLayout.removeComponent(verifyButton);
	}

	@Override
	public void setComponentError(ErrorMessage componentError)
	{
		editor.setComponentError(componentError);
	}

	public void setTextFieldId(String id)
	{
		editor.setId(id);
	}

}
