/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * RDBMS storage of the materialized attributes cache.
 */
@Repository(AttributesCacheRDBMSStore.BEAN)
public class AttributesCacheRDBMSStore extends GenericRDBMSCRUD<StoredAttribute, AttributesCacheBean>
		implements AttributesCacheDAO
{
	public static final String BEAN = DAO_ID + "rdbms";
	private final GroupDAO groupDAO;

	@Autowired
	AttributesCacheRDBMSStore(AttributesCacheRDBMSSerializer dbSerializer, GroupDAO groupDAO)
	{
		super(AttributesCacheMapper.class, dbSerializer, NAME);
		this.groupDAO = groupDAO;
	}

	@Override
	public void deleteCacheInGroup(long entityId, String group)
	{
		AttributesCacheMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCacheMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		AttributesCacheBean param = new AttributesCacheBean();
		param.setEntityId(entityId);
		param.setGroupId(groupId);
		mapper.deleteCacheInGroup(param);
	}

	@Override
	public List<StoredAttribute> getEntityAttributes(long entityId, String group)
	{
		AttributesCacheMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCacheMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		AttributesCacheBean param = new AttributesCacheBean();
		param.setEntityId(entityId);
		param.setGroupId(groupId);
		return convertList(mapper.getEntityGroupAttributes(param));
	}

	@Override
	public List<StoredAttribute> getGroupAttributes(String group)
	{
		AttributesCacheMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCacheMapper.class);
		return convertList(mapper.getGroupAttributes(group));
	}
}
