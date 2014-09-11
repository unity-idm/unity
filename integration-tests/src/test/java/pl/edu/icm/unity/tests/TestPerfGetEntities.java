/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.tests;

import java.io.IOException;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Test standard get entities 
 * 
 * @author P.Piernik
 * 
 */
public class TestPerfGetEntities extends IntegrationTestBase
{	
	@Test
	public void testGetEntities() throws EngineException, IOException
	{
		addGroups(GROUP_IN_TIER,GROUP_TIERS);
		addUsers(USERS);
		moveUserToGroup(USERS, GROUP_IN_TIER, GROUP_TIERS);
		
		//warn-up
		getAllEntities(USERS/10);
			
		
		for (int i = 0; i < 10; i++)
		{
			timer.startTimer();
			getAllEntities(GROUP_IN_TIER);
			timer.stopTimer(USERS, "Get entity");
		}
		timer.calculateResults("Get entity");
	}
}
