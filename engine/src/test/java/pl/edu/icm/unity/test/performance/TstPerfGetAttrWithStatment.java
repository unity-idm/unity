/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.identity.Entity;

/**
 * Test get user attributes with attributes statements processing
 * 
 * @author P.Piernik
 * 
 */
public class TstPerfGetAttrWithStatment extends PerformanceTestBase
{
	@Ignore
	@Test
	public void testGetAttributesWithStatment() throws EngineException, IOException
	{
		addGroups(GROUP_IN_TIER, GROUP_TIERS);
		addUsers(USERS);
		ArrayList<String> enInGroup = moveUserToGroup(USERS, GROUP_IN_TIER, GROUP_TIERS);
		ArrayList<Entity> entities = getAllEntities(USERS);
		
		addAttributeTypes(GROUP_IN_TIER);
		Map<String, AttributeType> attributeTypesAsMap = attrTypesMan.getAttributeTypesAsMap();
		
		addRandomAttributeToEntities(entities, enInGroup, attributeTypesAsMap, IMAGE_ATTRIBUTES,
				STRING_ATTRIBUTES, INT_ATTRIBUTES, FLOAT_ATTRIBUTES);
		
		ArrayList<GroupContents> con = getGroupContent(new Group("/"));
		
		
		addAttributeTypeForStatments();
		attributeTypesAsMap = attrTypesMan.getAttributeTypesAsMap();
		addAttrStatments(con, attributeTypesAsMap, GROUP_TIERS);
		
		//warm-up
		getUsersAttr(GROUP_IN_TIER/10, enInGroup, false);
		
		for (int i = 0; i < TEST_REPETITIONS; i++)
		{
			timer.startTimer();
			getUsersAttr(GROUP_IN_TIER, enInGroup, false);
			timer.stopTimer(GROUP_IN_TIER, "Get attribute for user with eval attr statment");
		}
		timer.calculateResults("Get attribute for user with eval attr statment");
		
	}	
}
