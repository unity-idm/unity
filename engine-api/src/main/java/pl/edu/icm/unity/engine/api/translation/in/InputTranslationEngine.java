/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.in;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;

public interface InputTranslationEngine
{

	/**
	 * Entry point.
	 * @param result
	 * @throws EngineException
	 */
	void process(MappingResult result) throws EngineException;

	/**
	 * Merges the information obtained after execution of an input translation profile with a manually specified
	 * entity.
	 * @param result
	 * @param baseEntity
	 * @throws EngineException 
	 */
	void mergeWithExisting(MappingResult result, EntityParam baseEntity) throws EngineException;

	/**
	 * 
	 * @param result
	 * @return true only if no one of mapped identities is present in db.
	 */
	boolean identitiesNotPresentInDb(MappingResult result);

	Entity resolveMappedIdentity(MappedIdentity checked) throws EngineException;

	MappedIdentity getExistingIdentity(MappingResult result);
}