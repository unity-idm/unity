/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.impl.groups.GroupRDBMSStore;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Serialization to from Json and to from RDBMS beans.
 * 
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
		try
		{
			geb.setContents(Constants.MAPPER.writeValueAsBytes(GroupMembershipBaseMapper.map(object)));

		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error saving group membership to DB", e);
		}
		return geb;
	}

	@Override
	public GroupMembership fromDB(GroupElementBean bean)
	{
		try
		{
			return GroupMembershipBaseMapper.map(
					Constants.MAPPER.readValue(bean.getContents(), DBGroupMembershipBase.class), bean.getGroup(),
					bean.getElementId());
		} catch (IOException e)
		{
			throw new IllegalStateException("Error parsing group membership from DB", e);
		}

	}
}
