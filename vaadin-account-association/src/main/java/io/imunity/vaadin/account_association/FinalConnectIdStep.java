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
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;

class FinalConnectIdStep extends WizardStep
{
	private final InputTranslationEngine translationEngine;
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;
	private final Runnable finishTask;

	private RemotelyAuthenticatedPrincipal authnContext;

	public FinalConnectIdStep(String label, Component component,
	                          InputTranslationEngine translationEngine, NotificationPresenter notificationPresenter,
							  MessageSource msg, Runnable finishTask)
	{
		super(label, component);
		this.translationEngine = translationEngine;
		this.notificationPresenter = notificationPresenter;
		this.msg = msg;
		this.finishTask = finishTask;
	}

	@Override
	protected void initialize()
	{
		if (authnContext == null)
			return;
			
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		try
		{
			translationEngine.mergeWithExisting(authnContext.getMappingResult(), 
					new EntityParam(loginSession.getEntityId()));
			finishTask.run();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("ConnectId.ConfirmStep.mergeFailed"), e.getMessage());
		}
	}

	void prepareStep(RemotelyAuthenticatedPrincipal authnContext)
	{
		this.authnContext = authnContext;
	}

}
