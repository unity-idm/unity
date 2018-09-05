/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.column.RemoteAuthenticationProgress;

/**
 * Wraps the top locale and remote authN progress component into one.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class SignUpTopHederComponent extends CustomComponent
{
	private RemoteAuthenticationProgress authNProgress;

	public SignUpTopHederComponent(UnityServerConfiguration cfg, UnityMessageSource msg, Runnable cancelHandler)
	{
		initU(cfg, msg, cancelHandler);
	}

	private void initU(UnityServerConfiguration cfg, UnityMessageSource msg, Runnable cancelHandler)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.setWidth(100, Unit.PERCENTAGE);

		LocaleChoiceComponent localeChoice = new LocaleChoiceComponent(cfg, msg);

		main.addComponent(localeChoice);
		main.setComponentAlignment(localeChoice, Alignment.TOP_RIGHT);

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
}
