/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.afterlogin;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.translation.in.MappedIdentity;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.association.afterlogin.ConnectIdWizardProvider.WizardFinishedCallback;
import pl.edu.icm.unity.wellknownurl.PostAssociationRedirectURLBuilder;
import pl.edu.icm.unity.wellknownurl.SecuredViewProvider;
import pl.edu.icm.unity.wellknownurl.PostAssociationRedirectURLBuilder.Status;

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
	private ConnectIdWellKnownURLProperties connectIdProperties;

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
	public void setEndpointConfiguration(Properties configuration)
	{
		connectIdProperties = new ConnectIdWellKnownURLProperties(configuration); 
	}
	
	private void onCancel()
	{
		String redirect = connectIdProperties.getValue(ConnectIdWellKnownURLProperties.REDIRECT_URL);
		if (redirect == null)
		{
			Page.getCurrent().reload();
		} else
		{
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			String fRedirect = new PostAssociationRedirectURLBuilder(redirect, Status.cancelled).
					setAssociatedInto(loginSession.getEntityId()).toString();
			Page.getCurrent().open(fRedirect, null);
		}
	}
	
	private void onSuccess(MappingResult associated)
	{
		String redirect = connectIdProperties.getValue(ConnectIdWellKnownURLProperties.REDIRECT_URL);
		if (redirect != null)
		{
			MappedIdentity mappedIdentity = associated.getIdentities().get(0);
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			String fRedirect = new PostAssociationRedirectURLBuilder(redirect, Status.success).
					setAssociatedInto(loginSession.getEntityId()).
					setAssociatedInfo(mappedIdentity.getIdentity().getValue(), 
							mappedIdentity.getIdentity().getRemoteIdp()).toString();
			Page.getCurrent().open(fRedirect, null);
		}
	}	

	private void onError(Exception error)
	{
		String redirect = connectIdProperties.getValue(ConnectIdWellKnownURLProperties.REDIRECT_URL);
		if (redirect != null)
		{
			LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
			String fRedirect = new PostAssociationRedirectURLBuilder(redirect, Status.error).
					setAssociatedInto(loginSession.getEntityId()).
					setErrorCode(error.toString()).toString();
			Page.getCurrent().open(fRedirect, null);
		}
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
					public void onCancel()
					{
						ConnectIdWellKnownURLViewProvider.this.onCancel();
					}

					@Override
					public void onSuccess(MappingResult mergedIdentity)
					{
						ConnectIdWellKnownURLViewProvider.this.onSuccess(mergedIdentity);
					}

					@Override
					public void onError(Exception error)
					{
						ConnectIdWellKnownURLViewProvider.this.onError(error);
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
