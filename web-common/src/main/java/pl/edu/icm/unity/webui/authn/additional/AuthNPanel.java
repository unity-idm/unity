/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.additional;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.webui.authn.AccessBlockedDialog;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * The login component of the additional authentication. Wraps a single Vaadin retrieval UI and connects 
 * it to the additional authentication dialog.
 * 
 * @author K. Benedyczak
 */
class AuthNPanel extends CustomComponent
{
	private final UnityMessageSource msg;
	private final ExecutorsService execService;
	private final VaadinAuthenticationUI authnUI;
	private final VerticalLayout authenticatorContainer;

	
	AuthNPanel(UnityMessageSource msg, ExecutorsService execService, VaadinAuthenticationUI authnUI)
	{
		this.msg = msg;
		this.execService = execService;
		this.authnUI = authnUI;
		this.authenticatorContainer = new VerticalLayout();

		authenticatorContainer.setHeight(100, Unit.PERCENTAGE);
		authenticatorContainer.setWidth(100, Unit.PERCENTAGE);
		authenticatorContainer.setSpacing(false);
		authenticatorContainer.setMargin(false);
		authenticatorContainer.addStyleName("u-authn-component");
		setCompositionRoot(authenticatorContainer);
		setAuthenticator();
	}

	private void setAuthenticator()
	{
		authnUI.disableCredentialReset();
		Component retrievalComponent = authnUI.getComponent();
		authenticatorContainer.addComponent(retrievalComponent);
	}
	
	void showWaitScreenIfNeeded(String clientIp)
	{
		UnsuccessfulAuthenticationCounter counter = StandardWebAuthenticationProcessor.getLoginCounter();
		if (counter.getRemainingBlockedTime(clientIp) > 0)
		{
			AccessBlockedDialog dialog = new AccessBlockedDialog(msg, execService);
			dialog.show();
			return;
		}
	}
	
	void focusIfPossible()
	{
		if (authenticatorContainer.getComponentCount() > 0)
			updateFocus(authenticatorContainer.getComponent(0));
	}

	private void updateFocus(Component retrievalComponent)
	{
		if (retrievalComponent instanceof Focusable)
			((Focusable)retrievalComponent).focus();
	}
}
