/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import static io.imunity.vaadin.elements.CssClassNames.INVALID;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;
import static io.imunity.vaadin.elements.CssClassNames.REQUIRED_LABEL;
import static java.util.Optional.ofNullable;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

public class TextFieldWithVerifyButton extends CustomField<String>
{
	private final Checkbox adminConfirmCheckBox;
	private final TextField editor;
	private final Span label;
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
		this.label.setVisible(false);
		verifyButtonIcon.setTooltipText(verifyButtonDesc);
		verifyButtonIcon.addClassName(POINTER.getName());
		editor = new TextField();
		editor.addValueChangeListener(
				e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));
		editor.setTitle("");
		editor.setWidthFull();
		adminConfirmCheckBox = new Checkbox(adminConfirmCheckBoxLabel);
		fieldLayout = new HorizontalLayout();
		fieldLayout.setMargin(false);
		fieldLayout.setSpacing(true);
		fieldLayout.setWidthFull();
		fieldLayout.getStyle().set("position", "relative");

		confirmationStatusIcon = new Div();
		confirmationStatusIcon.setClassName("u-confirmation-status-icon");

		this.verifyButtonIcon.setClassName("u-verify-button-icon");

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
		setWidthFull();
	}

	@Override
	public void setRequiredIndicatorVisible(boolean visible) 
	{
		if(requiredIndicator != null)
		{
			label.remove(requiredIndicator);
			label.setVisible(false);
		}
		if(visible)
		{ 
			label.setVisible(true);
			label.remove();
			requiredIndicator = new Span();
			requiredIndicator.setClassName(REQUIRED_LABEL.getName());
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
			ofNullable(requiredIndicator).ifPresent(dot -> dot.addClassName(INVALID.getName()));
		}
		else
			ofNullable(requiredIndicator).ifPresent(dot -> dot.removeClassName(INVALID.getName()));
	}


	@Override
	public void setInvalid(boolean invalid)
	{
		editor.setInvalid(invalid);
		if(invalid)
			ofNullable(requiredIndicator).ifPresent(dot -> dot.addClassName(INVALID.getName()));
		else
			ofNullable(requiredIndicator).ifPresent(dot -> dot.removeClassName(INVALID.getName()));
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
		if(label == null)
		{
			this.label.setVisible(false);
			this.label.getStyle().set("display", "none");
		}
		else
		{	
			this.label.setVisible(true);
			this.label.getStyle().remove("display");
		}
	}

	@Override
	public void setWidth(float width, Unit unit)
	{
		if (editor != null)
			editor.setWidth(width, unit);
	}

	public void setValueChangeMode(ValueChangeMode valueChangeMode)
	{
		if (valueChangeMode != null)
			editor.setValueChangeMode(valueChangeMode);
		
	}
}
