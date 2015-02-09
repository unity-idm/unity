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

import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TransientIdentity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Warning: this test works really only after mvn clean. Otherwise it barely test anything. 
 * @author K. Benedyczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:dbUpdate/to2_1_3/test-components.xml"})
@ActiveProfiles("test")
public class TestDatabaseUpdate2_1_3
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
		for (long mem: contents.getMembers())
		{
			EntityParam entityP = new EntityParam(mem);
			Entity entity = idsMan.getEntityNoContext(entityP, "/");
			Identity[] ids = entity.getIdentities();
			for (Identity id: ids)
			{
				if (id.getTypeId().equals(TransientIdentity.ID))
					Assert.fail("Transient not removed");
			}
		}
		idsMan.getEntity(new EntityParam(new IdentityTaV(TargetedPersistentIdentity.ID, 
				"18de4328-786d-43b9-9b56-249f489b79bf", "http://unity/as/sp", "main")), 
				"http://unity/as/sp", false, "/");
	}
}
