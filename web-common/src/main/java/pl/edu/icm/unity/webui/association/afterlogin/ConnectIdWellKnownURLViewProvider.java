/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.afterlogin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.association.afterlogin.ConnectIdWizardProvider.WizardFinishedCallback;
import pl.edu.icm.unity.wellknownurl.SecuredViewProvider;

import com.vaadin.navigator.View;
import com.vaadin.server.Page;


/**
 * Provides a view that can be used under a well-known URL to trigger account association.
 * @author K. Benedyczak
 */
@Component
public class ConnectIdWellKnownURLViewProvider implements SecuredViewProvider
{
	public static final String PATH = "account-association";
	private UnityMessageSource msg;
	private InputTranslationEngine translationEngine;
	private SandboxAuthnNotifier sandboxNotifier;
	private String sandboxUrlForAssociation;

	@Autowired
	public ConnectIdWellKnownURLViewProvider(UnityMessageSource msg, InputTranslationEngine translationEngine)
	{
		this.msg = msg;
		this.translationEngine = translationEngine;
	}
	
	@Override
	public String getViewName(String viewAndParameters)
	{
		return viewAndParameters.equals(PATH) ? viewAndParameters : null;
	}

	@Override
	public View getView(String viewName)
	{
		if (!viewName.equals(PATH))
			return null;
	
		ConnectIdWizardProvider wizardProvider = new ConnectIdWizardProvider(msg, sandboxUrlForAssociation, 
				sandboxNotifier, translationEngine, new WizardFinishedCallback()
				{
					@Override
					public void onSuccess()
					{
					}
					
					@Override
					public void onCancel()
					{
						Page.getCurrent().reload();
					}
				});
		
		return new ConnectIdWellKnownURLView(wizardProvider);
	}

	@Override
	public void setSandboxNotifier(SandboxAuthnNotifier sandboxNotifier, String sandboxUrlForAssociation)
	{
		this.sandboxNotifier = sandboxNotifier;
		this.sandboxUrlForAssociation = sandboxUrlForAssociation;
		
	}
}
