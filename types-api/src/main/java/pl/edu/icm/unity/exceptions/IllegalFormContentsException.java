/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when there is problem with submitted form (enquiry, registration etc).
 * Provides details useful for UI to mark wrong field. 
 * @author K. Benedyczak
 */
public class IllegalFormContentsException extends WrongArgumentException
{
	private int position;
	private Category category;

	public enum Category {CREDENTIAL, AGREEMENT, ATTRIBUTE, IDENTITY, GROUP};
	
	public IllegalFormContentsException(String msg, int position, Category category, Throwable cause)
	{
		super(msg, cause);
		this.position = position;
		this.category = category;
	}

	public IllegalFormContentsException(String msg, int position, Category category)
	{
		super(msg);
		this.position = position;
		this.category = category;
	}
	
	public IllegalFormContentsException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
	
	public IllegalFormContentsException(String msg)
	{
		super(msg);
	}
	
	public int getPosition()
	{
		return position;
	}

	public Category getCategory()
	{
		return category;
	}
}
