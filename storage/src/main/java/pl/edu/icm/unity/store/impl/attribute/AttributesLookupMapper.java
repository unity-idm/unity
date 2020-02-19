/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.BasicCRUDMapper;

/**
 * Access to AttributesLookup.xml operations.
 */
public interface AttributesLookupMapper extends BasicCRUDMapper<AttributeLookupBean>
{
	List<AttributeLookupBean> getByKeyword(String keyword);

	List<String> getAllKeywords(long attributeId);
}
