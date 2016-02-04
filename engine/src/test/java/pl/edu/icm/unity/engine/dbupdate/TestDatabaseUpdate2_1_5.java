/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.dbupdate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.server.translation.form.action.AddAttributeClassActionFactory;
import pl.edu.icm.unity.server.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.server.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.server.translation.form.action.RedirectActionFactory;
import pl.edu.icm.unity.server.translation.form.action.SetEntityStateActionFactory;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeStatement2.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
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
	
	@Autowired
	protected RegistrationsManagement regMan;
	
	@BeforeClass
	public static void copyDB() throws IOException
	{
		DBUpdateUtil.installTestDB("2_1_5");
	}
	
	@Test
	public void test() throws Exception
	{
		DBIntegrationTestBase.setupUserContext(sessionMan, identityResolver, "admin", false);
		
		checkStatements();
		checkForm();
	}
	
	private void checkForm() throws Exception
	{
		List<RegistrationForm> forms = regMan.getForms();
		assertThat(forms.size(), is(1));
		
		RegistrationForm form = forms.get(0);
		
		assertThat(form.getDefaultCredentialRequirement(), is("certificate"));
		
		TranslationProfile profile = form.getTranslationProfile();
		List<? extends TranslationRule> rules = profile.getRules();
		
		assertThat(rules.get(0).getCondition(), is("true"));
		assertThat(rules.get(0).getAction().getName(), 
				is(SetEntityStateActionFactory.NAME));
		assertThat(rules.get(0).getAction().getParameters()[0], 
				is(EntityState.authenticationDisabled.toString()));

		assertThat(rules.get(1).getCondition(), is("true"));
		assertThat(rules.get(1).getAction().getName(), 
				is(AutoProcessActionFactory.NAME));
		assertThat(rules.get(1).getAction().getParameters()[0], 
				is(AutomaticRequestAction.accept.toString()));

		assertThat(rules.get(2).getCondition(), is("true"));
		assertThat(rules.get(2).getAction().getName(), 
				is(RedirectActionFactory.NAME));
		assertThat(rules.get(2).getAction().getParameters()[0], 
				is("'http://example.com/foo?haha=true'"));

		assertThat(rules.get(3).getCondition(), is("true"));
		assertThat(rules.get(3).getAction().getName(), 
				is(AddAttributeActionFactory.NAME));
		assertThat(rules.get(3).getAction().getParameters()[0], 
				is("o"));
		assertThat(rules.get(3).getAction().getParameters()[1], 
				is("/A"));
		assertThat(rules.get(3).getAction().getParameters()[2], 
				is("['icm', 'uw']"));
		assertThat(rules.get(3).getAction().getParameters()[3], 
				is(AttributeVisibility.full.toString()));

		assertThat(rules.get(4).getCondition(), is("true"));
		assertThat(rules.get(4).getAction().getName(), 
				is(AddAttributeActionFactory.NAME));
		assertThat(rules.get(4).getAction().getParameters()[0], 
				is("postalcode"));
		assertThat(rules.get(4).getAction().getParameters()[1], 
				is("/"));
		assertThat(rules.get(4).getAction().getParameters()[2], 
				is("['00-000']"));
		assertThat(rules.get(4).getAction().getParameters()[3], 
				is(AttributeVisibility.local.toString()));
		
		assertThat(rules.get(5).getCondition(), is("true"));
		assertThat(rules.get(5).getAction().getName(), 
				is(AddAttributeClassActionFactory.NAME));
		assertThat(rules.get(5).getAction().getParameters()[0], 
				is("/"));
		assertThat(rules.get(5).getAction().getParameters()[1], 
				is("'Common attributes'"));
		
		assertThat(rules.get(6).getCondition(), is("true"));
		assertThat(rules.get(6).getAction().getName(), 
				is(AddToGroupActionFactory.NAME));
		assertThat(rules.get(6).getAction().getParameters()[0], 
				is("/A/B"));

		assertThat(rules.get(7).getCondition(), is("true"));
		assertThat(rules.get(7).getAction().getName(), 
				is(AddToGroupActionFactory.NAME));
		assertThat(rules.get(7).getAction().getParameters()[0], 
				is("/D"));
	}
	
	private void checkStatements() throws EngineException
	{
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

		assertThat(stmts[5].getCondition(), is("groups contains '/A'"));
		assertThat(stmts[5].getExtraAttributesGroup(), is(nullValue()));
		assertThat(stmts[5].getFixedAttribute().getName(), is("postalcode"));
		assertThat(stmts[5].getFixedAttribute().getValues().size(), is(2));
		assertThat(stmts[5].getFixedAttribute().getValues().get(0), is("00-000"));
		assertThat(stmts[5].getFixedAttribute().getValues().get(1), is("01-000"));
	}
}
