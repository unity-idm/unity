/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.common.Styles;

import java.util.Optional;

public class SignUpTopHeaderComponent extends CustomComponent
{
	private Button gotoSignIn;
	private LocaleChoiceComponent localeChoice;

	public SignUpTopHeaderComponent(UnityServerConfiguration cfg, MessageSource msg, 
			Optional<Runnable> signInRedirector)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(true);
		main.setWidth(100, Unit.PERCENTAGE);

		localeChoice = new LocaleChoiceComponent(cfg, msg);

		main.addComponent(localeChoice);
		main.setComponentAlignment(localeChoice, Alignment.TOP_RIGHT);

		if (signInRedirector.isPresent())
		{
			gotoSignIn = new Button(msg.getMessage("StandalonePublicFormView.gotoSignIn"));
			gotoSignIn.setStyleName(Styles.vButtonLink.toString());
			gotoSignIn.addStyleName("u-reg-gotoSignIn");
			gotoSignIn.addClickListener(e -> signInRedirector.get().run());
			main.addComponent(gotoSignIn);
			main.setComponentAlignment(gotoSignIn, Alignment.TOP_RIGHT);
		}
		
		setCompositionRoot(main);
	}

	void setInteractionsEnabled(boolean enabled)
	{
		localeChoice.setEnabled(enabled);
		if (gotoSignIn != null)
			gotoSignIn.setEnabled(enabled);
	}
}
