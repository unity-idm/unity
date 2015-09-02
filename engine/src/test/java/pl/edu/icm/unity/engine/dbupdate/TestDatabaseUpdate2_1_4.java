/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.dbupdate;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Warning: this test works really only after mvn clean. Otherwise it barely test anything. 
 * @author K. Benedyczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:dbUpdate/to2_1_4/test-components.xml"})
@ActiveProfiles("test")
public class TestDatabaseUpdate2_1_4
{
	@Autowired
	private SessionManagement sessionMan;
	
	@Autowired
	protected IdentityResolver identityResolver;
	
	@Autowired
	protected IdentitiesManagement idsMan;

	@Autowired
	protected GroupsManagement groupsMan;

	@Test
	public void test() throws Exception
	{
		DBIntegrationTestBase.setupUserContext(sessionMan, identityResolver, "admin", false);
		
		GroupContents contents = groupsMan.getContents("/", GroupContents.MEMBERS);
		for (GroupMembership mem: contents.getMembers())
		{
			Assert.assertNull(mem.getRemoteIdp());
			Assert.assertNull(mem.getTranslationProfile());
		}
	}
}
