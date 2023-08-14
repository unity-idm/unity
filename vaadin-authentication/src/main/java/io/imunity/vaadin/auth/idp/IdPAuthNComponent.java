/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.idp;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKeyUtils;

/**
 * Small widget showing a clickable component presenting a remote IdP. Implemented as Button.
 * Suitable as an authenticate button.
 * Logo + Sing in with ...
 */
public class IdPAuthNComponent extends VerticalLayout
{
	private final Button providerB;

	public IdPAuthNComponent(String id, Image logo, String name)
	{
		providerB = new Button();
		providerB.addClassName("u-idpAuthentication-" + AuthenticationOptionKeyUtils.encodeToCSS(id));
		providerB.setText(name);
		providerB.setWidthFull();
		providerB.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		if (logo != null)
			providerB.setIcon(logo);
		providerB.setTooltipText(name);
		add(providerB);
		setMargin(false);
		setPadding(false);
	}
}
