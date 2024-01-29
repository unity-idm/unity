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

import static io.imunity.vaadin.elements.CSSVars.RICH_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;
import static io.imunity.vaadin.elements.CssClassNames.SMALL_GAP;

public class LocalizedRichTextEditorDetails extends CustomField<Map<Locale, String>>
{
	public Map<Locale, LocalizedRichTextEditor> fields = new LinkedHashMap<>();

	public LocalizedRichTextEditorDetails(Collection<Locale> enabledLocales, Locale currentLocale, Function<Locale, String> valueGenerator)
	{
		setWidth(RICH_FIELD_BIG.value());
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

		LocalizedRichTextEditor defaultField = new LocalizedRichTextEditor(currentLocale);
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
					LocalizedRichTextEditor editor = new LocalizedRichTextEditor(locale);
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
		icon.addClassName(POINTER.getName());
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
}
