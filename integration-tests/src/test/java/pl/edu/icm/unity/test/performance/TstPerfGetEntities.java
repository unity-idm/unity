/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.performance;

import java.io.IOException;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Test standard get entities 
 * 
 * @author P.Piernik
 * 
 */
public class TstPerfGetEntities extends PerformanceTestBase
{	
	//@Test
	public void testGetEntities() throws EngineException, IOException
	{
		addGroups(GROUP_IN_TIER,GROUP_TIERS);
		addUsers(USERS);
		moveUserToGroup(USERS, GROUP_IN_TIER, GROUP_TIERS);
		
		//warn-up
		getAllEntities(USERS/10);
			
		
		for (int i = 0; i < TEST_REPETITIONS; i++)
		{
			timer.startTimer();
			getAllEntities(GROUP_IN_TIER);
			timer.stopTimer(USERS, "Get entity");
		}
		timer.calculateResults("Get entity");
	}
}
