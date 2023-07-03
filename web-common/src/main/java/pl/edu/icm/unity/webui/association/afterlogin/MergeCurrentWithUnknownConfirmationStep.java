/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.afterlogin;

import org.apache.logging.log4j.Logger;
import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.webui.association.AbstractConfirmationStep;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Shows confirmation of the account association when the sandbox login should return an unknown user.
 * This user is merged with the currently logged one.
 * @author K. Benedyczak
 */
class MergeCurrentWithUnknownConfirmationStep extends AbstractConfirmationStep
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB,
			MergeCurrentWithUnknownConfirmationStep.class);
	private RemotelyAuthenticatedPrincipal authnContext;
	private Exception mergeError;
	
	MergeCurrentWithUnknownConfirmationStep(MessageSource msg, InputTranslationEngine translationEngine, 
			Wizard wizard)
	{
		super(msg, translationEngine, wizard);
	}

	void setAuthnData(SandboxAuthnEvent event)
	{
		SandboxAuthnContext ctx = event.ctx;
		if (ctx.getAuthnException().isPresent())
		{
			log.info("Sandbox got authn error", ctx.getAuthnException().get());
			setError(msg.getMessage("ConnectId.ConfirmStep.error"));
		} else if (ctx.getRemotePrincipal().isEmpty())
		{
			log.error("Tried to associate local account with local account, what is unsupported and only remote account can be linked to a local one: {}", ctx);
			setError(msg.getMessage("Merging2LocalAccounts.error"));
		} else
		{
			if (!translationEngine.identitiesNotPresentInDb(ctx.getRemotePrincipal().get().getMappingResult()))
			{
				MappedIdentity existingIdentity = translationEngine.getExistingIdentity(
						ctx.getRemotePrincipal().get().getMappingResult());
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
				authnContext = ctx.getRemotePrincipal().get();
				introLabel.setHtmlValue("ConnectId.ConfirmStep.info", authnContext.getRemoteIdPName());
			}
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
