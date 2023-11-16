/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.maintenance.audit_log;

import io.imunity.vaadin.endpoint.common.plugins.attributes.components.ConfirmationInfoFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;

@Component
public class IdentityFormatter
{
	@Autowired
	private MessageSource msg;
	@Autowired
	private IdentityTypeSupport idTypeSupport;
	@Autowired
	private ConfirmationInfoFormatter confirmationInfoFormatter;
	
	public String toString(Identity id)
	{
		StringBuilder sb = new StringBuilder();
		IdentityTypeDefinition typeDefinition = idTypeSupport.getTypeDefinition(id.getTypeId());
		boolean verifiable = typeDefinition.isEmailVerifiable();
		sb.append(toStringSimple(id.getValue(), id, verifiable));
		if (id.getCreationTs() != null && id.getUpdateTs() != null)
		{
			sb.append(" ");
			sb.append(msg.getMessage("IdentityFormatter.timestampsInfo", 
					id.getCreationTs(), id.getUpdateTs()));
		}
		return sb.toString();
	}
	
	public String toString(IdentityParam id, IdentityTypeDefinition idType)
	{
		return toStringSimple(id.getValue(), id, idType.isEmailVerifiable());
	}


	private String toStringSimple(String coreValue, IdentityParam id, boolean verifiable)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(msg.getMessage("IdentityFormatter.identityCore", id.getTypeId(), coreValue));
		if (verifiable)
			sb.append(confirmationInfoFormatter.getConfirmationStatusString(id.getConfirmationInfo()));
		sb.append(getRemoteInfoString(id));
		return sb.toString();
	}
	
	private String getRemoteInfoString(IdentityParam id)
	{
		StringBuilder rep = new StringBuilder();
		if (!id.isLocal())
		{
			rep.append(" ");
			rep.append(msg.getMessage("IdentityFormatter.remoteInfo", id.getRemoteIdp()));
			if (id.getTranslationProfile() != null)
			{
				rep.append(" ");
				rep.append(msg.getMessage("IdentityFormatter.profileInfo", id.getTranslationProfile()));
			}
		}
		return rep.toString();
	}
}
