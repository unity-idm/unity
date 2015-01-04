/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.confirmations;

/**
 * Confirmation status. Contains user friendly message about confirmation
 * process
 * 
 * @author P. Piernik
 * 
 */
public class ConfirmationStatus
{
	private boolean success;

	public boolean isSuccess()
	{
		return success;
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}

	private String userMessage;

	public ConfirmationStatus(boolean status, String userMessage)
	{
		this.success = status;
		this.userMessage = userMessage;
	}

	public String getUserMessage()
	{
		return userMessage;
	}

	public void setUserMessage(String userMessage)
	{
		this.userMessage = userMessage;
	}
}
