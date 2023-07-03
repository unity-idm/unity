/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.EntityChange;
import pl.edu.icm.unity.engine.api.translation.in.GroupEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationContextFactory;
import pl.edu.icm.unity.engine.api.translation.in.MappedGroup;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.translation.in.action.EntityChangeActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MapAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MapGroupActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MapIdentityActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MultiMapAttributeActionFactory;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class TestInputMapActions
{
	@Test
	public void testMapAttribute() throws EngineException
	{
		AttributeType sA = new AttributeType("stringA", StringAttributeSyntax.ID);
		AttributeTypeSupport attrsMan = mock(AttributeTypeSupport.class);
		when(attrsMan.getType("stringA")).thenReturn(sA);
		
		ExternalDataParser parser = mock(ExternalDataParser.class);
		Attribute attr = new Attribute("stringA", StringAttributeSyntax.ID, "/A/B", Lists.newArrayList("a1-a2-idd"));
		when(parser.parseAsAttribute(any(), any(), eq(Lists.newArrayList("a1-a2-idd")), any(), any())).
			thenReturn(attr);

		MapAttributeActionFactory factory = new MapAttributeActionFactory(attrsMan, parser);
		
		InputTranslationAction mapAction = factory.getInstance("stringA", "/A/B", 
				"attr['attribute'] + '-' + attr['other'] + '-' + id", 
				AttributeEffectMode.CREATE_OR_UPDATE.toString());
				
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("idd", "i1"));
		input.addAttribute(new RemoteAttribute("attribute", "a1"));
		input.addAttribute(new RemoteAttribute("other", "a2"));
		
		Map<String, Object> ctx = InputTranslationContextFactory.createMvelContext(input);
		MappingResult result = mapAction.invoke(input, ctx, "testProf");
		
		Attribute a = result.getAttributes().get(0).getAttribute();
		assertThat(a).isEqualTo(attr);
	}

	@Test
	public void testMultiMapAttribute() throws EngineException
	{
		AttributeType sA = new AttributeType("stringA", StringAttributeSyntax.ID);
		AttributeTypeSupport attrsMan = mock(AttributeTypeSupport.class);
		when(attrsMan.getAttributeTypes()).thenReturn(Lists.newArrayList(sA));
		
		ExternalDataParser parser = mock(ExternalDataParser.class);
		Attribute attr1 = new Attribute("stringA", StringAttributeSyntax.ID, "/A/B", 
				newArrayList("a1", "a2"));
		Attribute attr2 = new Attribute("stringA", StringAttributeSyntax.ID, "/A/B", 
				newArrayList("a3"));
		when(parser.parseAsAttribute(any(), any(), eq(attr1.getValues()), any(), any())).
			thenReturn(attr1);
		when(parser.parseAsAttribute(any(), any(), eq(attr2.getValues()), any(), any())).
			thenReturn(attr2);
		
		MultiMapAttributeActionFactory factory = new MultiMapAttributeActionFactory(attrsMan, parser);
		
		InputTranslationAction mapAction = factory.getInstance("attribute stringA /A/B\n"
				+ "other stringA /A\n"
				+ "missing stringA /A", 
				AttributeEffectMode.CREATE_OR_UPDATE.toString());
				
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("idd", "i1"));
		input.addAttribute(new RemoteAttribute("attribute", "a1", "a2"));
		input.addAttribute(new RemoteAttribute("other", "a3"));
		
		MappingResult result = mapAction.invoke(input, 
				InputTranslationContextFactory.createMvelContext(input), "testProf");
		
		Attribute a = result.getAttributes().get(0).getAttribute();
		assertThat(a).isEqualTo(attr1);

		Attribute b = result.getAttributes().get(1).getAttribute();
		assertThat(b).isEqualTo(attr2);
	}


	@Test
	public void testMapGroup() throws EngineException
	{
		MapGroupActionFactory factory = new MapGroupActionFactory();
		InputTranslationAction mapAction = factory.getInstance("'/A/B/' + attr['attribute']", 
				GroupEffectMode.CREATE_GROUP_IF_MISSING.name());
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addAttribute(new RemoteAttribute("attribute", "a1"));
		
		MappingResult result = mapAction.invoke(input, 
				InputTranslationContextFactory.createMvelContext(input), "testProf");
		
		assertEquals(1, result.getGroups().size());
		MappedGroup mg = result.getGroups().iterator().next();
		assertEquals("/A/B/a1", mg.getGroup());
		assertEquals(GroupEffectMode.CREATE_GROUP_IF_MISSING, mg.getCreateIfMissing());
	}
	
	@Test
	public void testMapIdentity() throws EngineException
	{
		IdentityTypesRegistry idTypesReg = mock(IdentityTypesRegistry.class);
		when(idTypesReg.getByName("userName")).thenReturn(new UsernameIdentity());
		
		ExternalDataParser parser = mock(ExternalDataParser.class);
		IdentityParam id = new IdentityParam("userName", "a1-a2-idvalue");
		when(parser.parseAsIdentity(any(), eq(id.getValue()), any(), any())).thenReturn(id);
		
		MapIdentityActionFactory factory = new MapIdentityActionFactory(idTypesReg, parser);
		InputTranslationAction mapAction = factory.getInstance("userName", 
				"attr['attribute:colon'] + '-' + attr['other'] + '-' + id", 
				"CR", IdentityEffectMode.REQUIRE_MATCH.toString());
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("idvalue", "idtype"));
		input.addAttribute(new RemoteAttribute("attribute:colon", "a1"));
		input.addAttribute(new RemoteAttribute("other", "a2"));
		
		MappingResult result = mapAction.invoke(input, 
				InputTranslationContextFactory.createMvelContext(input), "testProf");
		
		MappedIdentity mi = result.getIdentities().get(0);
		assertEquals("CR", mi.getCredentialRequirement());
		assertEquals(IdentityEffectMode.REQUIRE_MATCH, mi.getMode());
		assertThat(mi.getIdentity()).isEqualTo(id);
	}
	
	@Test
	public void testEntityChange() throws EngineException
	{
		EntityChangeActionFactory factory = new EntityChangeActionFactory();
		InputTranslationAction mapAction = factory.getInstance(
				EntityScheduledOperation.REMOVE.toString(), 
				"1");
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		
		MappingResult result = mapAction.invoke(input, InputTranslationContextFactory.createMvelContext(input), 
				"testProf");
		
		EntityChange mi = result.getEntityChanges().get(0);
		assertEquals(EntityScheduledOperation.REMOVE, mi.getScheduledOperation());
		Date nextDay = new Date(System.currentTimeMillis() + 3600L*24*1000); 
		assertTrue(nextDay.getTime() >= mi.getScheduledTime().getTime());
		assertTrue(nextDay.getTime()-1000 < mi.getScheduledTime().getTime());
	}
}
