/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.rdbms.RDBMSDAO;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.EntityInGroup;
import pl.edu.icm.unity.store.types.PendingAttributesCacheEntry;

/**
 * RDBMS storage of the "attributes update pending" flag.
 */
@Repository(AttributesCachePendingRDBMSStore.BEAN)
public class AttributesCachePendingRDBMSStore implements AttributesCachePendingDAO, RDBMSDAO
{
	public static final String BEAN = DAO_ID + "rdbms";
	private static final long INITIAL_VERSION = 1;
	private final GroupDAO groupDAO;

	@Autowired
	AttributesCachePendingRDBMSStore(GroupDAO groupDAO)
	{
		this.groupDAO = groupDAO;
	}

	@Override
	public void markPending(long entityId, String group)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		AttributesCachePendingBean key = new AttributesCachePendingBean(entityId, groupId);
		if (mapper.incrementVersion(key) == 0)
			mapper.create(new AttributesCachePendingBean(entityId, groupId,
					new Timestamp(System.currentTimeMillis()), INITIAL_VERSION));
	}

	@Override
	public void markPendingForGroupMembers(String group)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		mapper.incrementVersionForGroupMembers(groupId);
		AttributesCachePendingBean param = newInsertParam();
		param.setGroupId(groupId);
		mapper.createMissingForGroupMembers(param);
	}

	@Override
	public void markPendingForAll()
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		mapper.incrementVersionOfAll();
		mapper.createMissingForAllMemberships(newInsertParam());
	}

	@Override
	public boolean clearPendingIfVersion(long entityId, String group, long version)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		AttributesCachePendingBean key = new AttributesCachePendingBean(entityId, groupId);
		key.setVersion(version);
		return mapper.deleteByKeyAndVersion(key) > 0;
	}

	@Override
	public boolean isPending(long entityId, String group)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		return mapper.getByKey(new AttributesCachePendingBean(entityId, groupId)) != null;
	}

	@Override
	public List<PendingAttributesCacheEntry> getAll()
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		return mapper.getAll().stream()
				.map(b -> new PendingAttributesCacheEntry(b.getEntityId(), b.getGroup(), b.getVersion()))
				.collect(Collectors.toList());
	}

	@Override
	public List<EntityInGroup> getPendingForGroup(String group)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		return convertList(mapper.getByGroup(groupId));
	}

	private static AttributesCachePendingBean newInsertParam()
	{
		AttributesCachePendingBean param = new AttributesCachePendingBean();
		param.setCreated(new Timestamp(System.currentTimeMillis()));
		param.setVersion(INITIAL_VERSION);
		return param;
	}

	private List<EntityInGroup> convertList(List<AttributesCachePendingBean> beans)
	{
		return beans.stream()
				.map(b -> new EntityInGroup(b.getEntityId(), b.getGroup()))
				.collect(Collectors.toList());
	}
}
