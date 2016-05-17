/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.List;
import java.util.stream.Collectors;

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
	@Autowired
	private GroupDAO dao;
	
	@Override
	protected List<Group> getAllToExport()
	{
		//do not export '/'
		return dao.getAll().stream().
				filter(g -> !g.getName().equals("/")).
				collect(Collectors.toList());
	}

	@Override
	protected ObjectNode toJsonSingle(Group exportedObj)
	{
		return exportedObj.toJson();
	}

	@Override
	protected void createSingle(Group toCreate)
	{
		dao.create(toCreate);
	}

	@Override
	protected Group fromJsonSingle(ObjectNode src)
	{
		return new Group(src);
	}
}



