/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Formats information about entity's group membership
 * @author K. Benedyczak
 */
public class MembershipFormatter
{
	public static String toString(UnityMessageSource msg, GroupMembership membership)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(msg.getMessage("MembershipFormatter.groupCore", membership.getGroup()));
		sb.append(getRemoteInfoString(msg, membership));
		if (membership.getCreationTs() != null)
		{
			sb.append(" ");
			sb.append(msg.getMessage("MembershipFormatter.timestampInfo", 
					membership.getCreationTs()));
		}
		return sb.toString();
	}
	
	private static String getRemoteInfoString(UnityMessageSource msg, GroupMembership membership)
	{
		StringBuilder rep = new StringBuilder();
		if (membership.getRemoteIdp() != null)
		{
			rep.append(" ");
			rep.append(msg.getMessage("MembershipFormatter.remoteInfo", membership.getRemoteIdp()));
			if (membership.getTranslationProfile() != null)
			{
				rep.append(" ");
				rep.append(msg.getMessage("MembershipFormatter.profileInfo", 
						membership.getTranslationProfile()));
			}
		}
		return rep.toString();
	}
}
