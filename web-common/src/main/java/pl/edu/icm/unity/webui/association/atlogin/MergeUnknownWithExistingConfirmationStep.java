/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.atlogin;

import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.association.AbstractConfirmationStep;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Shows confirmation of the account association when the sandbox login should return an existing user.
 * The unknown user given as input is merged with this existing user. 
 * @author K. Benedyczak
 */
class MergeUnknownWithExistingConfirmationStep extends AbstractConfirmationStep
{
	private final RemotelyAuthenticatedContext unknownUser;
	private AuthenticatedEntity locallyAuthenticatedEntity;
	
	MergeUnknownWithExistingConfirmationStep(UnityMessageSource msg, 
			RemotelyAuthenticatedContext unknownUser,
			InputTranslationEngine translationEngine,
			Wizard wizard)
	{
		super(msg, translationEngine, wizard);
		this.unknownUser = unknownUser;
	}

	void setAuthenticatedUser(AuthenticatedEntity ae)
	{
		locallyAuthenticatedEntity = ae;
		introLabel.setHtmlValue("MergeUnknownWithExistingConfirmationStep.info", 
				unknownUser.getRemoteIdPName(), 
				locallyAuthenticatedEntity.getAuthenticatedWith().get(0));
	}

	@Override
	protected void merge()
	{
		if (locallyAuthenticatedEntity == null)
			return;
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
