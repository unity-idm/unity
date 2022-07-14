/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.components;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.MessageSource;

import java.util.LinkedList;
import java.util.List;

public class LocaleTextFieldDetails extends VerticalLayout
{
	public List<LocaleTextField> fields = new LinkedList<>();

	public LocaleTextFieldDetails(MessageSource msg, String label)
	{
		VerticalLayout content = new VerticalLayout();
		content.setVisible(false);
		content.setPadding(false);

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

		LocaleTextField defaultField = new LocaleTextField(msg.getLocale());
		defaultField.setLabel(label);
		fields.add(defaultField);

		HorizontalLayout summary = new HorizontalLayout(defaultField, angleDown, angleUp);
		summary.setAlignItems(Alignment.CENTER);
		summary.getStyle().set("gap", "0.3em");

		msg.getEnabledLocales().values().stream()
				.filter(locale -> !msg.getLocale().equals(locale))
				.forEach(locale ->
				{
					LocaleTextField localeTextField = new LocaleTextField(locale);
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
