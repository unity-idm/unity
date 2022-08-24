/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.ComplexAttributeMapping;
import io.imunity.scim.config.DataValue;
import io.imunity.scim.config.DataValue.DataValueType;
import io.imunity.scim.config.ReferenceAttributeMapping;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;
import io.imunity.scim.config.SimpleAttributeMapping;
import io.imunity.scim.schema.SCIMAttributeType;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.mvel.CachingMVELGroupProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;

@RunWith(MockitoJUnitRunner.class)
public class UserSchemaEvaluatorTest
{
	private UserSchemaEvaluator evaluator;

	private MappingEvaluatorRegistry mappingEvaluatorRegistry;

	@Mock
	private ComplexMappingEvaluator complexMappingEvaluator;
	@Mock
	private ReferenceMappingEvaluator referenceMappingEvaluator;
	@Mock
	private SimpleMappingEvaluator simpleMappingEvaluator;

	@Before
	public void init()
	{
		when(complexMappingEvaluator.getId()).thenReturn(ComplexAttributeMapping.id);
		when(simpleMappingEvaluator.getId()).thenReturn(SimpleAttributeMapping.id);
		when(referenceMappingEvaluator.getId()).thenReturn(ReferenceAttributeMapping.id);

		mappingEvaluatorRegistry = new MappingEvaluatorRegistry(
				Optional.of(List.of(complexMappingEvaluator, referenceMappingEvaluator, simpleMappingEvaluator)));
	}

	@Test
	public void shouldEvalEnabledUserSchemas() throws JsonProcessingException, EngineException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		InvocationContext.setCurrent(context);

		evaluator = new UserSchemaEvaluator(SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(List.of("/scim/Members1", "/scim/Members2")).build(), mappingEvaluatorRegistry);

		AttributeDefinitionWithMapping complexAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("name").withMultiValued(false)
						.withType(SCIMAttributeType.COMPLEX)
						.withSubAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
								.withAttributeDefinition(AttributeDefinition.builder().withName("formatted")
										.withMultiValued(false).withType(SCIMAttributeType.STRING).build())
								.withAttributeMapping(SimpleAttributeMapping.builder()
										.withDataValue(DataValue.builder().withType(DataValueType.IDENTITY)
												.withValue("formatted").build())
										.withDataArray(Optional.empty()).build())
								.build()))
						.build())
				.withAttributeMapping(ComplexAttributeMapping.builder().build()).build();

		SchemaWithMapping basicUserSchema = SchemaWithMapping.builder().withType(SchemaType.USER_CORE)
				.withAttributesWithMapping(List.of(complexAttr)).withEnable(true).build();

		SchemaWithMapping extUserSchema = SchemaWithMapping.builder().withType(SchemaType.USER).withEnable(true)
				.withAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("formatted")
								.withMultiValued(false).withType(SCIMAttributeType.STRING).build())
						.withAttributeMapping(SimpleAttributeMapping.builder()
								.withDataValue(DataValue.builder().withType(DataValueType.IDENTITY)
										.withValue("formatted").build())
								.withDataArray(Optional.empty()).build())
						.build()))
				.build();

		User user = User.builder()
				.withAttributes(List.of(new AttributeExt(
						new Attribute("familyName", StringAttributeSyntax.ID, "/scim", List.of("familyNameValue")),
						false)))
				.build();

		when(complexMappingEvaluator.eval(any(), any(), eq(mappingEvaluatorRegistry)))
				.thenReturn(EvaluationResult.builder().build());

		when(simpleMappingEvaluator.eval(any(), any(), eq(mappingEvaluatorRegistry)))
				.thenReturn(EvaluationResult.builder().build());

		evaluator.evalUserSchema(user, List.of(basicUserSchema, extUserSchema),
				new CachingMVELGroupProvider(new HashMap<>()));

		verify(complexMappingEvaluator).eval(any(), any(), eq(mappingEvaluatorRegistry));
		verify(simpleMappingEvaluator).eval(any(), any(), eq(mappingEvaluatorRegistry));
	}

	@Test
	public void shouldFrowardToComplexSingleAttributeEvaluator() throws JsonProcessingException, EngineException
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		InvocationContext.setCurrent(context);

		evaluator = new UserSchemaEvaluator(SCIMEndpointDescription.builder()
				.withBaseLocation(URI.create("https://localhost:2443/scim")).withRootGroup("/scim")
				.withMembershipGroups(List.of("/scim/Members1", "/scim/Members2")).build(), mappingEvaluatorRegistry);

		AttributeDefinitionWithMapping complexAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("name").withMultiValued(false)
						.withType(SCIMAttributeType.COMPLEX)
						.withSubAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
								.withAttributeDefinition(AttributeDefinition.builder().withName("formatted")
										.withMultiValued(false).withType(SCIMAttributeType.STRING).build())
								.withAttributeMapping(SimpleAttributeMapping.builder()
										.withDataValue(DataValue.builder().withType(DataValueType.IDENTITY)
												.withValue("formatted").build())
										.withDataArray(Optional.empty()).build())
								.build()))
						.build())
				.withAttributeMapping(ComplexAttributeMapping.builder().build()).build();

		SchemaWithMapping basicUserSchema = SchemaWithMapping.builder().withType(SchemaType.USER_CORE)
				.withAttributesWithMapping(List.of(complexAttr)).withEnable(true).build();

		User user = User.builder()
				.withAttributes(List.of(new AttributeExt(
						new Attribute("familyName", StringAttributeSyntax.ID, "/scim", List.of("familyNameValue")),
						false)))
				.build();

		when(complexMappingEvaluator.eval(any(), any(), eq(mappingEvaluatorRegistry)))
				.thenReturn(EvaluationResult.builder().build());

		evaluator.evalUserSchema(user, List.of(basicUserSchema), new CachingMVELGroupProvider(new HashMap<>()));

		ArgumentCaptor<AttributeDefinitionWithMapping> attrWithMappingCaptor = ArgumentCaptor
				.forClass(AttributeDefinitionWithMapping.class);
		ArgumentCaptor<EvaluatorContext> contextCaptor = ArgumentCaptor.forClass(EvaluatorContext.class);

		verify(complexMappingEvaluator).eval(attrWithMappingCaptor.capture(), contextCaptor.capture(),
				eq(mappingEvaluatorRegistry));
		assertThat(attrWithMappingCaptor.getValue(), is(complexAttr));
	}
}
