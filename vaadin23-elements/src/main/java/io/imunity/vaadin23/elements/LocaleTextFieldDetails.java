/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

@CssImport("./styles/components/locale-text-field-details.css")
public class LocaleTextFieldDetails extends VerticalLayout
{
	public List<LocaleTextField> fields = new LinkedList<>();

	public LocaleTextFieldDetails(Set<Locale> enabledLocales, Locale currentLocale, String label, Function<Locale, String> valueGenerator)
	{
		VerticalLayout content = new VerticalLayout();
		content.setVisible(false);
		content.setPadding(false);

		Icon angleDown = crateIcon(VaadinIcon.ANGLE_DOWN, label);
		angleDown.getStyle().set("margin-bottom", "0.7em");
		Icon angleUp = crateIcon(VaadinIcon.ANGLE_UP, label);
		angleUp.getStyle().set("margin-bottom", "0.7em");

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
		fields.add(defaultField);

		HorizontalLayout summary = new HorizontalLayout(defaultField, angleDown, angleUp);
		summary.setAlignItems(Alignment.END);
		summary.getStyle().set("gap", "0.3em");

		enabledLocales.stream()
				.filter(locale -> !currentLocale.equals(locale))
				.forEach(locale ->
				{
					LocaleTextField localeTextField = new LocaleTextField(locale);
					localeTextField.setValue(valueGenerator.apply(locale));
					content.add(localeTextField);
					fields.add(localeTextField);
				});

		add(summary, content);
		setPadding(false);
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
}
