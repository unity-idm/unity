/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
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
	private LocalCredentialsRegistry reg;
	private Map<String, LocalCredentialVerificator> handlers = new HashMap<>();

	/**
	 * Constructs a new instance from {@link CredentialRequirements}
	 * @param reg
	 * @param json
	 * @param credDefs
	 * @throws IllegalCredentialException 
	 */
	public CredentialRequirementsHolder(LocalCredentialsRegistry reg, CredentialRequirements requirements, 
			Collection<CredentialDefinition> credDefs) throws IllegalCredentialException
	{
		this.reg = reg;
		this.requirements = requirements;
		initHandlers(requirements.getRequiredCredentials(), credDefs);
	}
	
	private void initHandlers(Set<String> configuredCredentials, Collection<CredentialDefinition> credDefs) 
			throws IllegalCredentialException
	{
		Map<String, CredentialDefinition> crDefsMap = new HashMap<>();
		for (CredentialDefinition cr: credDefs)
			crDefsMap.put(cr.getName(), cr);
		for (String credDef: configuredCredentials)
			initHandler(reg, crDefsMap.get(credDef));
	}

	public static void checkCredentials(CredentialRequirements requirements, 
			Map<String, CredentialDefinition> crDefsMap,
			LocalCredentialsRegistry reg) throws IllegalCredentialException
	{
		Set<String> configuredCredentials = requirements.getRequiredCredentials();
		for (String credDef: configuredCredentials)
		{
			CredentialDefinition def = crDefsMap.get(credDef);
			LocalCredentialVerificatorFactory fact = reg.getLocalCredentialFactory(def.getTypeId());
			if (fact == null)
				throw new IllegalCredentialException("The credential type " + def.getTypeId() + 
						" is unknown");
		}
	}
	
	private void initHandler(LocalCredentialsRegistry reg, CredentialDefinition def) 
			throws IllegalCredentialException
	{
		handlers.put(def.getName(), reg.createLocalCredentialVerificator(def));
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
	public LocalCredentialVerificator getCredentialHandler(String credentialName)
	{
		return handlers.get(credentialName);
	}
	
	/**
	 * Returns true only if all credentials of an entity are valid. As an argument, a map
	 * containing (at least) all credential attributes of the entity must be given.
	 * @param attributes
	 * @return
	 */
	public boolean areAllCredentialsValid(Map<String, ? extends Attribute> attributes)
	{
		for (Map.Entry<String, LocalCredentialVerificator> entry: handlers.entrySet())
		{
			Attribute currentCredA = attributes.get(CredentialAttributeTypeProvider.CREDENTIAL_PREFIX 
					+ entry.getKey());
			String currentCred = currentCredA == null ? null : (String)currentCredA.getValues().get(0);
			if (entry.getValue().checkCredentialState(currentCred).getState() != 
					LocalCredentialState.correct)
				return false;
		}
		return true;
	}
}
