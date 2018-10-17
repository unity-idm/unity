/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores information about which elements were prefilled by invitation
 * 
 * @author K. Benedyczak
 */
public class InvitationPrefillInfo
{
	private boolean byInvitation;
	private Map<Integer, Boolean> prefilledIdentitites = new HashMap<>();
	private Map<Integer, Boolean> prefilledAttributes = new HashMap<>();
	
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

	public void setPrefilledIdentity(int index)
	{
		prefilledIdentitites.put(index, true);
	}
	
	public boolean isIdentityPrefilled(int index)
	{
		return prefilledIdentitites.getOrDefault(index, false);
	}

	public void setPrefilledAttribute(int index)
	{
		prefilledAttributes.put(index, true);
	}
	
	public boolean isAttributePrefilled(int index)
	{
		return prefilledAttributes.getOrDefault(index, false);
	}
}
