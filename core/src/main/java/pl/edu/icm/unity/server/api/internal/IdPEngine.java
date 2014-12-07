/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api.internal;

import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.server.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * IdP engine is responsible for performing common IdP-related functionality. It resolves the information
 * about the user being queried, applies translation profile on the data and exposes it to the endpoint 
 * requiring the data.
 * 
 * @author K. Benedyczak
 */
@Component
public class IdPEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, IdPEngine.class);
	private AttributesManagement attributesMan;
	private IdentitiesManagement identitiesMan;
	private OutputTranslationEngine translationEngine;
	private TranslationProfileManagement profileManagement;
	
	@Autowired
	public IdPEngine(AttributesManagement attributesMan, IdentitiesManagement identitiesMan,
			OutputTranslationEngine translationEngine,
			@Qualifier("insecure") TranslationProfileManagement profileManagement)
	{
		super();
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;
		this.translationEngine = translationEngine;
		this.profileManagement = profileManagement;
	}



	/**
	 * Obtains a complete and translated information about entity, authorized to be published.
	 * @param entity entity for which the query is performed
	 * @param group the group from which attributes shall be resolved
	 * @param profile output translation profile to be consulted. Can be null -> then default profile is used. 
	 * @param requester identity of requester
	 * @param protocol identifier of access protocol
	 * @param protocolSubType sub identifier of protocol (e.g. binding)
	 * @param allowIdentityCreate whether a dynamic id can be established
	 * @return obtained data
	 * @throws EngineException
	 */
	public TranslationResult obtainUserInformation(EntityParam entity, String group, String profile,
			String requester, String protocol, String protocolSubType, boolean allowIdentityCreate) 
			throws EngineException
	{
		Collection<String> allGroups = identitiesMan.getGroups(entity);
		Collection<AttributeExt<?>> allAttributes = attributesMan.getAttributes(
				entity, group, null);
		Entity fullEntity = identitiesMan.getEntity(entity, requester, allowIdentityCreate, group);
		if (log.isTraceEnabled())
			log.trace("Attributes to be returned (before postprocessing): " + 
					allAttributes + "\nGroups: " + allGroups + "\nIdentities: " + 
					Arrays.toString(fullEntity.getIdentities()));

		OutputTranslationProfile translationProfile = profile == null ? 
				profileManagement.getDefaultOutputProfile() :  
				profileManagement.listOutputProfiles().get(profile);
		if (translationProfile == null)
			throw new ConfigurationException("The translation profile '" + profile + 
					"' configured for the authenticator does not exist");
		TranslationInput input = new TranslationInput(allAttributes, fullEntity, group, allGroups, 
				requester, protocol, protocolSubType);
		TranslationResult result = translationProfile.translate(input);
		translationEngine.process(input, result);
		return result;
	}
}
