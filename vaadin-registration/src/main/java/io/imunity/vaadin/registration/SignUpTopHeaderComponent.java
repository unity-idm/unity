/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.endpoint.common.LocaleChoiceComponent;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

import java.util.Optional;

public class SignUpTopHeaderComponent extends VerticalLayout
{
	public SignUpTopHeaderComponent(UnityServerConfiguration cfg, MessageSource msg,
	                                Optional<Runnable> signInRedirector)
	{
		setMargin(false);
		setSpacing(true);
		setAlignItems(Alignment.END);
		getStyle().set("gap", "0");

		LocaleChoiceComponent localeChoice = new LocaleChoiceComponent(cfg);

		add(localeChoice);

		if (signInRedirector.isPresent())
		{
			Div gotoSignIn = new LinkButton(msg.getMessage("StandalonePublicFormView.gotoSignIn"), e -> signInRedirector.get().run());
			gotoSignIn.addClassName("u-reg-gotoSignIn");
			add(gotoSignIn);
		}
	}
}
