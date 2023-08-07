/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.data.value.ValueChangeMode;
import org.vaadin.pekka.WysiwygE;

import java.util.Locale;

public class LocaleReachEditor extends WysiwygE
{
	public final Locale locale;

	public LocaleReachEditor(Locale locale)
	{
		this.locale = locale;
		setValueChangeMode(ValueChangeMode.EAGER);
		getStyle().set("margin-top", "0.3em");
		setWidth("60em");
		FlagIcon flagIcon = new FlagIcon(locale.getLanguage());
		flagIcon.getStyle().set("position", "absolute");
		flagIcon.getStyle().set("left", "56em");
		flagIcon.getStyle().set("top", "0.8em");
		getElement().appendChild(flagIcon.getElement());
	}
}
