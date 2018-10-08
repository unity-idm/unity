/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.Optional;

import org.apache.logging.log4j.util.Strings;

import com.vaadin.server.Page;
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

	public SignUpTopHeaderComponent(UnityServerConfiguration cfg, UnityMessageSource msg, Runnable cancelHandler,
			Optional<String> signInRedirect)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(true);
		main.setWidth(100, Unit.PERCENTAGE);

		localeChoice = new LocaleChoiceComponent(cfg, msg);

		main.addComponent(localeChoice);
		main.setComponentAlignment(localeChoice, Alignment.TOP_RIGHT);

		if (signInRedirect.isPresent() && !Strings.isEmpty(signInRedirect.get()))
		{
			gotoSignIn = new Button(msg.getMessage("StandalonePublicFormView.gotoSignIn"));
			gotoSignIn.setStyleName(Styles.vButtonLink.toString());
			gotoSignIn.addStyleName("u-reg-gotoSignIn");
			gotoSignIn.addClickListener(e -> 
				Page.getCurrent().open(signInRedirect.get(), null));
			main.addComponent(gotoSignIn);
			main.setComponentAlignment(gotoSignIn, Alignment.TOP_RIGHT);
		}
		
		authNProgress = new RemoteAuthenticationProgress(msg, cancelHandler);
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
