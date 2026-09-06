/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;

/**
 * Access to the AttributesCache.xml operations.
 */
public interface AttributesCacheMapper extends BasicCRUDMapper<AttributesCacheBean>
{
	void deleteCacheInGroup(AttributesCacheBean param);

	List<AttributesCacheBean> getEntityGroupAttributes(AttributesCacheBean param);

	List<AttributesCacheBean> getGroupAttributes(String group);
}
