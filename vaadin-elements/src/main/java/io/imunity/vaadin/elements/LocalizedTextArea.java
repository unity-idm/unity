/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.Locale;

public class LocalizedTextArea extends TextArea
{
	public final Locale locale;

	public LocalizedTextArea(Locale locale)
	{
		this.locale = locale;
		setMaxHeight("20em");
		setValueChangeMode(ValueChangeMode.ON_CHANGE);
		setSuffixComponent(new FlagIcon(locale.getLanguage()));
	}
}
