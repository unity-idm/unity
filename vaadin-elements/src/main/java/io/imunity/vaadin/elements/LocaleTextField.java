/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.textfield.TextField;

import java.util.Locale;

public class LocaleTextField extends TextField
{
	public final Locale locale;

	public LocaleTextField(Locale locale)
	{
		this.locale = locale;
		addToSuffix();
		setSuffixComponent(new FlagIcon(locale.getLanguage()));
	}
}
