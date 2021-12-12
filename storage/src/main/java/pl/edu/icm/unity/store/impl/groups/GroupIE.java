/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Handles import/export of groups.
 * @author K. Benedyczak
 */
@Component
public class GroupIE extends AbstractIEBase<Group>
{
	public static final String GROUPS_OBJECT_TYPE = "groups";

	private final GroupDAO dao;
	
	@Autowired
	public GroupIE(GroupDAO dao)
	{
		super(4, GROUPS_OBJECT_TYPE);
		this.dao = dao;
	}

	@Override
	protected List<Group> getAllToExport()
	{
		return dao.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(Group exportedObj)
	{
		return exportedObj.toJson();
	}

	@Override
	protected void createSingle(Group toCreate)
	{
		if (!toCreate.isTopLevel())
		{
			dao.create(toCreate);
		} else
		{
			dao.update(toCreate);
		}
	}

	@Override
	protected Group fromJsonSingle(ObjectNode src)
	{
		return new Group(src);
	}
}



