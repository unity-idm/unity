/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.identityTypes;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.IdentityTypesManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all identity type views
 * 
 * @author P.Piernik
 *
 */
@Component
class IdentityTypesController
{
	private MessageSource msg;
	private IdentityTypesManagement idMan;
	private IdentityTypeSupport idTypeSupport;
	private MessageTemplateManagement msgTemplateMan;

	@Autowired
	IdentityTypesController(MessageSource msg, IdentityTypesManagement idMan,
			IdentityTypeSupport idTypeSupport, MessageTemplateManagement msgTemplateMan)
	{
		this.msg = msg;
		this.idMan = idMan;
		this.idTypeSupport = idTypeSupport;
		this.msgTemplateMan = msgTemplateMan;
	}

	Collection<IdentityTypeEntry> getIdentityTypes() throws ControllerException
	{
		try
		{
			return idMan.getIdentityTypes().stream().map(
					t -> new IdentityTypeEntry(t, idTypeSupport.getTypeDefinition(t.getName())))
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("IdentityTypesController.getAllError"), e);
		}
	}

	IdentityType getIdentityType(String name) throws ControllerException
	{
		try
		{
			return idMan.getIdentityType(name);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("IdentityTypesController.getError", name), e);
		}
	}

	void updateIdentityType(IdentityType idType, EventsBus bus) throws ControllerException
	{
		try
		{
			idMan.updateIdentityType(idType);
			bus.fireEvent(new IdentityTypesUpdatedEvent(Sets.newHashSet(idType)));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("IdentityTypesController.updateError", idType.getName()), e);
		}
	}

	public IdentityTypeEditor getEditor(IdentityType idType)
	{
		return new IdentityTypeEditor(msg, idTypeSupport, msgTemplateMan, idType);
	}

}
