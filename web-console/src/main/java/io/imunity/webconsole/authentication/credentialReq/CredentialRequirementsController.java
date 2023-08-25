/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.credentialReq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.Collection;

/**
 * Controller for all credential requirements views
 * 
 * @author P.Piernik
 *
 */
@Component("CredentialRequirementsControllerV8")
class CredentialRequirementsController
{
	private MessageSource msg;
	private CredentialRequirementManagement credReqMan;
	private CredentialManagement credMan;

	@Autowired
	CredentialRequirementsController(MessageSource msg, CredentialRequirementManagement credReqMan,
			CredentialManagement credMan)
	{
		this.msg = msg;
		this.credReqMan = credReqMan;
		this.credMan = credMan;
	}

	Collection<CredentialRequirements> getCredentialRequirements() throws ControllerException
	{
		try
		{
			return credReqMan.getCredentialRequirements();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("CredentialRequirementsController.getAllError"),
					e);
		}
	}

	void removeCredentialRequirements(CredentialRequirements toRemove, String replacementId, EventsBus bus)
			throws ControllerException
	{
		try
		{
			credReqMan.removeCredentialRequirement(toRemove.getName(), replacementId);
			bus.fireEvent(new CredentialRequirementChangedEvent());

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("CredentialRequirementsController.removeError",
					toRemove.getName()), e);
		}
	}

	CredentialRequirements getCredentialRequirements(String credReqName) throws ControllerException
	{
		try
		{
			return credReqMan.getCredentialRequirements(credReqName);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("CredentialRequirementsController.getError", credReqName), e);
		}
	}

	void addCredentialRequirements(CredentialRequirements toAdd, EventsBus bus) throws ControllerException
	{
		try
		{
			credReqMan.addCredentialRequirement(toAdd);
			bus.fireEvent(new CredentialRequirementChangedEvent());
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("CredentialRequirementsController.addError", toAdd.getName()),
					e);
		}
	}

	void updateCredentialRequirements(CredentialRequirements updated, EventsBus bus) throws ControllerException
	{
		try
		{
			credReqMan.updateCredentialRequirement(updated);
			bus.fireEvent(new CredentialRequirementChangedEvent());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("CredentialRequirementsController.updateError",
					updated.getName()), e);
		}
	}

	CredentialRequirementEditor getEditor(CredentialRequirements toEdit) throws ControllerException
	{
		try
		{
			if (toEdit == null)
			{
				return new CredentialRequirementEditor(msg, credMan.getCredentialDefinitions());
			} else
			{
				return new CredentialRequirementEditor(msg, credMan.getCredentialDefinitions(), toEdit);
			}

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("CredentialRequirementsController.createEditorError"),
					e);
		}
	}

}
