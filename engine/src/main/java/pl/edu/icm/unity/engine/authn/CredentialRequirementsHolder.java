/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.authn.LocalCredentialHandler;
import pl.edu.icm.unity.server.authn.LocalCredentialHandlerFactory;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.JsonSerializable;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Internal management of {@link CredentialRequirements}
 * @author K. Benedyczak
 */
public class CredentialRequirementsHolder implements JsonSerializable
{
	private CredentialRequirements requirements;
	private AuthenticatorsRegistry reg;
	private Map<String, LocalCredentialHandler> verificators = new HashMap<String, LocalCredentialHandler>();

	public CredentialRequirementsHolder(String name, String description,
			Set<CredentialDefinition> configuredCredentials, AuthenticatorsRegistry reg)
	{
		this(reg);
		checkRequirements(configuredCredentials);
		requirements = new CredentialRequirements(name, description, configuredCredentials);
	}
	
	public CredentialRequirementsHolder(AuthenticatorsRegistry reg)
	{
		this.reg = reg;
	}
	
	private void checkCredentialDefinition(CredentialDefinition def, AuthenticatorsRegistry reg)
	{
		LocalCredentialHandlerFactory fact = reg.getLocalCredentialFactory(def.getTypeId());
		if (fact == null)
			throw new IllegalCredentialException("The credential type " + def.getTypeId() + " is unknown");
		LocalCredentialHandler validator = fact.newInstance();
		validator.setSerializedConfiguration(def.getJsonConfiguration());
		verificators.put(def.getName(), validator);
	}
	
	public CredentialRequirements getCredentialRequirements()
	{
		return requirements;
	}
	
	public LocalCredentialHandler getVerificator(String credentialName)
	{
		return verificators.get(credentialName);
	}
	
	/**
	 * Returns true only if all credentials of an entity are valid. As an argument, a map
	 * containing (at least) all credential attributes of the entity must be given.
	 * @param attributes
	 * @return
	 */
	public boolean areAllCredentialsValid(Map<String, Attribute<?>> attributes)
	{
		for (LocalCredentialHandler entry: verificators.values())
		{
			Attribute<?> currentCredA = attributes.get(SystemAttributeTypes.CREDENTIAL_PREFIX+entry.getName());
			String currentCred = currentCredA == null ? null : (String)currentCredA.getValues().get(0);
			if (entry.checkCredentialState(currentCred) != LocalCredentialState.correct)
				return false;
		}
		return true;
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(requirements);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize credential requiremets to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		try
		{
			requirements = Constants.MAPPER.readValue(json, CredentialRequirements.class);
		} catch (Exception e)
		{
			throw new RuntimeEngineException("Can't deserialize credential requiremets from JSON", e);
		}
		checkRequirements(requirements.getRequiredCredentials());
	}
	
	private void checkRequirements(Set<CredentialDefinition> configuredCredentials)
	{
		for (CredentialDefinition credDef: configuredCredentials)
			checkCredentialDefinition(credDef, reg);
	}
}
