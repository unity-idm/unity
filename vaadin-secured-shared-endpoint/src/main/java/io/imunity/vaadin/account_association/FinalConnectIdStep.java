/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.account_association;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import io.imunity.vaadin.account_association.wizard.WizardStep;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;

import java.util.Map;

class FinalConnectIdStep extends WizardStep
{
	private final InputTranslationEngine translationEngine;
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;

	private RemotelyAuthenticatedPrincipal authnContext;

	public FinalConnectIdStep(String label, Component component,
	                          InputTranslationEngine translationEngine, NotificationPresenter notificationPresenter, MessageSource msg)
	{
		super(label, component);
		this.translationEngine = translationEngine;
		this.notificationPresenter = notificationPresenter;
		this.msg = msg;
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
			UI.getCurrent().navigate(StatusView.class, QueryParameters.simple(Map.of(
					StatusView.TITLE_PARAM, msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"),
					StatusView.DESCRIPTION_PARAM, msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful"))
			));
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
