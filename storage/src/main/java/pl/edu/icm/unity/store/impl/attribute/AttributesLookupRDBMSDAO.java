/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.rdbms.RDBMSDAO;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

@Repository(AttributesLookupRDBMSDAO.DAO_ID)
class AttributesLookupRDBMSDAO implements RDBMSDAO
{
	static final String DAO_ID = "AttributesLookupRDBMSDAO";

	void createWithKey(AttributeLookupBean obj)
	{
		mapper().createWithKey(obj);
	}
	
	public List<AttributeLookupBean> getAll()
	{
		return mapper().getAll();
	}
	
	private AttributesLookupMapper mapper()
	{
		return SQLTransactionTL.getSql().getMapper(AttributesLookupMapper.class);
	}

	List<AttributeLookupBean> getByKeyword(String keyword)
	{
		return mapper().getByKeyword(keyword);
	}

	List<String> getAllKeywords(Long attributeId)
	{
		return mapper().getAllKeywords(attributeId);
	}
}
