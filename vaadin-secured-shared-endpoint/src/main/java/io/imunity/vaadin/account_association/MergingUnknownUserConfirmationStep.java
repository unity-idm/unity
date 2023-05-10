/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.account_association;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.vaadin.account_association.wizard.WizardStep;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

class MergingUnknownUserConfirmationStep extends WizardStep
{
	private final MessageSource msg;
	private final RemotelyAuthenticatedPrincipal unknownUser;
	AuthenticatedEntity locallyAuthenticatedEntity;

	MergingUnknownUserConfirmationStep(String label, MessageSource msg, RemotelyAuthenticatedPrincipal unknownUser)
	{
		super(label, new HorizontalLayout());
		this.msg = msg;
		this.unknownUser = unknownUser;
	}

	@Override
	protected void initialize()
	{
		((HorizontalLayout)component).removeAll();
		if (locallyAuthenticatedEntity != null)
		{
			String message = msg.getMessage("MergeUnknownWithExistingConfirmationStep.info", unknownUser.getRemoteIdPName(),
					locallyAuthenticatedEntity.getAuthenticatedWith().get(0));
			((HorizontalLayout)component).add(new Label(message));
			stepComplited();
			refreshWizard();
		} else
		{
			String message = msg.getMessage("MergeUnknownWithExistingConfirmationStep.errorNotExistingIdentity");
			((HorizontalLayout)component).add(new Label(message));
			setError("");
		}
	}

	protected void setError(String message)
	{
		((HorizontalLayout)component).add(VaadinIcon.EXCLAMATION_CIRCLE.create());
		((HorizontalLayout)component).add(new Label(message));
		((HorizontalLayout)component).setAlignItems(FlexComponent.Alignment.CENTER);
	}

	void prepareStep(AuthenticatedEntity authenticatedEntity)
	{
		this.locallyAuthenticatedEntity = authenticatedEntity;
	}
}
