/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import static java.util.Optional.ofNullable;

public class TextFieldWithVerifyButton extends CustomField<String>
{
	private final Checkbox adminConfirmCheckBox;
	private final TextField editor;
	private final Label label;
	private final HorizontalLayout fieldLayout;
	private final Div verifyButtonIcon;
	private final Div confirmationStatusIcon;
	private final boolean showLabelInline;
	private Span requiredIndicator;

	public TextFieldWithVerifyButton(boolean addConfirmCheckbox,
	                                 String verifyButtonDesc, Icon verifyButtonIcon,
	                                 String adminConfirmCheckBoxLabel, boolean showLabelInline)
	{
		this.showLabelInline = showLabelInline;
		this.verifyButtonIcon = new Div(verifyButtonIcon);
		this.label = new InputLabel("");
		verifyButtonIcon.setTooltipText(verifyButtonDesc);
		verifyButtonIcon.getStyle().set("cursor", "pointer");
		editor = new TextField();
		editor.setWidthFull();
		adminConfirmCheckBox = new Checkbox(adminConfirmCheckBoxLabel);
		fieldLayout = new HorizontalLayout();
		fieldLayout.setMargin(false);
		fieldLayout.setSpacing(true);
		fieldLayout.setWidthFull();
		fieldLayout.getStyle().set("position", "relative");

		confirmationStatusIcon = new Div();
		confirmationStatusIcon.getStyle().set("position", "absolute");
		confirmationStatusIcon.getStyle().set("left", "102%");

		this.verifyButtonIcon.getStyle().set("position", "absolute");
		this.verifyButtonIcon.getStyle().set("left", "110%");

		fieldLayout.add(editor, confirmationStatusIcon, this.verifyButtonIcon);
		fieldLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

		VerticalLayout main = new VerticalLayout();
		main.setPadding(false);
		main.setSpacing(false);
		main.add(label, fieldLayout);
		if (addConfirmCheckbox) 
		{
			main.add(adminConfirmCheckBox);
		}
		add(main);
	}

	@Override
	public void setRequiredIndicatorVisible(boolean visible) 
	{
		if(requiredIndicator != null)
			label.remove(requiredIndicator);
		if(visible)
		{
			label.remove();
			requiredIndicator = new Span();
			requiredIndicator.setClassName("required-label");
			label.add(requiredIndicator);
		}
	}

	public void setRequired(boolean visible)
	{
		editor.setRequired(visible);
	}

	public void setComponentError(String error)
	{
		if(error != null)
		{
			editor.setInvalid(true);
			editor.setErrorMessage(error);
			ofNullable(requiredIndicator).ifPresent(dot -> dot.addClassName("invalid"));
		}
		else
			ofNullable(requiredIndicator).ifPresent(dot -> dot.removeClassName("invalid"));
	}


	@Override
	public void setInvalid(boolean invalid)
	{
		editor.setInvalid(invalid);
		if(invalid)
			ofNullable(requiredIndicator).ifPresent(dot -> dot.addClassName("invalid"));
		else
			ofNullable(requiredIndicator).ifPresent(dot -> dot.removeClassName("invalid"));
	}

	@Override
	public void setErrorMessage(String errorMessage)
	{
		editor.setErrorMessage(errorMessage);
	}

	@Override
	public String getErrorMessage()
	{
		return editor.getErrorMessage();
	}

	@Override
	public String getLabel()
	{
		return label.getText();
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
		Icon icon = confirmed ? VaadinIcon.CHECK_CIRCLE_O.create() : VaadinIcon.EXCLAMATION_CIRCLE_O.create();
		confirmationStatusIcon.add(icon);
		icon.setTooltipText(value);
	}

	public void setConfirmationStatusIconVisiable(boolean visible)
	{
		confirmationStatusIcon.setVisible(visible);	
	}
	
	public void removeConfirmationStatusIcon()
	{
		fieldLayout.remove(confirmationStatusIcon);
	}

	public void addTextFieldValueChangeListener(ValueChangeListener<? super ComponentValueChangeEvent<TextField, String>> listener)

	{
		editor.addValueChangeListener(listener);
	}

	public void addVerifyButtonClickListener(ComponentEventListener<ClickEvent<Div>> listener)
	{
		verifyButtonIcon.addClickListener(listener);
	}

	public void addAdminConfirmCheckBoxValueChangeListener(ValueChangeListener<? super ComponentValueChangeEvent<Checkbox, Boolean>> listener)
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
			this.label.setText(normalizedLabel);
	}

	@Override
	public void setWidth(float width, Unit unit)
	{
		if (editor != null)
			editor.setWidth(width, unit);
	}
}
