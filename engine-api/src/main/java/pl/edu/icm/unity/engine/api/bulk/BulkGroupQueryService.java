/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.bulk;

import java.util.Map;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;

/**
 * Supports fast resolving of data about a group contents in bulk. Usage pattern:
 * first call {@link #getBulkDataForGroup(String)} to obtain a data object. This is the slowest part.
 * Then use it as an argument to other, fast methods converting it to desired contents.
 *  
 * @author K. Benedyczak
 */
public interface BulkGroupQueryService
{
	CompositeGroupContents getBulkDataForGroup(String group) throws EngineException;
	
	Map<Long, Map<String, AttributeExt>> getGroupUsersAttributes(String group, CompositeGroupContents dataO);

	Map<Long, Entity> getGroupEntitiesNoContextWithTargeted(String group, CompositeGroupContents dataO);

	Map<Long, Entity> getGroupEntitiesNoContextWithoutTargeted(String group, CompositeGroupContents dataO);
}
