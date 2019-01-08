/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.Optional;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.column.RemoteAuthenticationProgress;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Wraps the top locale and remote authN progress component into one. Possibly also the go to sign in link.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class SignUpTopHeaderComponent extends CustomComponent
{
	private RemoteAuthenticationProgress authNProgress;
	private Button gotoSignIn;
	private LocaleChoiceComponent localeChoice;

	public SignUpTopHeaderComponent(UnityServerConfiguration cfg, UnityMessageSource msg, 
			Runnable remoteSignupCancelHandler, Optional<Runnable> signInRedirector)
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
		
		authNProgress = new RemoteAuthenticationProgress(msg, remoteSignupCancelHandler);
		authNProgress.setInternalVisibility(false);
		main.addComponent(authNProgress);
		main.setComponentAlignment(authNProgress, Alignment.TOP_RIGHT);

		setCompositionRoot(main);
	}

	public void setAuthNProgressVisibility(boolean visible)
	{
		authNProgress.setInternalVisibility(visible);
	}
	
	void setInteractionsEnabled(boolean enabled)
	{
		localeChoice.setEnabled(enabled);
		if (gotoSignIn != null)
			gotoSignIn.setEnabled(enabled);
	}
}
