/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Serialization to from Json and to from RDBMS beans. 
 * @author K. Benedyczak
 */
@Component
public class MembershipJsonSerializer implements RDBMSObjectSerializer<GroupMembership, GroupElementBean>, 
			JsonSerializerForKryo<GroupMembership>
{
	@Autowired
	private GroupDAO groupDAO;
	
	@Override
	public Class<? extends GroupMembership> getClazz()
	{
		return GroupMembership.class;
	}

	@Override
	public GroupMembership fromJson(ObjectNode src)
	{
		return new GroupMembership(src);
	}

	@Override
	public ObjectNode toJson(GroupMembership src)
	{
		return src.toJson();
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
