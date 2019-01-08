/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.afterlogin;

import org.apache.logging.log4j.Logger;
import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.local.LocalSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.association.AbstractConfirmationStep;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnEvent;

/**
 * Shows confirmation of the account association when the sandbox login should return an unknown user.
 * This user is merged with the currently logged one.
 * @author K. Benedyczak
 */
class MergeCurrentWithUnknownConfirmationStep extends AbstractConfirmationStep
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			MergeCurrentWithUnknownConfirmationStep.class);
	private RemotelyAuthenticatedContext authnContext;
	private Exception mergeError;
	
	MergeCurrentWithUnknownConfirmationStep(UnityMessageSource msg, InputTranslationEngine translationEngine, 
			Wizard wizard)
	{
		super(msg, translationEngine, wizard);
	}

	void setAuthnData(SandboxAuthnEvent event)
	{
		SandboxAuthnContext ctx = event.getCtx();
		if (ctx instanceof RemoteSandboxAuthnContext)
			setRemoteAuthnData((RemoteSandboxAuthnContext) ctx);
		else
			setLocalAuthnData((LocalSandboxAuthnContext) ctx);
	}
	
	private void setRemoteAuthnData(RemoteSandboxAuthnContext ctx)
	{
		if (ctx.getAuthnException() != null)
		{
			setError(msg.getMessage("ConnectId.ConfirmStep.error"));
		} else
		{
			if (!translationEngine.identitiesNotPresentInDb(ctx.getAuthnContext().getMappingResult()))
			{
				MappedIdentity existingIdentity = translationEngine.getExistingIdentity(
						ctx.getAuthnContext().getMappingResult());
				Entity existingEntity;
				try
				{
					existingEntity = translationEngine.resolveMappedIdentity(existingIdentity);
					LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
					if (existingEntity.getId() == loginSession.getEntityId())
						setError(msg.getMessage("ConnectId.ConfirmStep.errorSameIdentity"));
					else
						setError(msg.getMessage("ConnectId.ConfirmStep.errorExistingIdentity"));
				} catch (EngineException e)
				{
					log.error("Shouldn't happen: existing identity can not be resolved", e);
					setError(msg.getMessage("ConnectId.ConfirmStep.errorExistingIdentity"));
				}
			} else
			{
				authnContext = ctx.getAuthnContext();
				introLabel.setHtmlValue("ConnectId.ConfirmStep.info", authnContext.getRemoteIdPName());
			}
		}
	}

	private void setLocalAuthnData(LocalSandboxAuthnContext ctx)
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
			NotificationPopup.showSuccess(msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"), 
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
