/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.forms.registration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

import java.util.Optional;

public class SignUpTopHeaderComponent extends VerticalLayout
{
	private Button gotoSignIn;
	private LocaleChoiceComponent localeChoice;

	public SignUpTopHeaderComponent(UnityServerConfiguration cfg, MessageSource msg,
	                                Optional<Runnable> signInRedirector)
	{
		setMargin(false);
		setSpacing(true);
		getStyle().set("flex-direction", "row-reverse");

		localeChoice = new LocaleChoiceComponent(cfg, msg);

		add(localeChoice);

		if (signInRedirector.isPresent())
		{
			gotoSignIn = new Button(msg.getMessage("StandalonePublicFormView.gotoSignIn"));
			gotoSignIn.addClassName("u-reg-gotoSignIn");
			gotoSignIn.addClickListener(e -> signInRedirector.get().run());
			add(gotoSignIn);
		}
	}

	void setInteractionsEnabled(boolean enabled)
	{
		localeChoice.setEnabled(enabled);
		if (gotoSignIn != null)
			gotoSignIn.setEnabled(enabled);
	}
}
