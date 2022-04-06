/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

import java.util.List;

public interface AttributeDAO extends BasicCRUDDAO<StoredAttribute>
{
	String DAO_ID = "AttributeDAO";
	String NAME = "attribute";

	void updateAttribute(StoredAttribute a);
	void deleteAttribute(String attribute, long entityId, String group);
	void deleteAttributesInGroup(long entityId, String group);

	/**
	 * Retrieves attributes with given criteria. Any of the arguments can be null, meaning no restriction. 
	 */
	List<StoredAttribute> getAttributes(String attribute, Long entityId, String group);

	/**
	 * Simplified version of {@link #getAttributes(String, Long, String)}. 
	 * Retrieves attributes with given criteria where attribute name and group can be given or not (then 
	 * use null as argument). Entity must be always given. Returned list is unwrapped, as attribute owning 
	 * entity is anyway known.
	 */
	List<AttributeExt> getEntityAttributes(long entity, String attribute, String group);
	
	/**
	 * @return all attributes (in any group) of members of the given group
	 */
	List<StoredAttribute> getAttributesOfGroupMembers(String group);

	/**
	 * @return selected attributes (in any group) of members of the given group,
	 * Attributes can be null or empty, meaning no restriction.
	 */
	List<StoredAttribute> getAttributesOfGroupMembers(List<String> attributes, List<String> groups);

	/**
	 * @return all attributes (in any group) of members of the given group,
	 * Attributes can be null or empty, meaning no restriction.
	 */
	List<StoredAttribute> getAttributesOfGroupMembers(List<String> groups);

	/**
	 * Simplified version of {@link #getEntityAttributes(long, String, String)}. 
	 * Retrieves all attributes of a given entity.
	 */
	default List<AttributeExt> getAllEntityAttributes(long entity)
	{
		return getEntityAttributes(entity, null, null);
	}
	
	/**
	 * Counts attributes without attributes with given types
	 * @param types
	 * @return
	 */
	long getCountWithoutType(List<String> types);
	
	List<Long> getAllIds();

	void linkKeywordToAttribute(String keyword, long attributeId);

	List<StoredAttribute> getAllWithKeyword(String keyword);

	List<String> getAllKeywordsFor(Long attributeId);
}
