/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.localCredentials;

import java.util.Collection;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistryV8;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all local credentials views
 * 
 * @author P.Piernik
 *
 */
@Component
class LocalCredentialsController
{
	private CredentialManagement credMan;
	private MessageSource msg;
	private CredentialEditorRegistryV8 credentialEditorReg;

	LocalCredentialsController(CredentialManagement credMan, MessageSource msg,
			CredentialEditorRegistryV8 credentialEditorReg)
	{
		this.credMan = credMan;
		this.msg = msg;
		this.credentialEditorReg = credentialEditorReg;
	}

	Collection<CredentialDefinition> getCredentials() throws ControllerException
	{
		try
		{
			return credMan.getCredentialDefinitions();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("LocalCredentialsController.getAllError"), e);
		}
	}

	void removeCredential(CredentialDefinition toRemove, EventsBus bus) throws ControllerException
	{
		try
		{
			credMan.removeCredentialDefinition(toRemove.getName());
			bus.fireEvent(new CredentialDefinitionChangedEvent(false, toRemove.getName()));

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("LocalCredentialsController.removeError", toRemove.getName()), e);
		}
	}

	CredentialDefinition getCredential(String credName) throws ControllerException
	{
		try
		{
			return credMan.getCredentialDefinition(credName);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("LocalCredentialsController.getError", credName), e);
		}
	}

	void addCredential(CredentialDefinition toAdd, EventsBus bus) throws ControllerException
	{
		try
		{
			credMan.addCredentialDefinition(toAdd);
			bus.fireEvent(new CredentialDefinitionChangedEvent(false, toAdd.getName()));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("LocalCredentialsController.addError", toAdd.getName()), e);
		}
	}

	void updateCredential(CredentialDefinition updated, LocalCredentialState state, EventsBus bus)
			throws ControllerException
	{
		try
		{
			credMan.updateCredentialDefinition(updated, state);
			bus.fireEvent(new CredentialDefinitionChangedEvent(true, updated.getName()));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("LocalCredentialsController.updateError", updated.getName()), e);
		}
	}

	CredentialDefinitionEditor getEditor(CredentialDefinition initial)
	{
		return new CredentialDefinitionEditor(msg, credentialEditorReg, initial);
	}

	CredentialDefinitionViewer getViewer(CredentialDefinition initial)
	{
		CredentialDefinitionViewer viewer = new CredentialDefinitionViewer(msg);
		viewer.setInput(initial, credentialEditorReg.getFactory(initial.getTypeId()));
		return viewer;
	}
}
