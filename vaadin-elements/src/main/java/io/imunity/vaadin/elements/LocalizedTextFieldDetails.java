/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.CSSVars.BASE_MARGIN;
import static io.imunity.vaadin.elements.VaadinClassNames.EMPTY_DETAILS_ICON;
import static io.imunity.vaadin.elements.VaadinClassNames.SMALL_GAP;

public class LocalizedTextFieldDetails extends CustomField<Map<Locale, String>> implements LocalizedErrorMessageHandler
{
	public Map<Locale, LocalizedTextField> fields = new LinkedHashMap<>();
	private final HorizontalLayout summary;

	public LocalizedTextFieldDetails(Collection<Locale> enabledLocales, Locale currentLocale)
	{
		this(enabledLocales, currentLocale, Optional.empty(), locale -> "");
	}

	public LocalizedTextFieldDetails(Collection<Locale> enabledLocales, Locale currentLocale, Optional<String> label, Function<Locale, String> valueGenerator)
	{
		VerticalLayout content = new VerticalLayout();
		content.setVisible(false);
		content.setPadding(false);
		content.setSpacing(false);

		Icon angleDown = crateIcon(VaadinIcon.ANGLE_DOWN, label);
		angleDown.getStyle().set("margin-top", BASE_MARGIN.value());
		Icon angleUp = crateIcon(VaadinIcon.ANGLE_UP, label);
		angleUp.getStyle().set("margin-top", BASE_MARGIN.value());

		angleUp.setVisible(false);
		angleDown.addClickListener(event ->
		{
			angleDown.setVisible(false);
			angleUp.setVisible(true);
			content.setVisible(true);
		});
		angleUp.addClickListener(event ->
		{
			angleDown.setVisible(true);
			angleUp.setVisible(false);
			content.setVisible(false);
		});

		LocalizedTextField defaultField = new LocalizedTextField(currentLocale);
		defaultField.setValue(valueGenerator.apply(currentLocale));
		label.ifPresent(defaultField::setLabel);
		fields.put(currentLocale, defaultField);

		summary = new HorizontalLayout(defaultField, angleDown, angleUp);
		summary.setWidthFull();
		summary.setClassName(SMALL_GAP.getName());

		enabledLocales.stream()
				.filter(locale -> !currentLocale.equals(locale))
				.forEach(locale ->
				{
					LocalizedTextField localizedTextField = new LocalizedTextField(locale);
					localizedTextField.setValue(valueGenerator.apply(locale));
					content.add(localizedTextField);
					fields.put(locale, localizedTextField);
				});

		fields.values().forEach(field -> field.addValueChangeListener(event ->
		{
			if(!event.getValue().isBlank())
				setInvalid(false);
		}));

		add(summary, content);
		propagateValueChangeEventFromNestedTextFieldToThisComponent();
	}

	private void propagateValueChangeEventFromNestedTextFieldToThisComponent()
	{
		fields.values().forEach(localizedTextField -> localizedTextField.addValueChangeListener(event ->
				fireEvent(new ComponentValueChangeEvent<>(this, event.getHasValue(), event.getOldValue(), event.isFromClient()))
		));
	}

	public void setWidthFull()
	{
		super.setWidth("100%");
		fields.values().forEach(HasSize::setWidthFull);
	}

	public void setWidth(String width)
	{
		summary.setWidth(width);
		fields.values().forEach(HasSize::setWidthFull);
	}

	public void focus()
	{
		fields.values().iterator().next().focus();
	}

	private Icon crateIcon(VaadinIcon angleDown, Optional<String> label)
	{
		Icon icon = angleDown.create();
		icon.addClassName("u-details-icon");
		if(label.isPresent())
			icon.setClassName(EMPTY_DETAILS_ICON.getName());
		return icon;
	}

	@Override
	public void setValue(Map<Locale, String> value)
	{
		fields.forEach((key, val) -> fields.get(key).setValue(value.getOrDefault(key, "")));
	}

	@Override
	public Map<Locale, String> getValue()
	{
		return fields.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getValue()));
	}

	@Override
	public void setReadOnly(boolean readOnly)
	{
		super.setReadOnly(readOnly);
		fields.values().forEach(field -> field.setReadOnly(readOnly));
	}

	public void addValuesChangeListener(BiConsumer<HasValue<?, String>, Integer> consumer)
	{
		fields.values().forEach(field -> field.getElement().addEventListener(
				"keyup",
				e -> field.getElement().executeJs("return this.inputElement.selectionStart")
						.then(Integer.class, pos -> consumer.accept(field, pos)))
		);

		fields.values().forEach(field -> field.addFocusListener(
				e -> field.getElement().executeJs("return this.inputElement.selectionStart")
						.then(Integer.class, pos -> consumer.accept(field, pos)))
		);
	}

	@Override
	public void setErrorMessage(String errorMessage)
	{
		if(errorMessage.isBlank())
			return;
		fields.values().iterator().next().setErrorMessage(errorMessage);
	}

	@Override
	public void setErrorMessage(Locale locale, String errorMessage)
	{
		fields.get(locale).setErrorMessage(errorMessage);
	}

	@Override
	public String getErrorMessage()
	{
		return fields.values().iterator().next().getErrorMessage();
	}

	@Override
	protected Map<Locale, String> generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(Map<Locale, String> newPresentationValue)
	{
		setValue(newPresentationValue);
	}

	@Override
	public void setInvalid(boolean invalid)
	{
		super.setInvalid(invalid);
		fields.values().forEach(field -> field.setInvalid(invalid));
		getElement().getParent().getClassList().set("invalid", invalid);
		getElement().getParent().getClassList().set("valid", !invalid);
	}
}
