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
	private String userMessageKey;
	private String[] userMessageArgs;
	
	public boolean isSuccess()
	{
		return success;
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}
	
	public ConfirmationStatus(boolean status, String userMessage, String...userMessageArgs)
	{
		this.success = status;
		this.userMessageKey = userMessage;
		this.userMessageArgs = userMessageArgs;
	}

	public String getUserMessageKey()
	{
		return userMessageKey;
	}

	public void setUserMessageKey(String userMessageKey)
	{
		this.userMessageKey = userMessageKey;
	}
	
	public String[] getUserMessageArgs()
	{
		return userMessageArgs;
	}

	public void setUserMessageArgs(String[] userMessageArgs)
	{
		this.userMessageArgs = userMessageArgs;
	}
}
