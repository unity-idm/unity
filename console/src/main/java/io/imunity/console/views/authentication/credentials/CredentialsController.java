/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.credentials;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;

import java.util.Collection;
import java.util.List;

@Component
class CredentialsController
{
	private final CredentialManagement credMan;
	private final MessageSource msg;
	private final CredentialEditorRegistry credentialEditorReg;
	private final NotificationPresenter notificationPresenter;

	CredentialsController(CredentialManagement credMan, MessageSource msg,
			CredentialEditorRegistry credentialEditorReg, NotificationPresenter notificationPresenter)
	{
		this.credMan = credMan;
		this.msg = msg;
		this.credentialEditorReg = credentialEditorReg;
		this.notificationPresenter = notificationPresenter;
	}

	Collection<CredentialDefinition> getCredentials()
	{
		try
		{
			return credMan.getCredentialDefinitions();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("LocalCredentialsController.getAllError"), e.getMessage());
		}
		return List.of();
	}

	void removeCredential(CredentialDefinition toRemove, EventsBus bus)
	{
		try
		{
			credMan.removeCredentialDefinition(toRemove.getName());
			bus.fireEvent(new CredentialDefinitionChangedEvent(false, toRemove.getName()));

		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("LocalCredentialsController.removeError", toRemove.getName()), e.getMessage());
		}
	}

	CredentialDefinition getCredential(String credName)
	{
		try
		{
			return credMan.getCredentialDefinition(credName);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("LocalCredentialsController.getError", credName), e.getMessage());
		}
		return null;
	}

	void addCredential(CredentialDefinition toAdd, EventsBus bus)
	{
		try
		{
			credMan.addCredentialDefinition(toAdd);
			bus.fireEvent(new CredentialDefinitionChangedEvent(false, toAdd.getName()));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("LocalCredentialsController.addError", toAdd.getName()), e.getMessage());
		}
	}

	void updateCredential(CredentialDefinition updated, LocalCredentialState state, EventsBus bus)
	{
		try
		{
			credMan.updateCredentialDefinition(updated, state);
			bus.fireEvent(new CredentialDefinitionChangedEvent(true, updated.getName()));
		} catch (Exception e)
		{
			notificationPresenter.showError(
					msg.getMessage("LocalCredentialsController.updateError", updated.getName()), e.getMessage());
		}
	}
}
