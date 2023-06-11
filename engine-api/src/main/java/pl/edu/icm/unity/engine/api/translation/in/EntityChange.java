/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import java.util.Date;

import pl.edu.icm.unity.base.identity.EntityScheduledOperation;

/**
 * Describes entity status change prescribed by the profile.
 * @author K. Benedyczak
 */
public class EntityChange
{
	private EntityScheduledOperation scheduledOperation;
	private Date scheduledTime;

	public EntityChange(EntityScheduledOperation scheduledOperation, Date scheduledTime)
	{
		this.scheduledOperation = scheduledOperation;
		this.scheduledTime = scheduledTime;
	}
	
	public EntityScheduledOperation getScheduledOperation()
	{
		return scheduledOperation;
	}
	public Date getScheduledTime()
	{
		return scheduledTime;
	}

	@Override
	public String toString()
	{
		return scheduledOperation + ", scheduled on " + scheduledTime;
	}
}
