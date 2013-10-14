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

	
	public AdminComment(String contents, long authorEntityId)
	{
		super(contents);
		this.authorEntityId = authorEntityId;
	}

	public AdminComment()
	{
		super();
	}

	public long getAuthorEntityId()
	{
		return authorEntityId;
	}

	public void setAuthorEntityId(long authorEntityId)
	{
		this.authorEntityId = authorEntityId;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (authorEntityId ^ (authorEntityId >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AdminComment other = (AdminComment) obj;
		if (authorEntityId != other.authorEntityId)
			return false;
		return true;
	}
}
