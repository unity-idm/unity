/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.Locale;

public class LocaleTextArea extends TextArea
{
	public final Locale locale;

	public LocaleTextArea(Locale locale)
	{
		this.locale = locale;
		addToSuffix();
		setValueChangeMode(ValueChangeMode.EAGER);
		setSuffixComponent(new FlagIcon(locale.getLanguage()));
	}
}
