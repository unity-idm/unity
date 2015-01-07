/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.Date;

/**
 * Stores information about entity, besides its identities, credentials and basic information as id.
 * @author K. Benedyczak
 */
public class EntityInformation
{
	private EntityState state;
	private Date timeToRemoveAdmin;
	private Date timeToDisableAdmin;
	private Date timeToRemoveUser;
	
	public EntityInformation(EntityState state)
	{
		this.state = state;
	}

	public EntityState getState()
	{
		return state;
	}

	public void setState(EntityState state)
	{
		this.state = state;
	}

	public Date getTimeToRemoveAdmin()
	{
		return timeToRemoveAdmin;
	}

	public void setTimeToRemoveAdmin(Date timeToRemoveAdmin)
	{
		this.timeToRemoveAdmin = timeToRemoveAdmin;
	}

	public Date getTimeToDisableAdmin()
	{
		return timeToDisableAdmin;
	}

	public void setTimeToDisableAdmin(Date timeToDisableAdmin)
	{
		this.timeToDisableAdmin = timeToDisableAdmin;
	}

	public Date getTimeToRemoveUser()
	{
		return timeToRemoveUser;
	}

	public void setTimeToRemoveUser(Date timeToRemoveUser)
	{
		this.timeToRemoveUser = timeToRemoveUser;
	}
}
