/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;

public class LocaleButtonsBar extends HorizontalLayout
{
	public LocaleButtonsBar(Collection<Locale> enabledLocales, String label, Function<Locale, Runnable> valueGenerator)
	{
		getStyle().set("margin-left", "var(--base-margin)");
		enabledLocales.stream().map(locale ->
		{
			Button button = new Button(label, e -> valueGenerator.apply(locale).run());
			button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
			button.setIcon(new FlagIcon(locale.getLanguage()));
			return button;
		}).forEach(this::add);
	}
}
