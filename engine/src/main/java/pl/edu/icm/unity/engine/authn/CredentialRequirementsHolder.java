/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.authn.LocalCredentialHandler;
import pl.edu.icm.unity.server.authn.LocalCredentialHandlerFactory;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Internal management of {@link CredentialRequirements}
 * @author K. Benedyczak
 */
public class CredentialRequirementsHolder
{
	private CredentialRequirements requirements;
	private AuthenticatorsRegistry reg;
	private Map<String, LocalCredentialHandler> handlers = new HashMap<String, LocalCredentialHandler>();

	/**
	 * Constructs a new instance from scratch
	 * @param requirements
	 * @param reg
	 * @param credDefs
	 */
	public CredentialRequirementsHolder(CredentialRequirements requirements, AuthenticatorsRegistry reg, 
			Collection<CredentialDefinition> credDefs)
	{
		this.reg = reg;
		this.requirements = requirements;
		initHandlers(requirements.getRequiredCredentials(), credDefs);
	}
	
	/**
	 * Constructs a new instance from serialized DB state
	 * @param reg
	 * @param json
	 * @param credDefs
	 */
	public CredentialRequirementsHolder(AuthenticatorsRegistry reg, byte[] json, 
			Collection<CredentialDefinition> credDefs)
	{
		this.reg = reg;
		this.requirements = CredentialRequirementsSerializer.deserialize(json);
		initHandlers(requirements.getRequiredCredentials(), credDefs);
	}
	
	private void initHandlers(Set<String> configuredCredentials, Collection<CredentialDefinition> credDefs)
	{
		Map<String, CredentialDefinition> crDefsMap = new HashMap<String, CredentialDefinition>();
		for (CredentialDefinition cr: credDefs)
			crDefsMap.put(cr.getName(), cr);
		for (String credDef: configuredCredentials)
			initHandler(reg, crDefsMap.get(credDef));
	}

	private void initHandler(AuthenticatorsRegistry reg, CredentialDefinition def)
	{
		LocalCredentialHandlerFactory fact = reg.getLocalCredentialFactory(def.getTypeId());
		if (fact == null)
			throw new IllegalCredentialException("The credential type " + def.getTypeId() + " is unknown");
		LocalCredentialHandler validator = fact.newInstance();
		validator.setSerializedConfiguration(def.getJsonConfiguration());
		handlers.put(def.getName(), validator);
	}
	
	/**
	 * @return wrapped credential requirements
	 */
	public CredentialRequirements getCredentialRequirements()
	{
		return requirements;
	}
	
	/**
	 * @param credentialName
	 * @return credential handler corresponding to the credential
	 */
	public LocalCredentialHandler getCredentialHandler(String credentialName)
	{
		return handlers.get(credentialName);
	}
	
	/**
	 * Returns true only if all credentials of an entity are valid. As an argument, a map
	 * containing (at least) all credential attributes of the entity must be given.
	 * @param attributes
	 * @return
	 */
	public boolean areAllCredentialsValid(Map<String, Attribute<?>> attributes)
	{
		for (Map.Entry<String, LocalCredentialHandler> entry: handlers.entrySet())
		{
			Attribute<?> currentCredA = attributes.get(SystemAttributeTypes.CREDENTIAL_PREFIX+entry.getKey());
			String currentCred = currentCredA == null ? null : (String)currentCredA.getValues().get(0);
			if (entry.getValue().checkCredentialState(currentCred) != LocalCredentialState.correct)
				return false;
		}
		return true;
	}
}
