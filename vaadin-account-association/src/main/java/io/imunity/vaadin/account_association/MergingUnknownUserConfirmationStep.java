/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.account_association;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import io.imunity.vaadin.elements.wizard.WizardStep;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

import static io.imunity.vaadin.elements.CssClassNames.WARNING_ICON;

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
			((HorizontalLayout)component).add(new Span(message));
			stepComplited();
			refreshWizard();
		} else
		{
			String message = msg.getMessage("MergeUnknownWithExistingConfirmationStep.errorNotExistingIdentity");
			((HorizontalLayout)component).add(new Span(message));
			setError("");
		}
	}

	protected void setError(String message)
	{
		Icon icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
		icon.addClassName(WARNING_ICON.getName());
		((HorizontalLayout)component).add(icon);
		((HorizontalLayout)component).add(new Span(message));
		((HorizontalLayout)component).setAlignItems(FlexComponent.Alignment.CENTER);
		interrupt();
	}

	void prepareStep(AuthenticatedEntity authenticatedEntity)
	{
		this.locallyAuthenticatedEntity = authenticatedEntity;
	}
}
