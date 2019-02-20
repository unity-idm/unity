/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.store.types;

import java.util.Objects;

import pl.edu.icm.unity.types.NamedObject;

/**
 * Represents an certificate with name stored in db
 * @author P.Piernik
 *
 */
public class StoredCertificate implements NamedObject
{
	private String name;
	private String value;
	
	public StoredCertificate()
	{
		super();
	}
	
	public StoredCertificate(String name, String value)
	{
		super();
		this.name = name;
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

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(name, value);
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
		StoredCertificate other = (StoredCertificate) obj;

		return Objects.equals(name, other.name) && Objects.equals(value, other.value);
	}
}
