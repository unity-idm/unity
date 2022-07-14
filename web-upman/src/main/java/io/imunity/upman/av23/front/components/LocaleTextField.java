/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.components;

import com.vaadin.flow.component.textfield.TextField;

import java.util.Locale;

public class LocaleTextField extends TextField
{
	public final Locale locale;

	public LocaleTextField(Locale locale)
	{
		this.locale = locale;
		setSuffixComponent(new FlagIcon(locale.getLanguage()));
	}
}
