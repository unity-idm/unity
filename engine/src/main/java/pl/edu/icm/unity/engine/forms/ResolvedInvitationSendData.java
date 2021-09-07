/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.forms;

import java.util.Objects;

import pl.edu.icm.unity.types.registration.invite.InvitationSendData;

class ResolvedInvitationSendData
{
	public final InvitationSendData sendData;
	public final String invitationTemplate;
	public final String formDisplayedName;
	public final String code;
	public final String url;
	
	ResolvedInvitationSendData(InvitationSendData sendData, String invitationTemplate, String formDisplayedName,
			String code, String url)
	{
		this.sendData = sendData;
		this.invitationTemplate = invitationTemplate;
		this.formDisplayedName = formDisplayedName;
		this.code = code;
		this.url = url;
	}
	@Override
	public int hashCode()
	{
		return Objects.hash(code, formDisplayedName, invitationTemplate, sendData, url);
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResolvedInvitationSendData other = (ResolvedInvitationSendData) obj;
		return Objects.equals(code, other.code) && Objects.equals(formDisplayedName, other.formDisplayedName)
				&& Objects.equals(invitationTemplate, other.invitationTemplate)
				&& Objects.equals(sendData, other.sendData) && Objects.equals(url, other.url);
	}
}
