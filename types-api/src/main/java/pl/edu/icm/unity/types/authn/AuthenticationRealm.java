/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import pl.edu.icm.unity.types.DescribedObjectImpl;

/**
 * Authentication realm definition.
 * @author K. Benedyczak
 */
public class AuthenticationRealm extends DescribedObjectImpl
{
	private int blockAfterUnsuccessfulLogins;
	private int blockFor;
	private int allowForRememberMeDays;
	private int maxInactivity;

	public AuthenticationRealm()
	{
	}

	public AuthenticationRealm(String name, String description, int blockAfterUnsuccessfulLogins, int blockFor,
			int allowForRememberMeDays, int maxInactivity)
	{
		super(name, description);
		this.blockAfterUnsuccessfulLogins = blockAfterUnsuccessfulLogins;
		this.blockFor = blockFor;
		this.allowForRememberMeDays = allowForRememberMeDays;
		this.maxInactivity = maxInactivity;
	}

	public int getBlockAfterUnsuccessfulLogins()
	{
		return blockAfterUnsuccessfulLogins;
	}
	public void setBlockAfterUnsuccessfulLogins(int blockAfterUnsuccessfulLogins)
	{
		this.blockAfterUnsuccessfulLogins = blockAfterUnsuccessfulLogins;
	}
	public int getBlockFor()
	{
		return blockFor;
	}
	public void setBlockFor(int blockFor)
	{
		this.blockFor = blockFor;
	}
	public int getAllowForRememberMeDays()
	{
		return allowForRememberMeDays;
	}
	public void setAllowForRememberMeDays(int allowForRememberMeDays)
	{
		this.allowForRememberMeDays = allowForRememberMeDays;
	}
	public int getMaxInactivity()
	{
		return maxInactivity;
	}
	public void setMaxInactivity(int maxInactivity)
	{
		this.maxInactivity = maxInactivity;
	}
}
