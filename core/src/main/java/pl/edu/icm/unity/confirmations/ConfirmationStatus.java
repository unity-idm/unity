/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

/**
 * Confirmation status. Contains key for user friendly message about confirmation
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

	private String userMessageKey;

	public ConfirmationStatus(boolean status, String userMessage)
	{
		this.success = status;
		this.userMessageKey = userMessage;
	}

	public String getUserMessage()
	{
		return userMessageKey;
	}

	public void setUserMessage(String userMessage)
	{
		this.userMessageKey = userMessage;
	}
}
