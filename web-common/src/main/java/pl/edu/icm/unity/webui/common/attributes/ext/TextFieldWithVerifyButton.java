/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.attributes.ext;

import com.vaadin.server.ErrorMessage;
import com.vaadin.server.Resource;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.ComponentWithLabel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 *  Simple verifiable field used by all verifiable attribute handlers. All fields behaviors can be modified from outside. 
 *  This is only simply field and button layout.
 * @author P.Piernik
 *
 */
public class TextFieldWithVerifyButton extends CustomField<String> implements ComponentWithLabel
{
	private CheckBox adminConfirmCheckBox;
	private Button verifyButton;
	private TextField editor;
	private HorizontalLayout fieldLayout;
	private VerticalLayout main;
	private Label confirmationStatusIcon;
	private boolean showLabelInline;
	
	public TextFieldWithVerifyButton(boolean adminMode, 
			String verifyButtonDesc, Resource verifyButtonIcon,
			String adminConfirmCheckBoxLabel, boolean showLabelInline)
	{
		this.showLabelInline = showLabelInline;
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
		
		confirmationStatusIcon = new Label();
		confirmationStatusIcon.setContentMode(ContentMode.HTML);
		
		fieldLayout.addComponents(editor, confirmationStatusIcon, verifyButton);
		fieldLayout.setComponentAlignment(confirmationStatusIcon, Alignment.MIDDLE_CENTER);
		
		main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(fieldLayout);
		if (adminMode) 
		{
			main.addComponent(adminConfirmCheckBox);
		}
			
	}
	
	@Override
	public void setRequiredIndicatorVisible(boolean visible) 
	{
		if (showLabelInline)
			editor.setRequiredIndicatorVisible(visible);
		else
			super.setRequiredIndicatorVisible(visible);
	}


	@Override
	public void setComponentError(ErrorMessage componentError)
	{
		editor.setComponentError(componentError);
	}
	
	public Registration addValueChangeListener(ValueChangeListener<String> listener)
	{
		return editor.addValueChangeListener(listener);
	}
	
	@Override
	public void focus()
	{
		editor.focus();
	}

	@Override
	public String getValue()
	{
		return editor.getValue();
	}

	@Override
	public String getEmptyValue() 
	{
		return "";
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
	
	public void setConfirmationStatusIcon(String value, boolean confirmed)
	{
		confirmationStatusIcon.setVisible(true);
		confirmationStatusIcon.setValue(confirmed ? Images.ok.getHtml() : Images.warn.getHtml()); 		
		confirmationStatusIcon.setDescription(value);
	}

	public void setConfirmationStatusIconVisiable(boolean visible)
	{
		confirmationStatusIcon.setVisible(visible);	
	}
	
	public void removeConfirmationStatusIcon()
	{
		fieldLayout.removeComponent(confirmationStatusIcon); 		
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
	
	public void setVerifyButtonVisible(boolean visible)
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

	public void setTextFieldId(String id)
	{
		editor.setId(id);
	}
	
	@Override
	public void setLabel(String label)
	{
		String normalizedLabel = ComponentWithLabel.normalizeLabel(label);
		if (showLabelInline)
			editor.setPlaceholder(normalizedLabel);
		else
			setCaption(normalizedLabel + ":");
	}

	@Override
	public void setWidth(float width, Unit unit)
	{
		if (editor != null)
			editor.setWidth(width, unit);
	}
}
