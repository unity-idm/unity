/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Comment made by a system entity, i.e. having an author.
 * @author K. Benedyczak
 */
public class AdminComment extends Comment
{
	private long authorEntityId;

	public long getAuthorEntityId()
	{
		return authorEntityId;
	}

	public void setAuthorEntityId(long authorEntityId)
	{
		this.authorEntityId = authorEntityId;
	}
}
