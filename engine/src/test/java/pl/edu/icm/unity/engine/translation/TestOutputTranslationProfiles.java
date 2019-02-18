/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfile;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfileRepository;
import pl.edu.icm.unity.engine.translation.out.action.CreateAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.CreateIdentityActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.CreatePersistentAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.CreatePersistentIdentityActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.FilterAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.out.action.IncludeOutputProfileActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

/**
 * Integration and engine related part tests of the subsystem mapping the remote data to the unity's representation. 
 * @author K. Benedyczak
 */
public class TestOutputTranslationProfiles extends DBIntegrationTestBase
{
	@Autowired
	private TranslationProfileManagement tprofMan;
	@Autowired
	private OutputTranslationEngine outputTrEngine;
	@Autowired
	private OutputTranslationActionsRegistry outtactionReg;
	@Autowired
	private TransactionalRunner tx;
	@Autowired
	private AttributeValueConverter attrConverter;
	@Autowired
	OutputTranslationProfileRepository outputProfileRepo;
	
	@Test
	public void testOutputPersistence() throws Exception
	{
		assertThat(listDefaultModeProfiles().size(), is(0));
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(CreateAttributeActionFactory.NAME, new String[] {
				"dynAttr", 
				"'joe'", "false"});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(FilterAttributeActionFactory.NAME, new String[] {
				"attr"}); 
		rules.add(new TranslationRule("true", action2));
		
		TranslationProfile toAdd = new TranslationProfile("p1", "", ProfileType.OUTPUT, rules);
		tprofMan.addProfile(toAdd);
		
		Map<String, TranslationProfile> profiles = tprofMan.listOutputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(2, profiles.get("p1").getRules().size());
		assertEquals(CreateAttributeActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getName());
		assertEquals("dynAttr", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		assertEquals("'joe'", profiles.get("p1").getRules().get(0).getAction().getParameters()[1]);
		
		rules.remove(0);
		toAdd = new TranslationProfile("p1", "", ProfileType.OUTPUT, rules);
		tprofMan.updateProfile(toAdd);
		profiles = tprofMan.listOutputProfiles();
		assertNotNull(profiles.get("p1"));
		assertEquals(1, profiles.get("p1").getRules().size());
		assertEquals(FilterAttributeActionFactory.NAME, profiles.get("p1").getRules().get(0).
				getAction().getName());
		assertEquals("attr", profiles.get("p1").getRules().get(0).getAction().getParameters()[0]);
		
		tprofMan.removeProfile(ProfileType.OUTPUT, "p1");
	
		assertThat(listDefaultModeProfiles().size(), is(0));

	}
	
	private List<TranslationProfile> listDefaultModeProfiles() throws EngineException
	{
		return tprofMan.listOutputProfiles().values().stream()
				.filter(t -> t.getProfileMode() == ProfileMode.DEFAULT)
				.collect(Collectors.toList());
	}
	
	@Test
	public void testIntegratedOutput() throws Exception
	{
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		oType.setMaxElements(10);
		aTypeMan.addAttributeType(oType);
		
		groupsMan.addGroup(new Group("/A"));
		
		Identity user = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1234"), 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, EntityState.valid, false);
		Entity userE = idsMan.getEntity(new EntityParam(user));
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(
				CreatePersistentIdentityActionFactory.NAME, new String[] {
						X500Identity.ID, 
						"'CN=foo,O=ICM,DC=' + authenticatedWith[0]"});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(
				CreatePersistentAttributeActionFactory.NAME, new String[] {
						"o", "'ICM'", "false", "/"}); 
		rules.add(new TranslationRule("true", action2));
		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.OUTPUT, rules);
		
		getUser();
		InvocationContext.getCurrent().getLoginSession().addAuthenticatedIdentities(Sets.newHashSet("user1"));
		
		TranslationInput input = new TranslationInput(new ArrayList<Attribute>(), userE, 
				"/", Collections.singleton("/"),
				"req", Collections.emptyList(), 
				"proto", "subProto", Collections.emptyMap());

		tx.runInTransactionThrowing(() -> {
			OutputTranslationProfile tp1 = new OutputTranslationProfile(tp1Cfg, 
					outputProfileRepo, outtactionReg, attrConverter);
			TranslationResult result = tp1.translate(input);
			outputTrEngine.process(input, result);
		});
		
		setupAdmin();
		
		EntityParam ep = new EntityParam(new IdentityTaV(X500Identity.ID, "CN=foo,O=ICM,DC=user1"));
		Entity entity = idsMan.getEntity(ep);
		assertEquals(userE.getId(), entity.getId());
		assertEquals(3, entity.getIdentities().size());
		Identity id = getIdentityByType(entity.getIdentities(), X500Identity.ID);
		assertNotNull(id.getCreationTs());
		assertNotNull(id.getUpdateTs());
		assertEquals("p1", id.getTranslationProfile());
		
		Collection<AttributeExt> atrs = attrsMan.getAttributes(ep, "/", "o");
		assertEquals(1, atrs.size());
		AttributeExt at = atrs.iterator().next();
		assertEquals(1, at.getValues().size());
		assertEquals("ICM", at.getValues().get(0));
		assertNotNull(at.getCreationTs());
		assertNotNull(at.getUpdateTs());
		assertEquals("p1", at.getTranslationProfile());
	}
	
