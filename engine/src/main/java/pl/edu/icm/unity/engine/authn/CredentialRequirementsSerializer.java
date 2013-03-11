/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

/**
 * Internal management of {@link CredentialRequirements}
 * @author K. Benedyczak
 */
public class CredentialRequirementsSerializer
{
	public static byte[] serialize(CredentialRequirements requirements)
	{
		try
		{
			return Constants.MAPPER.writeValueAsBytes(requirements);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize credential requiremets to JSON", e);
		}
	}

	public static CredentialRequirements deserialize(byte[] json)
	{
		try
		{
			return Constants.MAPPER.readValue(json, CredentialRequirements.class);
		} catch (Exception e)
		{
			throw new RuntimeEngineException("Can't deserialize credential requiremets from JSON", e);
		}
	}
}
