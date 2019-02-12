/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.function.Function;

import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.webui.authn.AccessBlockedDialog;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;

/**
 * The login component of the 1st factor authentication. Wraps a single Vaadin retrieval UI and connects 
 * it to the authentication screen.  
 * 
 * @author K. Benedyczak
 */
public class FirstFactorAuthNPanel extends AuthNPanelBase implements AuthenticationUIController
{
	private final UnityMessageSource msg;
	private final ExecutorsService execService;
	private final Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider; 
	private final boolean gridCompatible;
	
	public FirstFactorAuthNPanel(UnityMessageSource msg, 
			ExecutorsService execService,
			CancelHandler cancelHandler,
			Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider,
			boolean gridCompatible,
			VaadinAuthenticationUI authnUI,
			String authnId)
	{
		super(authnUI, authnId, new VerticalLayout());
		this.msg = msg;
		this.execService = execService;
		this.unknownUserDialogProvider = unknownUserDialogProvider;
		this.gridCompatible = gridCompatible;

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
		authenticatorContainer.removeAllComponents();
		Component retrievalComponent = gridCompatible ? authnUI.getGridCompatibleComponent() : authnUI.getComponent();
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
	
	void showUnknownUserDialog(UnknownRemoteUserException ee)
	{
		UnknownUserDialog dialog = unknownUserDialogProvider.apply(ee.getResult()); 
		dialog.show();
	}	
}
