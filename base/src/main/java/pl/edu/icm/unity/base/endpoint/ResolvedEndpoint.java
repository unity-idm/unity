/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.endpoint;

import com.fasterxml.jackson.annotation.JsonIgnore;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;

/**
 * Enriches {@link Endpoint} with data which is derived from that object, but is handy to be stored 
 * in a resolved way: full {@link AuthenticationRealm} and {@link EndpointTypeDescription} are added. 
 * 
 * @author K. Benedyczak
 */
public class ResolvedEndpoint
{
	private Endpoint endpoint;
	private AuthenticationRealm realm;
	private EndpointTypeDescription type;

	public ResolvedEndpoint(Endpoint endpoint, AuthenticationRealm realm,
			EndpointTypeDescription type)
	{
		this.endpoint = endpoint;
		this.realm = realm;
		this.type = type;
	}

	protected ResolvedEndpoint()
	{
	}
	
	public Endpoint getEndpoint()
	{
		return endpoint;
	}

	public AuthenticationRealm getRealm()
	{
		return realm;
	}

	public EndpointTypeDescription getType()
	{
		return type;
	}

	/**
	 * @return the same as {@link #getEndpoint()}.getName()
	 */
	@JsonIgnore
	public String getName()
	{
		return endpoint.getName();
	}
	
	@Override
	public String toString()
	{
		return "EndpointDescription [" + endpoint + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		ResolvedEndpoint other = (ResolvedEndpoint) obj;
		if (endpoint == null)
		{
			if (other.endpoint != null)
				return false;
		} else if (!endpoint.equals(other.endpoint))
			return false;
		if (realm == null)
		{
			if (other.realm != null)
				return false;
		} else if (!realm.equals(other.realm))
			return false;
		if (type == null)
		{
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
