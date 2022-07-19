/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.components;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import static com.vaadin.flow.component.icon.VaadinIcon.SEARCH;

public class SearchField extends TextField
{
	public SearchField(String placeholder, Runnable reloader)
	{
		setPlaceholder(placeholder);
		addValueChangeListener(event -> reloader.run());
		setPrefixComponent(SEARCH.create());
		setValueChangeMode(ValueChangeMode.EAGER);
	}
}
