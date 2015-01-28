/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities;

import java.sql.Date;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Utility class presenting an {@link Identity} or {@link IdentityParam} in web-ready and readable form.
 * 
 * @author K. Benedyczak
 */
public class IdentityFormatter
{
	public static String toString(UnityMessageSource msg, Identity id)
	{
		StringBuilder sb = new StringBuilder();
		boolean verifiable = id.getType().getIdentityTypeProvider().isVerifiable();
		sb.append(toStringSimple(msg, id.getValue(), id, verifiable));
		if (id.getCreationTs() != null && id.getUpdateTs() != null)
		{
			sb.append(" ");
			sb.append(msg.getMessage("IdentityFormatter.timestampsInfo", 
					id.getCreationTs(), id.getUpdateTs()));
		}
		return sb.toString();
	}
	
	public static String toString(UnityMessageSource msg, IdentityParam id, IdentityTypeDefinition idType)
	{
		return toStringSimple(msg, id.getValue(), id, idType.isVerifiable());
	}


	private static String toStringSimple(UnityMessageSource msg, String coreValue, IdentityParam id, 
			boolean verifiable)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(msg.getMessage("IdentityFormatter.identityCore", id.getTypeId(), coreValue));
		if (verifiable)
			sb.append(getConfirmationStatusString(msg, id.getConfirmationInfo()));
		sb.append(getRemoteInfoString(msg, id));
		return sb.toString();
	}
	
	public static String getConfirmationStatusString(UnityMessageSource msg, ConfirmationInfo cdata)
	{
		StringBuilder rep = new StringBuilder();
		if (cdata != null)
		{
			rep.append(" ");
			if (cdata.isConfirmed())
			{
				rep.append(msg.getMessage("VerifiableEmail.confirmed",
						new Date(cdata.getConfirmationDate())));
			} else
			{
				if (cdata.getSentRequestAmount() == 0)
					rep.append(msg.getMessage("VerifiableEmail.unconfirmed"));
				else
					rep.append(msg.getMessage("VerifiableEmail.unconfirmedWithRequests",
							cdata.getSentRequestAmount()));
			}
		}
		return rep.toString();
	}
	
	private static String getRemoteInfoString(UnityMessageSource msg, IdentityParam id)
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
