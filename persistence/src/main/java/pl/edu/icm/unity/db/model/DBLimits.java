/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

import pl.edu.icm.unity.exceptions.IllegalArgumentException;

/**
 * DB has certain size limits on different objects. Those are available from this class.
 * @author K. Benedyczak
 */
public class DBLimits
{
	private int nameLimit;
	private int contentsLimit;

	public int getNameLimit()
	{
		return nameLimit;
	}
	public void setNameLimit(int nameLimit)
	{
		this.nameLimit = nameLimit;
	}
	public int getContentsLimit()
	{
		return contentsLimit;
	}
	public void setContentsLimit(int contentsLimit)
	{
		this.contentsLimit = contentsLimit;
	}
	
	public void checkNameLimit(String ofWhat)
	{
		if (ofWhat.length() > getNameLimit())
			throw new IllegalArgumentException("Name length must not exceed " + 
					getNameLimit() + " characters");

	}
	
	public void checkContentsLimit(byte[] ofWhat)
	{
		if (ofWhat.length > getContentsLimit())
			throw new IllegalArgumentException("Contents must not exceed " + 
					getContentsLimit() + " bytes");

	} 
}
