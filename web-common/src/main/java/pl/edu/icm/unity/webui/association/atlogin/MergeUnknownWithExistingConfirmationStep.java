/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.atlogin;

import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.LocalSandboxAuthnContext;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.translation.in.MappedIdentity;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.association.AbstractConfirmationStep;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Shows confirmation of the account association when the sandbox login should return an existing user.
 * The unknown user given as input is merged with this existing user. 
 * @author K. Benedyczak
 */
public class MergeUnknownWithExistingConfirmationStep extends AbstractConfirmationStep
{
	private final RemotelyAuthenticatedContext unknownUser;
	
	private AuthenticatedEntity locallyAuthenticatedEntity;
	private MappedIdentity remotelyAuthenticatedExistingIdentity;
	
	public MergeUnknownWithExistingConfirmationStep(UnityMessageSource msg, 
			InputTranslationEngine translationEngine, Wizard wizard,
			RemotelyAuthenticatedContext unknownUser)
	{
		super(msg, translationEngine, wizard);
		this.unknownUser = unknownUser;
	}

	@Override
	protected void setRemoteAuthnData(RemoteSandboxAuthnContext ctx)
	{
		if (ctx.getAuthnException() != null)
		{
			setError(msg.getMessage("ConnectId.ConfirmStep.error"));
		} else
		{
			remotelyAuthenticatedExistingIdentity = translationEngine.getExistingIdentity(
					ctx.getAuthnContext().getMappingResult());
			if (remotelyAuthenticatedExistingIdentity == null)
			{
				setError(msg.getMessage("ConnectId.ConfirmStep.errorNotExistingIdentity"));
			} else
			{
				introLabel.setHtmlValue("MergeUnknownWithExistingConfirmationStep.info", 
						unknownUser.getRemoteIdPName(),
						remotelyAuthenticatedExistingIdentity.getIdentity().getValue());
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
			locallyAuthenticatedEntity = ctx.getAuthenticationResult().getAuthenticatedEntity();
			introLabel.setHtmlValue("MergeUnknownWithExistingConfirmationStep.info", 
					unknownUser.getRemoteIdPName(), 
					locallyAuthenticatedEntity.getAuthenticatedWith().get(0));
		}
	}

	@Override
	protected void merge()
	{
		if (remotelyAuthenticatedExistingIdentity == null && locallyAuthenticatedEntity == null)
			return;
		EntityParam existing;
		if (locallyAuthenticatedEntity != null)
			existing = new EntityParam(locallyAuthenticatedEntity.getEntityId());
		else
			existing = new EntityParam(remotelyAuthenticatedExistingIdentity.getIdentity());
		
		try
		{
			translationEngine.mergeWithExisting(unknownUser.getMappingResult(), existing);
			NotificationPopup.showSuccess(msg, msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"), 
					msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful"));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("ConnectId.ConfirmStep.mergeFailed"), e);
		}
	}
	
	@Override
	public boolean onAdvance()
	{
		return remotelyAuthenticatedExistingIdentity != null || locallyAuthenticatedEntity != null;
	}
}
