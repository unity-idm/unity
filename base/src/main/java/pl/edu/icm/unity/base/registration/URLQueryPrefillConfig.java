/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import pl.edu.icm.unity.base.registration.invite.PrefilledEntryMode;

/**
 * Describes whether&how a parameter can be pre-filled with information obtained from a URL query string
 * 
 * @author K. Benedyczak
 */
public class URLQueryPrefillConfig
{
	public final String paramName;
	public final PrefilledEntryMode mode;
	
	@JsonCreator
	public URLQueryPrefillConfig(@JsonProperty("paramName") String paramName, 
			@JsonProperty("mode") PrefilledEntryMode mode)
	{
		this.paramName = paramName;
		this.mode = mode;
	}

	@Override
	public String toString()
	{
		return "URLQueryPrefillable [paramName=" + paramName + ", mode=" + mode + "]";
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(mode, paramName);
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
		URLQueryPrefillConfig other = (URLQueryPrefillConfig) obj;
		return mode == other.mode && Objects.equals(paramName, other.paramName);
	}
}
