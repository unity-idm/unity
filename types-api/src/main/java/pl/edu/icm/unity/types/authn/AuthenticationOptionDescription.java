/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import java.io.IOException;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Set of {@link Authenticator}s. The purpose of this set is to allow for
 * multi way authentication, i.e. if the set contains more then one {@link Authenticator}
 * then each of them must be used by a principal to have an overall authentication successful.
 * @author K. Benedyczak
 */
public class AuthenticationOptionDescription
{
	private String primaryAuthenticator;
	private String mandatory2ndAuthenticator;
	
	@JsonCreator
	public AuthenticationOptionDescription(ObjectNode json)
	{
		fromJson(json);
	}
	
	public AuthenticationOptionDescription(String primaryAuthenticator)
	{
		this.primaryAuthenticator = primaryAuthenticator;
	}

	public AuthenticationOptionDescription(String primaryAuthenticator, String mandatory2ndAuthenticator)
	{
		this.primaryAuthenticator = primaryAuthenticator;
		this.mandatory2ndAuthenticator = mandatory2ndAuthenticator;
	}

	
	public boolean contains(String id)
	{
		return primaryAuthenticator.equals(id) || id.equals(mandatory2ndAuthenticator);
	}
	
	public String getPrimaryAuthenticator()
	{
		return primaryAuthenticator;
	}

	public String getMandatory2ndAuthenticator()
	{
		return mandatory2ndAuthenticator;
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode ret = Constants.MAPPER.createObjectNode();
		ret.put("primaryAuthenticator", primaryAuthenticator);
		if (mandatory2ndAuthenticator != null)
			ret.put("mandatory2ndAuthenticator", mandatory2ndAuthenticator);
		return ret;
	}
	
	private void fromJson(ObjectNode json)
	{
		this.primaryAuthenticator = json.get("primaryAuthenticator").asText();
		if (json.has("mandatory2ndAuthenticator"))
			this.mandatory2ndAuthenticator = json.get("mandatory2ndAuthenticator").asText();
	}
	
	public static List<AuthenticationOptionDescription> parseLegacyAuthenticatorSets(JsonNode authnSetsNode) 
			throws IOException
	{
		String rawValue = authnSetsNode.asText();
		ArrayNode root = (ArrayNode) Constants.MAPPER.readTree(rawValue);
		List<AuthenticationOptionDescription> aDescs = new ArrayList<>(root.size());
		for (JsonNode set: root)
		{
			ObjectNode setO = (ObjectNode) set;
			ArrayNode setContents = (ArrayNode) setO.get("authenticators");
			if (setContents.size() == 1)
			{
				aDescs.add(new AuthenticationOptionDescription(setContents.get(0).asText()));
			} else if (setContents.size() > 1)
			{
				aDescs.add(new AuthenticationOptionDescription(setContents.get(0).asText(),
					setContents.get(1).asText()));
			} else
			{
				throw new InternalException("Can't deserialize JSON endpoint state: "
						+ "the authenticators sepcification is invalid");
			}
		}
		return aDescs;
	}
	
	@Override
	public String toString()
	{
		return mandatory2ndAuthenticator == null ? primaryAuthenticator : 
			primaryAuthenticator + "," + mandatory2ndAuthenticator;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((mandatory2ndAuthenticator == null) ? 0
						: mandatory2ndAuthenticator.hashCode());
		result = prime
				* result
				+ ((primaryAuthenticator == null) ? 0 : primaryAuthenticator
						.hashCode());
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
		AuthenticationOptionDescription other = (AuthenticationOptionDescription) obj;
		if (mandatory2ndAuthenticator == null)
		{
			if (other.mandatory2ndAuthenticator != null)
				return false;
		} else if (!mandatory2ndAuthenticator.equals(other.mandatory2ndAuthenticator))
			return false;
		if (primaryAuthenticator == null)
		{
			if (other.primaryAuthenticator != null)
				return false;
		} else if (!primaryAuthenticator.equals(other.primaryAuthenticator))
			return false;
		return true;
	}
}
