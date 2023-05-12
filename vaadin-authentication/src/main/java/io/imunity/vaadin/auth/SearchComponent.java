/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import pl.edu.icm.unity.MessageSource;

import java.util.function.Consumer;

public class SearchComponent extends VerticalLayout
{
	public SearchComponent(MessageSource msg, Consumer<String> filterChangedCallback)
	{
		TextField search = new TextField();
		search.setPlaceholder(msg.getMessage("IdpSelectorComponent.filter"));
		search.addValueChangeListener(event -> filterChangedCallback.accept(search.getValue()));
		search.addClassName("u-authn-search");
		setMargin(false);
		setPadding(false);
		add(search);
	}
}
