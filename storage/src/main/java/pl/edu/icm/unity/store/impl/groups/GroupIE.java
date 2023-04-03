/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, GroupIE.class);	

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
		return Constants.MAPPER.valueToTree(GroupMapper.map(exportedObj));
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
		try {
			return GroupMapper.map(Constants.MAPPER.treeToValue(src, DBGroup.class));
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize Group object:", e);
		}
		return null;
	}
}



