/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.dbupdate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

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
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeStatement2.ConflictResolution;
import pl.edu.icm.unity.types.basic.GroupContents;

/**
 * Warning: this test works really only after mvn clean. Otherwise it barely test anything. 
 * @author K. Benedyczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:dbUpdate/to2_1_5/test-components.xml"})
@ActiveProfiles("test")
public class TestDatabaseUpdate2_1_5
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
		
		GroupContents contents = groupsMan.getContents("/A", GroupContents.METADATA);
		AttributeStatement2[] stmts = contents.getGroup().getAttributeStatements();
		assertThat(stmts.length, is(6));

		assertThat(stmts[0].getCondition(), is("true"));
		assertThat(stmts[0].getExtraAttributesGroup(), is(nullValue()));
		assertThat(stmts[0].getFixedAttribute().getName(), is("cn"));

		assertThat(stmts[1].getCondition(), is("eattrs contains 'o'"));
		assertThat(stmts[1].getExtraAttributesGroup(), is("/"));
		assertThat(stmts[1].getDynamicAttributeType().getName(), is("o"));
		assertThat(stmts[1].getDynamicAttributeExpression(), is("eattrs['o']"));
		assertThat(stmts[1].getConflictResolution(), is(ConflictResolution.overwrite));

		assertThat(stmts[2].getCondition(), is("eattrs contains 'height'"));
		assertThat(stmts[2].getExtraAttributesGroup(), is("/A/B"));
		assertThat(stmts[2].getDynamicAttributeType().getName(), is("height"));
		assertThat(stmts[2].getDynamicAttributeExpression(), is("eattrs['height']"));
		assertThat(stmts[2].getConflictResolution(), is(ConflictResolution.skip));

		assertThat(stmts[3].getCondition(), is("eattrs contains 'email'"));
		assertThat(stmts[3].getExtraAttributesGroup(), is("/"));
		assertThat(stmts[3].getFixedAttribute().getName(), is("sys:AuthorizationRole"));

		assertThat(stmts[4].getCondition(), is("eattrs contains 'email'"));
		assertThat(stmts[4].getExtraAttributesGroup(), is("/A/B"));
		assertThat(stmts[4].getFixedAttribute().getName(), is("o"));

		assertThat(stmts[5].getCondition(), is("groups contains '/portal'"));
		assertThat(stmts[5].getExtraAttributesGroup(), is(nullValue()));
		assertThat(stmts[5].getFixedAttribute().getName(), is("postalcode"));
		assertThat(stmts[5].getFixedAttribute().getValues().size(), is(2));
		assertThat(stmts[5].getFixedAttribute().getValues().get(0), is("00-000"));
		assertThat(stmts[5].getFixedAttribute().getValues().get(1), is("01-000"));
	}
}
