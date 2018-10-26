/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.identity;

import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Support for obtaining displayed name of entities, both individual and bulk.
 * 
 * @author K. Benedyczak
 */
public interface DisplayedNameProvider
{
	String getDisplayedName(EntityParam entity) throws EngineException;
	Map<Long, String> getDisplayedNamesInGroup(String group) throws EngineException;
}
