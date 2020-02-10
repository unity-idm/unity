/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

public class InvitationPrefillInfo
{
	private boolean byInvitation;
	
	public InvitationPrefillInfo()
	{
		this(false);
	}

	public InvitationPrefillInfo(boolean byInvitation)
	{
		this.byInvitation = byInvitation;
	}

	public boolean isByInvitation()
	{
		return byInvitation;
	}
}
