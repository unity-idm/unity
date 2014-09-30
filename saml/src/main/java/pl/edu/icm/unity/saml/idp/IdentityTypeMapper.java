/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TransientIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.exceptions.SAMLRequesterException;

/**
 * Maps SAML identity to the Unity identity. In the first place the configuration is used, 
 * as a fallback the hardcoded defaults.
 * @author K. Benedyczak
 */
public class IdentityTypeMapper
{
	private Map<String, String> configuredMappings;
	private static final Map<String, String> DEFAULTS;
	
	static 
	{
		DEFAULTS = new HashMap<String, String>();
		DEFAULTS.put(SAMLConstants.NFORMAT_PERSISTENT, TargetedPersistentIdentity.ID);
		DEFAULTS.put(SAMLConstants.NFORMAT_UNSPEC, TargetedPersistentIdentity.ID);
		DEFAULTS.put(SAMLConstants.NFORMAT_DN, X500Identity.ID);
		DEFAULTS.put(SAMLConstants.NFORMAT_TRANSIENT, TransientIdentity.ID);
		DEFAULTS.put("unity:persistent", PersistentIdentity.ID);
		DEFAULTS.put("unity:identifier", IdentifierIdentity.ID);
		DEFAULTS.put("unity:userName", UsernameIdentity.ID);
	}

	public IdentityTypeMapper(SAMLIDPProperties config)
	{
		Set<String> keys = config.getStructuredListKeys(SAMLIDPProperties.IDENTITY_MAPPING_PFX);
		configuredMappings = new HashMap<String, String>(keys.size());
		configuredMappings.putAll(DEFAULTS);
		for (String key: keys)
		{
			String localId = config.getValue(key+SAMLIDPProperties.IDENTITY_LOCAL);
			String samlId = config.getValue(key+SAMLIDPProperties.IDENTITY_SAML);
			if (localId.trim().equals(""))
				configuredMappings.remove(samlId);
			else
				configuredMappings.put(samlId, localId);
		}
	}

	/**
	 * @param samlIdentity
	 * @return Unity identity type of the SMAL identity
	 * @throws SAMLRequesterException if the requested type has no mapping 
	 */
	public String mapIdentity(String samlIdentity) throws SAMLRequesterException
	{
		String ret = configuredMappings.get(samlIdentity);
		if (ret != null)
			return ret;
		throw new SAMLRequesterException(SAMLConstants.SubStatus.STATUS2_INVALID_NAMEID_POLICY,
				samlIdentity + " is not supported by this service.");		
	}
	
	public Set<String> getSupportedIdentityTypes()
	{
		return new HashSet<String>(configuredMappings.keySet());
	}
}
