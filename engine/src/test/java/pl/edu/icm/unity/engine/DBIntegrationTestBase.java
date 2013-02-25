/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.ServerManagement;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:META-INF/test-components.xml"})
@ActiveProfiles("test")
public abstract class DBIntegrationTestBase
{
	@Autowired
	protected GroupsManagement groupsMan;
	@Autowired
	protected IdentitiesManagement idsMan;
	@Autowired
	protected AttributesManagement attrsMan;
	@Autowired
	protected ServerManagement serverMan;
	
	
	@Before
	public void clear() throws EngineException
	{
		serverMan.resetDatabase();
	}
	
}
