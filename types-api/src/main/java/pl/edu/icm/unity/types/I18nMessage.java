/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;


/**
 * Objects are used to store message templates with localized strings.
 * @author K. Benedyczak
 */
public class I18nMessage
{
	private I18nString body;
	private I18nString subject;
	
	public I18nMessage()
	{
	}	
	
	public I18nMessage(I18nString subject, I18nString body)
	{
		this.subject = subject;
		this.body = body;
	}	
	
	public void setBody(I18nString body)
	{
		this.body = body;
	}

	public void setSubject(I18nString subject)
	{
		this.subject = subject;
	}

	public I18nString getBody()
	{
		return body;
	}

	public I18nString getSubject()
	{
		return subject;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		I18nMessage other = (I18nMessage) obj;
		if (body == null)
		{
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (subject == null)
		{
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		return true;
	}
}