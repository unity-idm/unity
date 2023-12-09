/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.account_association;

import com.vaadin.flow.component.Component;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.wizard.WizardStep;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;

class FinalConnectIdAtLoginStep extends WizardStep
{
	private final InputTranslationEngine translationEngine;
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;
	private final RemotelyAuthenticatedPrincipal unknownUser;

	AuthenticatedEntity locallyAuthenticatedEntity;


	FinalConnectIdAtLoginStep(String label, Component component,
	                          InputTranslationEngine translationEngine, NotificationPresenter notificationPresenter, MessageSource msg,
	                          RemotelyAuthenticatedPrincipal unknownUser)
	{
		super(label, component);
		this.translationEngine = translationEngine;
		this.notificationPresenter = notificationPresenter;
		this.msg = msg;
		this.unknownUser = unknownUser;
	}

	@Override
	protected void initialize()
	{
		if (locallyAuthenticatedEntity == null)
		{
			notificationPresenter.showError(msg.getMessage("ConnectId.ConfirmStep.mergeFailed"), "");
			return;
		}
		EntityParam existing = new EntityParam(locallyAuthenticatedEntity.getEntityId());
		try
		{
			translationEngine.mergeWithExisting(unknownUser.getMappingResult(), existing);
			notificationPresenter.showSuccess(
					msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"),
					msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful")
			);
			wizard.close();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("ConnectId.ConfirmStep.mergeFailed"), e.getMessage());
		}
	}

	void prepareStep(AuthenticatedEntity locallyAuthenticatedEntity)
	{
		this.locallyAuthenticatedEntity = locallyAuthenticatedEntity;
	}

}
