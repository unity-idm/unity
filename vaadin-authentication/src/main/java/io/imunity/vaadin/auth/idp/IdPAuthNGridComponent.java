/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.idp;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Simplified version of {@link IdPAuthNComponent} - no logo, other style, same otherwise
 */
public class IdPAuthNGridComponent extends VerticalLayout
{
	private final Button providerB;

	public IdPAuthNGridComponent(String id, String name)
	{
		providerB = new Button();
		providerB.addClassName("u-idpAuthentication-" + id);
		providerB.setText(name);
		providerB.setTooltipText(name);
		providerB.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		providerB.setWidthFull();
		providerB.addClassName("u-text-left");
		add(providerB);
		setMargin(false);
		setPadding(false);
	}

	public void addButtonClickListener(ComponentEventListener<ClickEvent<Button>> listener)
	{
		providerB.addClickListener(listener);
	}
}
