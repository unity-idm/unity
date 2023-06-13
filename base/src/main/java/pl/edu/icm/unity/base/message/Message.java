/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.message;

import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Holds information about single message
 * 
 * @author P.Piernik
 *
 */
public class Message
{
	private String name;
	private Locale locale;
	private String value;

	@JsonCreator
	public Message()
	{

	}

	public Message(String name, Locale locale, String value)
	{
		this.name = name;
		this.locale = locale;
		this.value = value;
	}

	public Locale getLocale()
	{
		return locale;
	}

	public void setLocale(Locale locale)
	{
		this.locale = locale;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof Message))
			return false;
		Message castOther = (Message) other;

		return Objects.equals(this.name, castOther.name) && Objects.equals(this.locale, castOther.locale)
				&& Objects.equals(this.value, castOther.value);

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, locale, value);
	}
}