	/**
	 * We need to ensure that attribute created with CreateAttribute action are always string
	 * and have also string values. 
	 * @throws Exception
	 */
	@Test
	public void outputTranslationProducesStringAttributes() throws Exception
	{
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(oType);
		AttributeType eType = new AttributeType("e", VerifiableEmailAttributeSyntax.ID);
		aTypeMan.addAttributeType(eType);
		AttributeType fType = new AttributeType("f", FloatingPointAttributeSyntax.ID);
		aTypeMan.addAttributeType(fType);
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(
				CreateAttributeActionFactory.NAME, new String[] {
						"a1", "attr['o']", "false"});
		rules.add(new TranslationRule("true", action1));
		TranslationAction action2 = new TranslationAction(
				CreateAttributeActionFactory.NAME, new String[] {
						"a2", "attr['e']", "false"}); 
		rules.add(new TranslationRule("true", action2));
		TranslationAction action3 = new TranslationAction(
				CreateAttributeActionFactory.NAME, new String[] {
						"a3", "attr['f']", "false"}); 
		rules.add(new TranslationRule("true", action3));

		TranslationProfile tp1Cfg = new TranslationProfile("p1", "", ProfileType.OUTPUT, rules);

		Entity userE = getUser();
		setupUserContext(DEF_USER, null);
		InvocationContext.getCurrent().getLoginSession().addAuthenticatedIdentities(Sets.newHashSet("user1"));
		
		TranslationInput input = new TranslationInput(
				Lists.newArrayList(
					StringAttribute.of("o", "/", "v1"),
					VerifiableEmailAttribute.of("e", "/", "email@example.com"),
					FloatingPointAttribute.of("f", "/", 123d)), 
				userE, 
				"/", Collections.singleton("/"),
				"req", Collections.emptyList(),
				"proto", "subProto", Collections.emptyMap());
		
		TranslationResult result = tx.runInTransactionRetThrowing(() -> {
			OutputTranslationProfile tp1 = new OutputTranslationProfile(tp1Cfg, 
					outputProfileRepo, outtactionReg, attrConverter);
			return tp1.translate(input);
		});
		
		Collection<DynamicAttribute> attributes = result.getAttributes();
		assertThat(attributes.size(), is(6));
		for (DynamicAttribute da: attributes)
		{
			Attribute a = da.getAttribute();
			if (a.getName().startsWith("a"))
			{
				assertThat(a.getValueSyntax(), is(StringAttributeSyntax.ID));
				for (Object val: a.getValues())
					assertThat(val, is(instanceOf(String.class)));
			}
		}
	}
	
