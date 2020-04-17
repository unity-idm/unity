/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.store.impl.messages;

import java.util.Objects;

import pl.edu.icm.unity.store.rdbms.BaseBean;

public class MessageBean extends BaseBean
{
	private String locale;
	
	public MessageBean()
	{
	}
	
	public MessageBean(String name, String locale, byte[] contents)
	{
		super(name, contents);
		this.setLocale(locale);
	}

	public MessageBean(String name, String locale)
	{
		setName(name);
		this.setLocale(locale);
	}
	
	public String getLocale()
	{
		return locale;
	}

	public void setLocale(String locale)
	{
		this.locale = locale;
	}
	
	
	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof MessageBean))
			return false;
		MessageBean castOther = (MessageBean) other;
		
		return super.equals(other) 
				&& Objects.equals(this.locale, castOther.locale);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), locale);
	}

	
}
