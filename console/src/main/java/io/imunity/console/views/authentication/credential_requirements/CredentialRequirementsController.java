/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.credential_requirements;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;

import java.util.Collection;
import java.util.List;


@Component
class CredentialRequirementsController
{
	private final MessageSource msg;
	private final CredentialRequirementManagement credReqMan;
	private final CredentialManagement credMan;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	CredentialRequirementsController(MessageSource msg, CredentialRequirementManagement credReqMan,
									 CredentialManagement credMan, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.credReqMan = credReqMan;
		this.credMan = credMan;
		this.notificationPresenter = notificationPresenter;
	}

	Collection<CredentialRequirements> getCredentialRequirements()
	{
		try
		{
			return credReqMan.getCredentialRequirements();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialRequirementsController.getAllError"), e.getMessage());
		}
		return List.of();
	}

	Collection<CredentialDefinition> getCredentialRequirementDefinitions()
	{
		try
		{
			return credMan.getCredentialDefinitions();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialRequirementsController.getAllError"), e.getMessage());
		}
		return List.of();
	}

	void removeCredentialRequirements(CredentialRequirements toRemove, String replacementId, EventsBus bus)
	{
		try
		{
			credReqMan.removeCredentialRequirement(toRemove.getName(), replacementId);
			bus.fireEvent(new CredentialRequirementChangedEvent());

		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialRequirementsController.removeError", toRemove.getName()), e.getMessage());
		}
	}

	CredentialRequirements getCredentialRequirements(String credReqName)
	{
		try
		{
			return credReqMan.getCredentialRequirements(credReqName);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialRequirementsController.removeError", credReqName), e.getMessage());
		}
		return null;
	}

	void addCredentialRequirements(CredentialRequirements toAdd, EventsBus bus)
	{
		try
		{
			credReqMan.addCredentialRequirement(toAdd);
			bus.fireEvent(new CredentialRequirementChangedEvent());
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialRequirementsController.addError", toAdd.getName()), e.getMessage());
		}
	}

	void updateCredentialRequirements(CredentialRequirements updated, EventsBus bus)
	{
		try
		{
			credReqMan.updateCredentialRequirement(updated);
			bus.fireEvent(new CredentialRequirementChangedEvent());
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialRequirementsController.updateError", updated.getName()), e.getMessage());
		}
	}
}
