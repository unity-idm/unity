/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.RDBMSDAO;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.types.basic.GroupMembership;


/**
 * RDBMS storage of {@link GroupMembership}
 * @author K. Benedyczak
 */
@Repository(MembershipRDBMSStore.BEAN)
public class MembershipRDBMSStore implements MembershipDAO, RDBMSDAO
{
	public static final String BEAN = DAO_ID + "rdbms";
	private MembershipJsonSerializer jsonSerializer;
	private GroupDAO groupDAO;
	
	@Autowired
	MembershipRDBMSStore(MembershipJsonSerializer jsonSerializer, GroupDAO groupDAO)
	{
		this.jsonSerializer = jsonSerializer;
		this.groupDAO = groupDAO;
	}

	@Override
	public void create(GroupMembership obj)
	{
		MembershipMapper mapper = SQLTransactionTL.getSql().getMapper(MembershipMapper.class);
		GroupElementBean toAdd = jsonSerializer.toDB(obj);
		StorageLimits.checkContentsLimit(toAdd.getContents());
		mapper.create(toAdd);
	}

	@Override
	public void deleteByKey(long entityId, String group)
	{
		MembershipMapper mapper = SQLTransactionTL.getSql().getMapper(MembershipMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		GroupElementBean param = new GroupElementBean(groupId, entityId);
		GroupElementBean byKey = mapper.getByKey(param);
		if (byKey == null)
			throw new IllegalArgumentException("Entity " + entityId + 
					" is not a member of group " + group);
		mapper.deleteByKey(param);
	}

	@Override
	public boolean isMember(long entityId, String group)
	{
		MembershipMapper mapper = SQLTransactionTL.getSql().getMapper(MembershipMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		GroupElementBean param = new GroupElementBean(groupId, entityId);
		return mapper.getByKey(param) != null;
	}

	@Override
	public List<GroupMembership> getEntityMembership(long entityId)
	{
		MembershipMapper mapper = SQLTransactionTL.getSql().getMapper(MembershipMapper.class);
		List<GroupElementBean> entityMembershipB = mapper.getEntityMembership(entityId);
		return deserializeList(entityMembershipB);
	}

	@Override
	public List<GroupMembership> getMembers(String group)
	{
		MembershipMapper mapper = SQLTransactionTL.getSql().getMapper(MembershipMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		List<GroupElementBean> entityMembershipB = mapper.getMembers(groupId);
		return deserializeList(entityMembershipB);
	}

	@Override
	public List<GroupMembership> getAll()
	{
		MembershipMapper mapper = SQLTransactionTL.getSql().getMapper(MembershipMapper.class);
		List<GroupElementBean> entityMembershipB = mapper.getAll();
		return deserializeList(entityMembershipB);
	}
	
	
	private List<GroupMembership> deserializeList(List<GroupElementBean> entityMembershipB)
	{
		List<GroupMembership> ret = new ArrayList<>(entityMembershipB.size());
		for (GroupElementBean geb: entityMembershipB)
			ret.add(jsonSerializer.fromDB(geb));
		return ret;
	}

}
