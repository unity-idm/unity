/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.idp;

import java.util.List;

import eu.unicore.samly2.exceptions.SAMLRequesterException;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;

public interface IdPEngine
{

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
	TranslationResult obtainUserInformation(EntityParam entity, String group, String profile,
			String requester, String protocol, String protocolSubType,
			boolean allowIdentityCreate, boolean triggerImport) throws EngineException;
	
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