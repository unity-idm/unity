/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Handles import/export of group membership info.
 * @author K. Benedyczak
 */
@Component
public class MembershipIE extends AbstractIEBase<GroupMembership>
{
	public static final String GROUP_MEMBERS_OBJECT_TYPE = "groupMembers";
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, MembershipIE.class);

	
	private final MembershipDAO dao;
	
	@Autowired
	public MembershipIE(MembershipDAO dao)
	{
		super(5, GROUP_MEMBERS_OBJECT_TYPE);
		this.dao = dao;
	}

	@Override
	protected List<GroupMembership> getAllToExport()
	{
		return dao.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(GroupMembership exportedObj)
	{
		return Constants.MAPPER.valueToTree(GroupMembershipMapper.map(exportedObj));
	}

	@Override
	protected void createSingle(GroupMembership toCreate)
	{
		dao.create(toCreate);
	}

	@Override
	protected GroupMembership fromJsonSingle(ObjectNode src)
	{
		try
		{
			return GroupMembershipMapper.map(Constants.MAPPER.treeToValue(src, DBGroupMembership.class));

		} catch (JsonProcessingException e)
		{
			log.error("Failed to deserialize group membership object:", e);
		}

		return null;
	}
}








