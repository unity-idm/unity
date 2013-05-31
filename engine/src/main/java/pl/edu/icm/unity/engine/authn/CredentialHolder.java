/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.types.JsonSerializable;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Internal management of {@link CredentialDefinition}
 * @author K. Benedyczak
 */
public class CredentialHolder implements JsonSerializable
{
	private CredentialDefinition credential;
	private AuthenticatorsRegistry reg;
	private LocalCredentialVerificator handler;

	public CredentialHolder(CredentialDefinition credDef, AuthenticatorsRegistry reg) 
			throws IllegalCredentialException
	{
		this(reg);
		checkCredentialDefinition(credDef, reg);
		credential = credDef;
	}
	
	public CredentialHolder(AuthenticatorsRegistry reg)
	{
		this.reg = reg;
	}
	
	private void checkCredentialDefinition(CredentialDefinition def, AuthenticatorsRegistry reg) 
			throws IllegalCredentialException
	{
		LocalCredentialVerificatorFactory fact = reg.getLocalCredentialFactory(def.getTypeId());
		if (fact == null)
			throw new IllegalCredentialException("The credential type " + def.getTypeId() + " is unknown");
		LocalCredentialVerificator handler = fact.newInstance();
		handler.setSerializedConfiguration(def.getJsonConfiguration());
		this.handler = handler;
	}
	
	public CredentialDefinition getCredentialDefinition()
	{
		return credential;
	}
	
	public LocalCredentialVerificator getHandler()
	{
		return handler;
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(credential);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize credential to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		try
		{
			credential = Constants.MAPPER.readValue(json, CredentialDefinition.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize credential from JSON", e);
		}
		try
		{
			checkCredentialDefinition(credential, reg);
		} catch (IllegalCredentialException e)
		{
			throw new InternalException("The credential definition loaded from " +
					"DB uses an unsupported implementation", e);
		}
	}
}
