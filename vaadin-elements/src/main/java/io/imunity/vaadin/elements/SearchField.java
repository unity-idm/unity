/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import static com.vaadin.flow.component.icon.VaadinIcon.SEARCH;

import java.util.function.Consumer;

public class SearchField extends TextField
{
	public SearchField(String placeholder, Consumer<String> reloader)
	{
		setPlaceholder(placeholder);
		addValueChangeListener(event -> reloader.accept(event.getValue()));
		setPrefixComponent(SEARCH.create());
		setValueChangeMode(ValueChangeMode.EAGER);
	}
}
