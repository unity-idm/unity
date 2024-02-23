/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.group;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.base.tx.Transactional;

/**
 * Invoked periodically to scan group attribute statements, and remove the invalid ones.
 * This is done only to tidy the database - the attribute resolve process is ignoring outdated statements.
 * <p>
 * WARNING if interface is implemented fix transactions
 * @author K. Benedyczak
 */
@Component
public class AttributeStatementsCleaner
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE,
			AttributeStatementsCleaner.class);
	private GroupHelper groupHelper;
	private GroupDAO groupDAO;
	
	@Autowired
	public AttributeStatementsCleaner(GroupHelper groupHelper, GroupDAO groupDAO)
	{
		this.groupHelper = groupHelper;
		this.groupDAO = groupDAO;
	}

	@Transactional
	public void updateGroups() throws IllegalGroupValueException, IllegalAttributeTypeException
	{
		updateAllGroups();
	}
	
	/**
	 * Loads all groups, deserializes their contents what removes the outdated entries and update it
	 * if something was changed.
	 * @throws IllegalAttributeTypeException 
	 * @throws IllegalGroupValueException 
	 */
	private void updateAllGroups()
	{
		for (Group gb: groupDAO.getAll())
		{
			updateGroup(gb);
		}
	}
	
	private void updateGroup(Group group)
	{
		AttributeStatement[] attributeStatements = group.getAttributeStatements();
		List<AttributeStatement> updated = new ArrayList<>(attributeStatements.length);
		for (AttributeStatement as: attributeStatements)
		{
			try
			{
				groupHelper.validateGroupStatement(group.getName(), as);
				updated.add(as);
			} catch (Exception e)
			{
				log.info("Group " + group + " contains an outdated attribute statement, "
						+ "it will be removed: " + as);
			}
		}
		if (updated.size() != attributeStatements.length)
		{
			group.setAttributeStatements(updated.toArray(new AttributeStatement[updated.size()]));
			groupDAO.update(group);
		}
	}
}
