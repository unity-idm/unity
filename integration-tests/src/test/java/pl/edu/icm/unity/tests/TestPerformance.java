package pl.edu.icm.unity.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;

public class TestPerformance extends IntegrationTestBase
{	int T = 3; //Group tiers
	int N = 10; // Group in tier
	int NU = 100; // users 

	int ImageAttr = 10; // image attribute
	int StingAttr = 100; // string attribute
	int FloatAttr = 100; // float attributes
	int IntAttr = 100; // int attributes
	
	@Test
	public void testGetEntities() throws EngineException, IOException
	{
		addGroups(N,T);
		addUsers(NU);
		moveUserToGroup(NU, N, T);
		
		//warn-up
		getAllEntities(N/10);
			
		startTimer();
		getAllEntities(N);
		stopTimer(NU, "Get entity");	
	}

	@Test
	public void testGetAttributes() throws EngineException, IOException
	{
		addGroups(N,T);
		addUsers(NU);
		ArrayList<String> enInGroup = moveUserToGroup(NU, N, T);
		ArrayList<Entity> entities = getAllEntities(NU);
		
		addAttributeTypes(N);
		Map<String, AttributeType> attributeTypesAsMap = attrsMan.getAttributeTypesAsMap();
		
		addRandomAttributeToEntities(entities, enInGroup, attributeTypesAsMap, ImageAttr,
				StingAttr, IntAttr, FloatAttr);
		
		//warn-up
		getUsersAttr(N/10, enInGroup, false);
		
		startTimer();
		getUsersAttr(N, enInGroup, false);
		stopTimer(N, "Get attribute for user");		
	}
	
	@Test
	public void testGetAttributesWithStatment() throws EngineException, IOException
	{
		addGroups(N,T);
		addUsers(NU);
		ArrayList<String> enInGroup = moveUserToGroup(NU, N, T);
		ArrayList<Entity> entities = getAllEntities(NU);
		
		addAttributeTypes(N);
		Map<String, AttributeType> attributeTypesAsMap = attrsMan.getAttributeTypesAsMap();
		
		addRandomAttributeToEntities(entities, enInGroup, attributeTypesAsMap, ImageAttr,
				StingAttr, IntAttr, FloatAttr);
		
		ArrayList<GroupContents> con = getGroupContent(new Group("/"));
		
		addAttributeTypeForStatments();
		attributeTypesAsMap = attrsMan.getAttributeTypesAsMap();
		addAttrStatments(con, attributeTypesAsMap, T);
		
		//warn-up
		getUsersAttr(N/10, enInGroup, false);
			
		startTimer();
		getUsersAttr(N, enInGroup, false);
		stopTimer(N, "Get attribute for user with eval attr statment");
		
	}	
}

