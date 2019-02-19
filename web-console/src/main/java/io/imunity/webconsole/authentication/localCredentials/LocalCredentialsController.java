/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.localCredentials;

import java.util.Collection;

import org.springframework.stereotype.Component;

import io.imunity.webadmin.credentials.CredentialDefinitionChangedEvent;
import io.imunity.webadmin.credentials.CredentialDefinitionEditor;
import io.imunity.webadmin.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
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
	private UnityMessageSource msg;
	private CredentialEditorRegistry credentialEditorReg;

	LocalCredentialsController(CredentialManagement credMan, UnityMessageSource msg,
			CredentialEditorRegistry credentialEditorReg)
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
			throw new ControllerException(msg.getMessage("LocalCredentialsController.getAllError"),
					e.getMessage(), e);
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
					msg.getMessage("LocalCredentialsController.removeError", toRemove.getName()),
					e.getMessage(), e);
		}
	}

	CredentialDefinition getCredential(String credName) throws ControllerException
	{
		try
		{
			return credMan.getCredentialDefinition(credName);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("LocalCredentialsController.getError", credName),
					e.getMessage(), e);
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
					msg.getMessage("LocalCredentialsController.addError", toAdd.getName()),
					e.getMessage(), e);
		}
	}

	void updateCredential(CredentialDefinition updated, LocalCredentialState state, EventsBus bus)
			throws ControllerException
	{
		try
		{
			credMan.updateCredentialDefinition(updated, LocalCredentialState.correct);
			bus.fireEvent(new CredentialDefinitionChangedEvent(true, updated.getName()));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("LocalCredentialsController.updateError", updated.getName()),
					e.getMessage(), e);
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
