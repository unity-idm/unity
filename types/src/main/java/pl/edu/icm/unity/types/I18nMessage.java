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
}