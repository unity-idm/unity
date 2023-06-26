/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import com.vaadin.flow.data.value.ValueChangeMode;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.function.Consumer;

public class SearchComponent extends VerticalLayout
{
	public SearchComponent(MessageSource msg, Consumer<String> filterChangedCallback)
	{
		TextField search = new TextField();
		search.setPlaceholder(msg.getMessage("IdpSelectorComponent.filter"));
		search.addValueChangeListener(event -> filterChangedCallback.accept(search.getValue()));
		search.addClassName("u-authn-search");
		search.setValueChangeMode(ValueChangeMode.EAGER);
		setPadding(false);
		setAlignItems(Alignment.END);
		add(search);
	}
}
