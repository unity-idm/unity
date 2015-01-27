/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;

/**
 * Warning: this test works really only after mvn clean. Otherwise it barely test anything
 * @author P. Piernik
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:dbUpdate/to2_1_2/test-components.xml"})
@ActiveProfiles("test")
public class TestDatabaseUpdate2_1_2
{
	@Autowired
	private SessionManagement sessionMan;
	
	@Autowired
	protected IdentityResolver identityResolver;
	
	@Autowired
	protected IdentitiesManagement idsMan;

	@Autowired
	protected TokensManagement tokensMan;

	@Test
	public void test() throws Exception
	{
		DBIntegrationTestBase.setupUserContext(sessionMan, identityResolver, "admin", false);
		tokensMan.addToken("TYPE", "1", (new String("xx")).getBytes(StandardCharsets.UTF_8) , 
				new Date(), new Date());	
	}
}
