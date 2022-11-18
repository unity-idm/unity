/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;
import pl.edu.icm.unity.stdext.identity.*;

import java.util.*;

/**
 * Maps SAML identity to the Unity identity. In the first place the configuration is used, 
 * as a fallback the hardcoded defaults.
 * @author K. Benedyczak
 */
public class IdentityTypeMapper
{
	private final Map<String, String> effectiveSamlToUnityIdMappings;
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
		effectiveSamlToUnityIdMappings = new HashMap<>(DEFAULTS);
		effectiveSamlToUnityIdMappings.putAll(configuredMappings);
	}
	
	/**
	 * @param samlIdentity
	 * @return Unity identity type of the SMAL identity
	 * @throws SAMLRequesterException if the requested type has no mapping 
	 */
	public String mapIdentity(String samlIdentity) throws SAMLRequesterException
	{
		String ret = effectiveSamlToUnityIdMappings.get(samlIdentity);
		if (ret != null)
			return ret;
		throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_INVALID_NAMEID_POLICY,
				samlIdentity + " is not supported by this service.");		
	}
	
	public Set<String> getSupportedIdentityTypes()
	{
		return new HashSet<>(effectiveSamlToUnityIdMappings.keySet());
	}
}
