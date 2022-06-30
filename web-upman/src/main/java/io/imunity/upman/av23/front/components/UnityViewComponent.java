/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.av23.front.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;


public abstract class UnityViewComponent extends Composite<Div> implements HasUrlParameter<String> {

	public UnityViewComponent() {
		getContent().setClassName("unity-view");
		getContent().setHeightFull();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {}


	protected void addPreventionForMultiEnterClick() {
		Shortcuts.addShortcutListener(getContent(), event -> {}, Key.ENTER);
	}
}