	@Test
	public void includedProfileShouldCreateAttribute() throws Exception
	{
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(oType);
		AttributeType eType = new AttributeType("e", VerifiableEmailAttributeSyntax.ID);
		aTypeMan.addAttributeType(eType);
		
		
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(CreateAttributeActionFactory.NAME,
				new String[] { "a0", "attr['o']", "false" });
		rules.add(new TranslationRule("true", action1));
		TranslationProfile included0 = new TranslationProfile("included0", "",
				ProfileType.OUTPUT, rules);
		tprofMan.addProfile(included0);
	
		rules = new ArrayList<>();
		action1 = new TranslationAction(CreateAttributeActionFactory.NAME,
				new String[] { "a1", "attr['o']", "false" });
		rules.add(new TranslationRule("true", action1));
		TranslationAction include = new TranslationAction(
				IncludeOutputProfileActionFactory.NAME,
				new String[] { "included0" });
		rules.add(new TranslationRule("true", include));
	
		TranslationProfile included1 = new TranslationProfile("included1", "",
				ProfileType.OUTPUT, rules);
		tprofMan.addProfile(included1);
		
		rules = new ArrayList<>();
		TranslationAction action2 = new TranslationAction(CreateAttributeActionFactory.NAME,
				new String[] { "a2", "attr['e']", "false" });
		rules.add(new TranslationRule("true", action2));

		TranslationAction include1 = new TranslationAction(
				IncludeOutputProfileActionFactory.NAME,
				new String[] { "included1" });
		rules.add(new TranslationRule("true", include1));
		TranslationProfile tpMain = new TranslationProfile("tp1", "", ProfileType.OUTPUT,
				rules);
		tprofMan.addProfile(tpMain);

		Entity userE = getUser();
		TranslationInput input = new TranslationInput(
				Lists.newArrayList(StringAttribute.of("o", "/", "v1"),
						VerifiableEmailAttribute.of("e", "/",
								"email@example.com")),
				userE, "/", Collections.singleton("/"), 
				"req", Collections.emptyList(),
				"proto", "subProto", 
				Collections.emptyMap());
		TranslationResult res = tx.runInTransactionRetThrowing(() -> {
			OutputTranslationProfile tp1 = new OutputTranslationProfile(tpMain,
					outputProfileRepo, outtactionReg, attrConverter);
			TranslationResult result = tp1.translate(input);
			return result;
		});

		Collection<DynamicAttribute> attributes = res.getAttributes();

		assertThat(attributes.size(), is(5));
		for (DynamicAttribute da : attributes)
		{
			Attribute attr = da.getAttribute();
		
			if (attr.getName().equals("a0"))
			{
				assertThat(attr.getValues().get(0), is("v1"));
			}
			
			if (attr.getName().equals("a1"))
			{
				assertThat(attr.getValues().get(0), is("v1"));
			}

			if (attr.getName().equals("a2"))
			{
				assertThat(attr.getValues().get(0), is("email@example.com"));
			}

		}
	}
	
	@Test
	public void includedProfileShouldOverwriteAttributeFromParentProfile() throws Exception
	{

		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(CreateAttributeActionFactory.NAME,
				new String[] { "a1", "'x2'", "false" });
		rules.add(new TranslationRule("true", action1));
		TranslationProfile included = new TranslationProfile("included", "",
				ProfileType.OUTPUT, rules);
		tprofMan.addProfile(included);

		rules = new ArrayList<>();
		TranslationAction action2 = new TranslationAction(CreateAttributeActionFactory.NAME,
				new String[] { "a1", "'x'", "false" });
		rules.add(new TranslationRule("true", action2));

		TranslationAction include = new TranslationAction(
				IncludeOutputProfileActionFactory.NAME,
				new String[] { "included" });
		rules.add(new TranslationRule("true", include));
		TranslationProfile tpMain = new TranslationProfile("tp1", "", ProfileType.OUTPUT,
				rules);
		tprofMan.addProfile(tpMain);

		Entity userE = getUser();
		
		TranslationInput input = new TranslationInput(
				Lists.newArrayList(),
				userE, "/", Collections.singleton("/"), 
				"req", Collections.emptyList(),
				"proto", "subProto", 
				Collections.emptyMap());

		TranslationResult res = tx.runInTransactionRetThrowing(() -> {
			OutputTranslationProfile tp1 = new OutputTranslationProfile(tpMain,
					outputProfileRepo, outtactionReg, attrConverter);
			TranslationResult result = tp1.translate(input);
			return result;
		});

		Collection<DynamicAttribute> attributes = res.getAttributes();

		assertThat(attributes.size(), is(1));
		for (DynamicAttribute da : attributes)
		{
			Attribute attr = da.getAttribute();
			if (attr.getName().equals("a1"))
			{
				assertThat(attr.getValues().get(0), is("x2"));
			}
		}
	}

