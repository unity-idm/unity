/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.groups.GroupRDBMSStore;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Serialization to from Json and to from RDBMS beans. 
 * @author K. Benedyczak
 */
@Component
class MembershipJsonSerializer implements RDBMSObjectSerializer<GroupMembership, GroupElementBean>
{
	private final GroupRDBMSStore groupDAO;
	
	MembershipJsonSerializer(GroupRDBMSStore groupDAO)
	{
		this.groupDAO = groupDAO;
	}

	@Override
	public GroupElementBean toDB(GroupMembership object)
	{
		long groupId = groupDAO.getKeyForName(object.getGroup());
		GroupElementBean geb = new GroupElementBean(groupId, object.getEntityId());
		geb.setContents(JsonUtil.serialize2Bytes(object.toJsonBase()));
		return geb;
	}

	@Override
	public GroupMembership fromDB(GroupElementBean bean)
	{
		GroupMembership gm = new GroupMembership(bean.getGroup(), 
				bean.getElementId(), null);
		gm.fromJsonBase(JsonUtil.parse(bean.getContents()));
		return gm;
	}
}
