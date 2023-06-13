/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.atlogin;

import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.webui.association.AbstractConfirmationStep;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Shows confirmation of the account association when the sandbox login should return an existing user.
 * The unknown user given as input is merged with this existing user. 
 * @author K. Benedyczak
 */
class MergeUnknownWithExistingConfirmationStep extends AbstractConfirmationStep
{
	private final RemotelyAuthenticatedPrincipal unknownUser;
	private AuthenticatedEntity locallyAuthenticatedEntity;
	
	MergeUnknownWithExistingConfirmationStep(MessageSource msg, 
			RemotelyAuthenticatedPrincipal unknownUser,
			InputTranslationEngine translationEngine,
			Wizard wizard)
	{
		super(msg, translationEngine, wizard);
		this.unknownUser = unknownUser;
	}

	void setAuthenticatedUser(AuthenticatedEntity ae)
	{
		locallyAuthenticatedEntity = ae;
		if (ae != null)
		{
			introLabel.setHtmlValue("MergeUnknownWithExistingConfirmationStep.info", unknownUser.getRemoteIdPName(),
					locallyAuthenticatedEntity.getAuthenticatedWith().get(0));
		} else
		{
			introLabel.setHtmlValue("MergeUnknownWithExistingConfirmationStep.errorNotExistingIdentity");
			//block finish button
			errorComponent.setVisible(true);
		}
	}

	@Override
	protected void merge()
	{
		if (locallyAuthenticatedEntity == null)
		{
			NotificationPopup.showError(msg.getMessage("ConnectId.ConfirmStep.mergeFailed"), "");
			return;
		}
		EntityParam existing = new EntityParam(locallyAuthenticatedEntity.getEntityId());
		try
		{
			translationEngine.mergeWithExisting(unknownUser.getMappingResult(), existing);
			NotificationPopup.showSuccess(msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"), 
					msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful"));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("ConnectId.ConfirmStep.mergeFailed"), e);
		}
	}
	
	@Override
	public boolean onAdvance()
	{
		return locallyAuthenticatedEntity != null;
	}
}