	@Test
	public void profileShouldOverwriteAttribute() throws Exception
	{
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(oType);

		AttributeType eType = new AttributeType("e", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(eType);

		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(CreateAttributeActionFactory.NAME,
				new String[] { "o", "attr['e']", "false" });
		rules.add(new TranslationRule("true", action1));
		TranslationProfile tp1Cfg = new TranslationProfile("tp1", "", ProfileType.OUTPUT,
				rules);

		TranslationInput input = new TranslationInput(
				Lists.newArrayList(StringAttribute.of("o", "/", "v1"),
						StringAttribute.of("e", "/", "v2")),
				getUser(), "/", Collections.singleton("/"), 
				"req", Collections.emptyList(),
				"proto",
				"subProto", Collections.emptyMap());

		TranslationResult result = tx.runInTransactionRetThrowing(() -> {
			OutputTranslationProfile tp1 = new OutputTranslationProfile(tp1Cfg,
					outputProfileRepo, outtactionReg, attrConverter);
			return tp1.translate(input);
		});

		Collection<DynamicAttribute> attributes = result.getAttributes();
		assertThat(attributes.size(), is(2));
		for (DynamicAttribute da : attributes)
		{
			Attribute a = da.getAttribute();
			if (a.getName().equals("o"))
			{
				assertThat(a.getValues().get(0), is("v2"));
			}
		}
	}

	@Test
	public void profileShouldManipulateOfAttributeExternalValueRepresetation() throws Exception
	{
		AttributeType eType = new AttributeType("e", VerifiableEmailAttributeSyntax.ID);
		aTypeMan.addAttributeType(eType);

		ArrayList<TranslationRule> rules = new ArrayList<>();
		TranslationAction action = new TranslationAction(CreateAttributeActionFactory.NAME,
				new String[] { "a2", "attr['e']", "false" });
		rules.add(new TranslationRule("true", action));
		TranslationProfile tp1Cfg = new TranslationProfile("tp1", "", ProfileType.OUTPUT,
				rules);

		TranslationInput input = new TranslationInput(
				Lists.newArrayList(VerifiableEmailAttribute.of("e", "/",
						"email@example.com")),
				getUser(), "/", Collections.singleton("/"), 
				"req", Collections.emptyList(),
				"proto", "subProto", Collections.emptyMap());

		TranslationResult result = tx.runInTransactionRetThrowing(() -> {
			OutputTranslationProfile tp1 = new OutputTranslationProfile(tp1Cfg,
					outputProfileRepo, outtactionReg, attrConverter);
			return tp1.translate(input);
		});

		Collection<DynamicAttribute> attributes = result.getAttributes();
		assertThat(attributes.size(), is(2));
		for (DynamicAttribute da : attributes)
		{
			Attribute a = da.getAttribute();
			if (a.getName().equals("a2"))
			{
				assertThat(a.getValues().get(0), is("email@example.com"));
			}
		}
	}

	@Test
	public void includedProfileShouldOverwriteIdentityFromParentProfile() throws Exception
	{

		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(CreateIdentityActionFactory.NAME,
				new String[] { UsernameIdentity.ID, "'x'" });
		rules.add(new TranslationRule("true", action1));
		TranslationProfile tp1Cfg = new TranslationProfile("included", "",
				ProfileType.OUTPUT, rules);
		tprofMan.addProfile(tp1Cfg);

		rules = new ArrayList<>();
		action1 = new TranslationAction(CreateIdentityActionFactory.NAME,
				new String[] { UsernameIdentity.ID, "'x2'" });
		rules.add(new TranslationRule("true", action1));
		action1 = new TranslationAction(IncludeOutputProfileActionFactory.NAME,
				new String[] { "included" });

		rules.add(new TranslationRule("true", action1));

		TranslationProfile tp2Cfg = new TranslationProfile("tp1", "", ProfileType.OUTPUT,
				rules);

		Entity e = getUser();

		TranslationInput input = new TranslationInput(Lists.newArrayList(), e, "/",
				Collections.singleton("/"), "req", Collections.emptyList(),
				"proto", "subProto", 
				Collections.emptyMap());
		TranslationResult result = tx.runInTransactionRetThrowing(() -> {
			OutputTranslationProfile tp1 = new OutputTranslationProfile(tp2Cfg,
					outputProfileRepo, outtactionReg, attrConverter);
			return tp1.translate(input);
		});

		assertThat(result.getIdentities().size(), is(2));

		for (IdentityParam id : result.getIdentities())
		{
			if (id.getTypeId().equals(EmailIdentity.ID))
				assertThat(id.getValue(), is("x"));

		}

	}

	@Test
	public void profileShouldOverwriteIdentity() throws Exception
	{

		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(CreateIdentityActionFactory.NAME,
				new String[] { UsernameIdentity.ID, "'x'" });
		rules.add(new TranslationRule("true", action1));
		TranslationProfile tp2Cfg = new TranslationProfile("tp1", "", ProfileType.OUTPUT,
				rules);

		Entity e = getUser();

		TranslationInput input = new TranslationInput(Lists.newArrayList(), e, "/",
				Collections.singleton("/"), "req", Collections.emptyList(),
				"proto", "subProto", 
				Collections.emptyMap());
		TranslationResult result = tx.runInTransactionRetThrowing(() -> {
			OutputTranslationProfile tp1 = new OutputTranslationProfile(tp2Cfg,
					outputProfileRepo, outtactionReg, attrConverter);
			return tp1.translate(input);
		});

		assertThat(result.getIdentities().size(), is(2));

		for (IdentityParam id : result.getIdentities())
		{
			if (id.getTypeId().equals(UsernameIdentity.ID))
				assertThat(id.getValue(), is("x"));

		}

	}
	
	@Test
	public void profileShouldFilterAlsoPersistentAttribute() throws Exception
	{
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(oType);

		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(
				CreatePersistentAttributeActionFactory.NAME,
				new String[] { "o", "'ICM'", "false", "/" });
		rules.add(new TranslationRule("true", action1));

		action1 = new TranslationAction(FilterAttributeActionFactory.NAME,
				new String[] { "o*" });
		rules.add(new TranslationRule("true", action1));

		TranslationProfile tp2Cfg = new TranslationProfile("tp1", "", ProfileType.OUTPUT,
				rules);

		TranslationInput input = new TranslationInput(Lists.newArrayList(), getUser(), "/",
				Collections.singleton("/"), "req", Collections.emptyList(),
				"proto", "subProto", 
				Collections.emptyMap());

		TranslationResult result = tx.runInTransactionRetThrowing(() -> {
			OutputTranslationProfile tp1 = new OutputTranslationProfile(tp2Cfg,
					outputProfileRepo, outtactionReg, attrConverter);
			return tp1.translate(input);
		});
		assertThat(result.getAttributes().size(), is(0));
		assertThat(result.getAttributesToPersist().size(), is(0));
	}
	
	@Test
	public void profileShouldNotFailIfAttributeIsMissing() throws Exception
	{
		AttributeType oType = new AttributeType("o", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(oType);

		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction(
				CreatePersistentAttributeActionFactory.NAME,
				new String[] { "o", "'ICM'", "false", "/" });
		rules.add(new TranslationRule("true", action1));

		TranslationProfile tp2Cfg = new TranslationProfile("tp1", "", ProfileType.OUTPUT,
				rules);
		TranslationInput input = new TranslationInput(Lists.newArrayList(), getUser(), "/",
				Collections.singleton("/"), "req", Collections.emptyList(),
				"proto", "subProto", 
				Collections.emptyMap());
		aTypeMan.removeAttributeType("o", true);	
		try{
			TranslationResult result = tx.runInTransactionRetThrowing(() -> {
				OutputTranslationProfile tp1 = new OutputTranslationProfile(tp2Cfg,
						outputProfileRepo, outtactionReg, attrConverter);
				return tp1.translate(input);
			});
			assertThat(result.getAttributes().size(), is(0));
			assertThat(result.getAttributesToPersist().size(), is(0));
		} catch (Exception e)
		{
			fail("Exception throw when run misconfigured action");
		}
		
	}

	private Entity getUser() throws Exception
	{
		setupPasswordAuthn();
		Identity user = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		return idsMan.getEntity(new EntityParam(user));
	}
}



