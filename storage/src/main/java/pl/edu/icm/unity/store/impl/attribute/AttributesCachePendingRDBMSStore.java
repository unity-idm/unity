/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.rdbms.RDBMSDAO;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.EntityInGroup;

/**
 * RDBMS storage of the "attributes update pending" flag.
 */
@Repository(AttributesCachePendingRDBMSStore.BEAN)
public class AttributesCachePendingRDBMSStore implements AttributesCachePendingDAO, RDBMSDAO
{
	public static final String BEAN = DAO_ID + "rdbms";
	private final GroupDAO groupDAO;
	private final MembershipDAO membershipDAO;

	@Autowired
	AttributesCachePendingRDBMSStore(GroupDAO groupDAO, MembershipDAO membershipDAO)
	{
		this.groupDAO = groupDAO;
		this.membershipDAO = membershipDAO;
	}

	@Override
	public void markPending(long entityId, String group)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		AttributesCachePendingBean key = new AttributesCachePendingBean(entityId, groupId);
		if (mapper.getByKey(key) == null)
		{
			key.setCreated(new Timestamp(System.currentTimeMillis()));
			mapper.create(key);
		}
	}

	@Override
	public void markPendingForGroup(String group)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		Set<Long> alreadyPending = mapper.getByGroup(groupId).stream()
				.map(AttributesCachePendingBean::getEntityId)
				.collect(Collectors.toSet());
		List<GroupMembership> members = membershipDAO.getMembers(group);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		List<AttributesCachePendingBean> toInsert = new ArrayList<>();
		for (GroupMembership member : members)
			if (!alreadyPending.contains(member.getEntityId()))
				toInsert.add(new AttributesCachePendingBean(member.getEntityId(), groupId, now));
		if (!toInsert.isEmpty())
			mapper.createList(toInsert);
	}

	@Override
	public void clearPending(long entityId, String group)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		mapper.deleteByKey(new AttributesCachePendingBean(entityId, groupId));
	}

	@Override
	public boolean isPending(long entityId, String group)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		return mapper.getByKey(new AttributesCachePendingBean(entityId, groupId)) != null;
	}

	@Override
	public List<EntityInGroup> getAll()
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		return convertList(mapper.getAll());
	}

	@Override
	public List<EntityInGroup> getPendingForGroup(String group)
	{
		AttributesCachePendingMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesCachePendingMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		return convertList(mapper.getByGroup(groupId));
	}

	private List<EntityInGroup> convertList(List<AttributesCachePendingBean> beans)
	{
		return beans.stream()
				.map(b -> new EntityInGroup(b.getEntityId(), b.getGroup()))
				.collect(Collectors.toList());
	}
}
