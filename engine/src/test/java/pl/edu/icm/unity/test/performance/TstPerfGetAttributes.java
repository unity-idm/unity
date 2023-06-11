package pl.edu.icm.unity.test.performance;
/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.IdentityTaV;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.base.utils.StopWatch;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

/**
 * Test get user attributes 
 * 
 * 
 */
//@ActiveProfiles("test-storage-h2")
public class TstPerfGetAttributes extends PerformanceTestBase2
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE,
			TstPerfGetAttributes.class);
	private final int TEST_REPETITIONS = 3;
	
	private final int GROUP_TIERS = 1; 
	private final int GROUPS_IN_TIER = 10;
	private final int GROUP_ATTR_STATEMENTS = 2;
	
	private final int ENTITIES = 1000;
	private final int ID_PER_ENTITY = 10;

	private final int ATTRIBUTES = 10; 
	
	@Ignore
	@Test
	public void testGetAttributes() throws EngineException, IOException
	{
		//createDBContents();
		
		//warm-up
		getAll();

		log.info("STARTING TEST...");
		//System.in.read();
		for (int i = 0; i < TEST_REPETITIONS; i++)
		{
			timer.startTimer();
			getAll();
			timer.stopTimer(ENTITIES, "Get complete user info");
		}
		timer.calculateResults("Get complete user info");

		log.info("TESTING FINISHED");
	}
	
	protected void createDBContents() throws EngineException, IOException
	{
		log.info("CREATING TEST DB CONTENTS...");
		StopWatch watch = new StopWatch();
		
		addAttributeTypes(ATTRIBUTES, GROUP_ATTR_STATEMENTS);

		watch.printPeriod("Attribute types created: {0}");
		
		addGroups(GROUPS_IN_TIER,GROUP_TIERS, GROUP_ATTR_STATEMENTS);

		watch.printPeriod("Groups created: {0}");

		addUsers(ENTITIES, ID_PER_ENTITY);

		watch.printPeriod("Entities created: {0}");
		
		addEntitiesToGroups(ENTITIES, GROUPS_IN_TIER);

		watch.printPeriod("Entities added to groups: {0}");

		addAttributes(ENTITIES, ATTRIBUTES);

		watch.printPeriod("Attributes assigned: {0}");

		watch.printTotal("Time elapsed: {0}");
		log.info("CREATED TEST DB CONTENTS ");
	}
	
	private void getAll() throws EngineException
	{
		TimeStore timeStore = new TimeStore();
		
		for (int i=0; i<ENTITIES; i++)
		{
			EntityParam entity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user" + i));
			getSingle(entity, timeStore);
		}
		System.out.println(timeStore);
	}
	
	private void getSingle(EntityParam ep, TimeStore individual) throws EngineException
	{
		long start = System.currentTimeMillis();
		Entity entity = idsMan.getEntity(ep);
		long end = System.currentTimeMillis();
		individual.add("getEntity", end-start);
		
		start = System.currentTimeMillis();
		Collection<Group> groups = idsMan.getGroupsForPresentation(ep);
		end = System.currentTimeMillis();
		individual.add("getGroupsForPresentation", end-start);
		
		start = System.currentTimeMillis();
		attrsMan.getAllAttributes(ep, true, (String)null, null, true);
		end = System.currentTimeMillis();
		individual.add("getAttributes", end-start);
		
		assertThat(entity, is(notNullValue()));
		assertThat(groups.size(), is(GROUPS_IN_TIER + 1));
		//direct and dynamic attrs in group tier plus 2 system (cr + cred) + direct in '/'. 
//		assertThat(attributes.size(), is((ATTRIBUTES + GROUP_ATTR_STATEMENTS) * GROUPS_IN_TIER + 
//				2 + ATTRIBUTES));
	}
}
