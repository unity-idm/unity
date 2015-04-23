/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.internal.SessionManagementImpl;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Warning: this test works really only after mvn clean. Otherwise it barely
 * tests anything
 * 
 * @author P. Piernik
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:META-INF/components.xml","classpath:dbUpdate/to2_1_2/test-components.xml" })
@ActiveProfiles("test")
public class TestDatabaseUpdate2_1_2
{
	@Autowired
	private SessionManagement sessionMan;
	@Autowired
	private IdentityResolver identityResolver;
	@Autowired
	private TokensManagement tokensMan;
	@Autowired
	protected IdentitiesManagement idsMan;
	@Autowired
	protected GroupsManagement groupsMan;

	@Test
	public void test() throws Exception
	{
		Assert.assertEquals(2,
				tokensMan.getAllTokens(SessionManagementImpl.SESSION_TOKEN_TYPE)
						.size());
		DBIntegrationTestBase.setupUserContext(sessionMan, identityResolver, "admin", true);
		GroupContents contents = groupsMan.getContents("/", GroupContents.MEMBERS);
		for (long mem : contents.getMembers())
		{
			EntityParam entityP = new EntityParam(mem);
			Entity entity = idsMan.getEntityNoContext(entityP, "/");
			Identity[] ids = entity.getIdentities();
			Assert.assertTrue(ids.length > 1);
			for (Identity id : ids)
			{
				Assert.assertNull(id.getConfirmationInfo());
			}
		}
		Assert.assertEquals(2,
				tokensMan.getAllTokens(SessionManagementImpl.SESSION_TOKEN_TYPE)
						.size());
		Assert.assertFalse("",
				tokensMan.getAllTokens(SessionManagementImpl.SESSION_TOKEN_TYPE)
						.get(0).getContentsString().isEmpty());

	}
}
