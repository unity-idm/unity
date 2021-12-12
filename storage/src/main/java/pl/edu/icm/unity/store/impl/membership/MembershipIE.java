/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

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
		return exportedObj.toJson();
	}

	@Override
	protected void createSingle(GroupMembership toCreate)
	{
		dao.create(toCreate);
	}

	@Override
	protected GroupMembership fromJsonSingle(ObjectNode src)
	{
		return new GroupMembership(src);
	}
}








