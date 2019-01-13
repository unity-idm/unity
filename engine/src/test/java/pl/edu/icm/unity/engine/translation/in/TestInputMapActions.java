/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.mockito.AdditionalAnswers;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.EntityChange;
import pl.edu.icm.unity.engine.api.translation.in.GroupEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationContextFactory;
import pl.edu.icm.unity.engine.api.translation.in.MappedGroup;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.engine.translation.in.action.EntityChangeActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MapAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MapGroupActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MapIdentityActionFactory;
import pl.edu.icm.unity.engine.translation.in.action.MultiMapAttributeActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;

public class TestInputMapActions
{
	@Test
	public void testMapAttribute() throws EngineException
	{
		AttributeType sA = new AttributeType("stringA", StringAttributeSyntax.ID);
		AttributeTypeSupport attrsMan = mock(AttributeTypeSupport.class);
		when(attrsMan.getType("stringA")).thenReturn(sA);
		
		AttributeValueConverter converter = mock(AttributeValueConverter.class); 
		when(converter.externalValuesToInternal(eq("stringA"), anyList())).
			then(AdditionalAnswers.returnsSecondArg());
		when(converter.externalValuesToInternal(eq("stringA"), anyList())).
			then(AdditionalAnswers.returnsSecondArg());

		MapAttributeActionFactory factory = new MapAttributeActionFactory(attrsMan, converter);
		
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
		assertEquals("stringA", a.getName());
		assertEquals("a1-a2-idd", a.getValues().get(0));
	}

	@Test
	public void testMultiMapAttribute() throws EngineException
	{
		AttributeType sA = new AttributeType("stringA", StringAttributeSyntax.ID);
		AttributeTypeSupport attrsMan = mock(AttributeTypeSupport.class);
		when(attrsMan.getAttributeTypes()).thenReturn(Lists.newArrayList(sA));
		
		AttributeValueConverter converter = mock(AttributeValueConverter.class); 
		when(converter.externalValuesToInternal(eq("stringA"), anyList())).
			then(AdditionalAnswers.returnsSecondArg());
		when(converter.externalValuesToInternal(eq("stringA"), anyList())).
			then(AdditionalAnswers.returnsSecondArg());
		
		MultiMapAttributeActionFactory factory = new MultiMapAttributeActionFactory(attrsMan, converter);
		
		InputTranslationAction mapAction = factory.getInstance("attribute stringA /A/B\n"
				+ "other stringA /A\n"
				+ "missing stringA /A", 
				AttributeEffectMode.CREATE_OR_UPDATE.toString());
				
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("test");
		input.addIdentity(new RemoteIdentity("idd", "i1"));
		input.addAttribute(new RemoteAttribute("attribute", "a1", "a2"));
		input.addAttribute(new RemoteAttribute("other", "a2"));
		
		MappingResult result = mapAction.invoke(input, 
				InputTranslationContextFactory.createMvelContext(input), "testProf");
		
		Attribute a = result.getAttributes().get(0).getAttribute();
		assertEquals("stringA", a.getName());
		assertEquals("a1", a.getValues().get(0));
		assertEquals("a2", a.getValues().get(1));

		Attribute b = result.getAttributes().get(1).getAttribute();
		assertEquals("stringA", b.getName());
		assertEquals("a2", b.getValues().get(0));
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
		
		MapIdentityActionFactory factory = new MapIdentityActionFactory(idTypesReg);
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
		assertEquals("userName", mi.getIdentity().getTypeId());
		assertEquals("a1-a2-idvalue", mi.getIdentity().getValue());
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
