/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@CssImport("./styles/components/locale-text-field-details.css")
public class LocaleTextFieldDetails extends CustomField<Map<Locale, String>>
{
	public Map<Locale, LocaleTextField> fields = new LinkedHashMap<>();
	private final HorizontalLayout summary;

	public LocaleTextFieldDetails(Set<Locale> enabledLocales, Locale currentLocale, String label, Function<Locale, String> valueGenerator)
	{
		VerticalLayout content = new VerticalLayout();
		content.setVisible(false);
		content.setPadding(false);
		content.setSpacing(false);

		Icon angleDown = crateIcon(VaadinIcon.ANGLE_DOWN, label);
		Icon angleUp = crateIcon(VaadinIcon.ANGLE_UP, label);

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

		LocaleTextField defaultField = new LocaleTextField(currentLocale);
		defaultField.setValue(valueGenerator.apply(currentLocale));
		defaultField.setLabel(label);
		fields.put(currentLocale, defaultField);

		summary = new HorizontalLayout(defaultField, angleDown, angleUp);
		summary.setAlignItems(FlexComponent.Alignment.CENTER);
		summary.setWidthFull();
		summary.getStyle().set("gap", "0.3em");

		enabledLocales.stream()
				.filter(locale -> !currentLocale.equals(locale))
				.forEach(locale ->
				{
					LocaleTextField localeTextField = new LocaleTextField(locale);
					localeTextField.setValue(valueGenerator.apply(locale));
					content.add(localeTextField);
					fields.put(locale, localeTextField);
				});

		fields.values().forEach(field -> field.addValueChangeListener(event ->
		{
			if(!event.getValue().isBlank())
				setInvalid(false);
		}));

		add(summary, content);
	}

	public void setWidthFull()
	{
		super.setWidthFull();
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

	private Icon crateIcon(VaadinIcon angleDown, String label)
	{
		Icon icon = angleDown.create();
		icon.setColor("unset");
		icon.getStyle().set("cursor", "pointer");
		if(!label.isBlank())
			icon.getStyle().set("margin-top", "2em");
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
		fields.values().iterator().next().setErrorMessage(errorMessage);
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
