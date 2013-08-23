/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.notifications;

import java.util.concurrent.Future;

/**
 * Notification status. Not an exception as the status is retrieved asynchronously, via a {@link Future}.
 * @author K. Benedyczak
 */
public class NotificationStatus
{
	private Exception problem;

	public NotificationStatus(Exception problem)
	{
		this.problem = problem;
	}
	
	public NotificationStatus()
	{
		this.problem = null;
	}
	
	public boolean isSuccessful()
	{
		return problem == null;
	}
	
	public Exception getProblem()
	{
		return problem;
	}

	public void setProblem(Exception problem)
	{
		this.problem = problem;
	}
}
