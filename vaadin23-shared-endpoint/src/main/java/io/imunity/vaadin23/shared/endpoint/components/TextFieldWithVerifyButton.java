/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import pl.edu.icm.unity.webui.common.ComponentWithLabel;

public class TextFieldWithVerifyButton extends CustomField<String>
{
	private Checkbox adminConfirmCheckBox;
	private TextField editor;
	private HorizontalLayout fieldLayout;
	private VerticalLayout main;
	private Div verifyButtonIcon;
	private Div confirmationStatusIcon;
	private boolean showLabelInline;
	
	public TextFieldWithVerifyButton(boolean addConfirmCheckbox,
	                                 String verifyButtonDesc, Icon verifyButtonIcon,
	                                 String adminConfirmCheckBoxLabel, boolean showLabelInline)
	{
		this.showLabelInline = showLabelInline;
		this.verifyButtonIcon = new Div(verifyButtonIcon);
		verifyButtonIcon.getElement().setProperty("title", verifyButtonDesc);
		verifyButtonIcon.getStyle().set("cursor", "pointer");
		editor = new TextField();
		adminConfirmCheckBox = new Checkbox(adminConfirmCheckBoxLabel);
		fieldLayout = new HorizontalLayout();
		fieldLayout.setMargin(false);
		fieldLayout.setSpacing(true);

		confirmationStatusIcon = new Div();

		fieldLayout.add(editor, confirmationStatusIcon, this.verifyButtonIcon);
		fieldLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

		main = new VerticalLayout();
		main.setMargin(false);
		main.setPadding(false);
		main.add(fieldLayout);
		if (addConfirmCheckbox) 
		{
			main.add(adminConfirmCheckBox);
		}
		add(main);
	}

	@Override
	public void setRequiredIndicatorVisible(boolean visible) 
	{
		editor.setRequiredIndicatorVisible(visible);
	}

	public void setRequired(boolean visible)
	{
		editor.setRequired(visible);
	}

	public void setComponentError(String error)
	{
		editor.setInvalid(error != null);
		editor.setErrorMessage(error);
	}


	@Override
	public void setInvalid(boolean invalid) {
		editor.setInvalid(invalid);
	}

	@Override
	public void setErrorMessage(String errorMessage) {
		editor.setErrorMessage(errorMessage);
	}

	@Override
	public String getErrorMessage() {
		return editor.getErrorMessage();
	}

	@Override
	public String getLabel() {
		return editor.getLabel();
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
	public void setValue(String value)
	{
		editor.setValue(value);
	}
	
	public void setConfirmationStatusIcon(String value, boolean confirmed)
	{
		confirmationStatusIcon.removeAll();
		confirmationStatusIcon.setVisible(true);
		confirmationStatusIcon.add(confirmed ? VaadinIcon.CHECK_CIRCLE_O.create() : VaadinIcon.EXCLAMATION_CIRCLE_O.create());
		confirmationStatusIcon.getElement().setProperty("title", value);
	}

	public void setConfirmationStatusIconVisiable(boolean visible)
	{
		confirmationStatusIcon.setVisible(visible);	
	}
	
	public void removeConfirmationStatusIcon()
	{
		fieldLayout.remove(confirmationStatusIcon);
	}

	public void addTextFieldValueChangeListener(HasValue.ValueChangeListener<? super ComponentValueChangeEvent<TextField, String>> listener)

	{
		editor.addValueChangeListener(listener);
	}

	public void addVerifyButtonClickListener(ComponentEventListener<ClickEvent<Div>> listener)
	{
		verifyButtonIcon.addClickListener(listener);
	}

	public void addAdminConfirmCheckBoxValueChangeListener(HasValue.ValueChangeListener<? super ComponentValueChangeEvent<Checkbox, Boolean>> listener)
	{
		adminConfirmCheckBox.addValueChangeListener(listener);
	}
	
	public void setVerifyButtonVisible(boolean visible)
	{
		verifyButtonIcon.setVisible(visible);
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
		fieldLayout.remove(verifyButtonIcon);
	}

	public void setTextFieldId(String id)
	{
		editor.setId(id);
	}

	@Override
	protected String generateModelValue()
	{
		return editor.getValue();
	}

	@Override
	protected void setPresentationValue(String newPresentationValue)
	{
		editor.setValue(newPresentationValue);
	}

	@Override
	public void setLabel(String label)
	{
		String normalizedLabel = ComponentWithLabel.normalizeLabel(label);
		if (showLabelInline)
			editor.setPlaceholder(normalizedLabel);
		else
			editor.setLabel(normalizedLabel + ":");
	}

	@Override
	public void setWidth(float width, Unit unit)
	{
		if (editor != null)
			editor.setWidth(width, unit);
	}
}
