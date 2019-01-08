/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl;


/**
 * Provides shared storage limits, ensuring that our dat model can be put in all supported storage engines.
 * @author K. Benedyczak
 */
public class StorageLimits
{
	public static final int NAME_LIMIT = 200;
	public static final int CONTENTS_LIMIT = 60000;
	
	public static void checkNameLimit(String ofWhat) throws IllegalArgumentException
	{
		if (ofWhat != null && ofWhat.length() > NAME_LIMIT)
			throw new SizeLimitExceededException("Name length must not exceed " + 
					NAME_LIMIT + " characters");

	}
	
	public static void checkContentsLimit(byte[] ofWhat) throws IllegalArgumentException
	{
		if (ofWhat != null && ofWhat.length > CONTENTS_LIMIT)
			throw new SizeLimitExceededException("Contents must not exceed " + 
					CONTENTS_LIMIT + " bytes");

	} 

	public static void checkAttributeLimit(byte[] ofWhat, int limit) throws IllegalArgumentException
	{
		if (ofWhat != null && ofWhat.length > limit)
			throw new SizeLimitExceededException("Attribute value(s) is too big, it must not exceed " + 
					limit + " bytes");

	}
	
	public static class SizeLimitExceededException extends IllegalArgumentException
	{
		SizeLimitExceededException(String msg)
		{
			super(msg);
		}
	}
}
