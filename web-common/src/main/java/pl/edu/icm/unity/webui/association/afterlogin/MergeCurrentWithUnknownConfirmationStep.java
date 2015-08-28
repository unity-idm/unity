/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.afterlogin;

import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.LocalSandboxAuthnContext;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.association.AbstractConfirmationStep;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Shows confirmation of the account association when the sandbox login should return an unknown user.
 * This user is merged with the currently logged one.
 * @author K. Benedyczak
 */
public class MergeCurrentWithUnknownConfirmationStep extends AbstractConfirmationStep
{
	private RemotelyAuthenticatedContext authnContext;
	private Exception mergeError;
	
	public MergeCurrentWithUnknownConfirmationStep(UnityMessageSource msg, InputTranslationEngine translationEngine, 
			final Wizard wizard)
	{
		super(msg, translationEngine, wizard);
	}

	@Override
	protected void setRemoteAuthnData(RemoteSandboxAuthnContext ctx)
	{
		if (ctx.getAuthnException() != null)
		{
			setError(msg.getMessage("ConnectId.ConfirmStep.error"));
		} else
		{
			if (!translationEngine.identitiesNotPresentInDb(ctx.getAuthnContext().getMappingResult()))
			{
				setError(msg.getMessage("ConnectId.ConfirmStep.errorExistingIdentity"));
			} else
			{
				authnContext = ctx.getAuthnContext();
				introLabel.setHtmlValue("ConnectId.ConfirmStep.info", authnContext.getRemoteIdPName());
			}
		}
	}

	@Override
	protected void setLocalAuthnData(LocalSandboxAuthnContext ctx)
	{
		AuthenticationResult ae = ctx.getAuthenticationResult();
		if (ae.getStatus() != Status.success)
		{
			setError(msg.getMessage("ConnectId.ConfirmStep.error"));
		} else
		{
			setError(msg.getMessage("ConnectId.ConfirmStep.errorExistingIdentity"));
		}
	}

	@Override
	protected void merge()
	{
		if (authnContext == null)
			return;
			
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		try
		{
			translationEngine.mergeWithExisting(authnContext.getMappingResult(), 
					new EntityParam(loginSession.getEntityId()));
			NotificationPopup.showSuccess(msg, msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"), 
					msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful"));
		} catch (EngineException e)
		{
			this.mergeError = e;
			NotificationPopup.showError(msg, msg.getMessage("ConnectId.ConfirmStep.mergeFailed"), e);
		}
	}
	
	public Exception getMergeError()
	{
		return mergeError;
	}
	
	public MappingResult getMerged()
	{
		return authnContext.getMappingResult();
	}
	
	@Override
	public boolean onAdvance()
	{
		return authnContext != null;
	}
}
