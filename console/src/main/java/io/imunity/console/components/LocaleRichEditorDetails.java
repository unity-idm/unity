/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.components;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.imunity.vaadin.elements.VaadinClassNames.SMALL_GAP;

public class LocaleRichEditorDetails extends CustomField<Map<Locale, String>>
{
	public Map<Locale, LocaleRichEditor> fields = new LinkedHashMap<>();

	public LocaleRichEditorDetails(Collection<Locale> enabledLocales, Locale currentLocale, Function<Locale, String> valueGenerator)
	{
		VerticalLayout content = new VerticalLayout();
		content.setVisible(false);
		content.setPadding(false);
		content.setSpacing(false);

		Icon angleDown = crateIcon(VaadinIcon.ANGLE_DOWN);
		angleDown.getStyle().set("margin-left", "2em");
		Icon angleUp = crateIcon(VaadinIcon.ANGLE_UP);
		angleUp.getStyle().set("margin-left", "2em");

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

		LocaleRichEditor defaultField = new LocaleRichEditor(currentLocale);
		defaultField.setValue(valueGenerator.apply(currentLocale));
		defaultField.addValueChangeListener(e -> setInvalid(false));
		fields.put(currentLocale, defaultField);

		HorizontalLayout summary = new HorizontalLayout(defaultField, angleDown, angleUp);
		summary.setAlignItems(FlexComponent.Alignment.BASELINE);
		summary.setWidthFull();
		summary.setClassName(SMALL_GAP.getName());

		enabledLocales.stream()
				.filter(locale -> !currentLocale.equals(locale))
				.forEach(locale ->
				{
					LocaleRichEditor editor = new LocaleRichEditor(locale);
					editor.setValue(valueGenerator.apply(locale));
					content.add(editor);
					fields.put(locale, editor);
				});

		add(summary, content);
	}

	@Override
	public void setWidthFull()
	{
		super.setWidthFull();
		fields.values().forEach(HasSize::setWidthFull);
	}

	private Icon crateIcon(VaadinIcon angleDown)
	{
		Icon icon = angleDown.create();
		icon.setColor("unset");
		icon.getStyle().set("cursor", "pointer");
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
	public void setInvalid(boolean invalid)
	{
		super.setInvalid(invalid);
		fields.values().forEach(field -> field.setInvalid(invalid));
		getElement().getParent().getClassList().set("invalid", invalid);
		getElement().getParent().getClassList().set("valid", !invalid);
	}
}
