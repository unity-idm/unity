/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.idp;

import java.util.List;
import java.util.Optional;

import eu.unicore.samly2.exceptions.SAMLRequesterException;
import eu.unicore.util.configuration.PropertiesHelper;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

public interface IdPEngine
{

	/**
	 * Obtains a complete and translated information about entity, authorized to be published.
	 * This variant assumes that the entity should be tried to be imported first and only then
	 * resolved. Therefore this variant is useful for 3rd party queries where queried user may 
	 * at the time of query be missing. 
	 * 
	 * @param entity entity for which the query is performed
	 * @param group the group from which attributes shall be resolved
	 * @param profile output translation profile to be consulted. Can be null -> then default profile is used. 
	 * @param requester identity of requester
	 * @param requesterEntity if present then attributes of this entity in the given group will be added to the output profile context
	 * @param protocol identifier of access protocol
	 * @param protocolSubType sub identifier of protocol (e.g. binding)
	 * @param allowIdentityCreate whether a dynamic id can be established
	 * @return obtained data
	 * @throws EngineException
	 */
	TranslationResult obtainUserInformationWithEarlyImport(IdentityTaV entity, String group, String profile,
			String requester, Optional<EntityInGroup> requesterEntity, 
			String protocol, String protocolSubType,
			boolean allowIdentityCreate, PropertiesHelper importsConfig) throws EngineException;

	/**
	 * Obtains a complete and translated information about entity, authorized to be published.
	 * This variant assumes that the entity should be imported after being resolved, 
	 * i.e. import is enriching already existing entity. Therefore this is useful for
	 * obtaining information about authenticated user.
	 * 
	 * @param entity
	 * @param group
	 * @param profile
	 * @param requester
	 * @param protocol
	 * @param protocolSubType
	 * @param allowIdentityCreate
	 * @param userImports
	 * @return
	 * @throws EngineException
	 */
	TranslationResult obtainUserInformationWithEnrichingImport(EntityParam entity, String group, String profile,
			String requester, Optional<EntityInGroup> requesterEntity, String protocol, String protocolSubType,
			boolean allowIdentityCreate, PropertiesHelper importsConfig) throws EngineException;
	
	/**
	 * Finds selected identity among validIdentities and returns it as IdentityParam. Argument must be given
	 * using comparable identity value.
	 * @param validIdentities
	 * @param selectedIdentity
	 * @return
	 * @throws EngineException
	 * @throws SAMLRequesterException
	 */
	IdentityParam getIdentity(List<IdentityParam> validIdentities, String selectedIdentity) 
			throws EngineException, SAMLRequesterException;
}