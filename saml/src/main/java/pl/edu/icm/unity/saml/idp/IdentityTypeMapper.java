/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TransientIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;

/**
 * Maps SAML identity to the Unity identity. In the first place the configuration is used, 
 * as a fallback the hardcoded defaults.
 * @author K. Benedyczak
 */
public class IdentityTypeMapper
{
	private Map<String, String> effectiveMappings;
	public static final Map<String, String> DEFAULTS = Map.of(
			SAMLConstants.NFORMAT_PERSISTENT, TargetedPersistentIdentity.ID,
			SAMLConstants.NFORMAT_UNSPEC, TargetedPersistentIdentity.ID,
			SAMLConstants.NFORMAT_DN, X500Identity.ID,
			SAMLConstants.NFORMAT_TRANSIENT, TransientIdentity.ID,
			"unity:persistent", PersistentIdentity.ID,
			"unity:identifier", IdentifierIdentity.ID,
			"unity:userName", UsernameIdentity.ID);
	
	public IdentityTypeMapper(Map<String, String> configuredMappings)
	{
		effectiveMappings = new HashMap<>(DEFAULTS);
		effectiveMappings.putAll(configuredMappings);
	}

	@Deprecated
	//TODO this method should be dropped after refactoring of SAML IDP code to be based on non Properties config.
	public IdentityTypeMapper(SamlProperties config)
	{
		Set<String> keys = config.getStructuredListKeys(SamlProperties.IDENTITY_MAPPING_PFX);
		effectiveMappings = new HashMap<>(keys.size());
		effectiveMappings.putAll(DEFAULTS);
		for (String key: keys)
		{
			String localId = config.getValue(key+SamlProperties.IDENTITY_LOCAL);
			String samlId = config.getValue(key+SamlProperties.IDENTITY_SAML);
			if (localId.trim().equals(""))
				effectiveMappings.remove(samlId);
			else
				effectiveMappings.put(samlId, localId);
		}
	}
	
	/**
	 * @param samlIdentity
	 * @return Unity identity type of the SMAL identity
	 * @throws SAMLRequesterException if the requested type has no mapping 
	 */
	public String mapIdentity(String samlIdentity) throws SAMLRequesterException
	{
		String ret = effectiveMappings.get(samlIdentity);
		if (ret != null)
			return ret;
		throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_INVALID_NAMEID_POLICY,
				samlIdentity + " is not supported by this service.");		
	}
	
	public Set<String> getSupportedIdentityTypes()
	{
		return new HashSet<>(effectiveMappings.keySet());
	}
}
