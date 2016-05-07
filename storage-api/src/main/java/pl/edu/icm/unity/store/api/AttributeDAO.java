/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.List;

import pl.edu.icm.unity.types.basic2.AttributeExt2;

public interface AttributeDAO extends BasicCRUDDAO<StoredAttribute>
{
	String DAO_ID = "AttributeDAO";
	String NAME = "attribute";

	void updateAttribute(StoredAttribute a);
	void deleteAttribute(String attribute, long entityId, String group);
	void deleteAttributesInGroup(long entityId, String group);

	/**
	 * Group and attribute arguments may be null, what means that there is no filtering by that coordinate.
	 * @param attribute
	 * @param entityId
	 * @param group
	 * @return
	 */
	List<AttributeExt2> getAttributes(String attribute, long entityId, String group);

}
