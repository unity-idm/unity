/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.ComplexAttributeMapping;
import io.imunity.scim.config.DataArray;
import io.imunity.scim.config.DataArray.DataArrayType;
import io.imunity.scim.config.DataValue;
import io.imunity.scim.config.DataValue.DataValueType;
import io.imunity.scim.config.SimpleAttributeMapping;
import io.imunity.scim.schema.SCIMAttributeType;
import pl.edu.icm.unity.exceptions.EngineException;

@RunWith(MockitoJUnitRunner.class)
public class ComplexMappingEvaluatorTest
{
	private ComplexMappingEvaluator evaluator;

	@Mock
	private DataArrayResolver dataArrayResolver;

	@Mock
	private SimpleMappingEvaluator simpleMappingEvaluator;

	private MappingEvaluatorRegistry mappingEvaluatorRegistry;

	@Before
	public void init()
	{
		evaluator = new ComplexMappingEvaluator(dataArrayResolver);

		when(simpleMappingEvaluator.getId()).thenReturn(SimpleAttributeMapping.id);

		mappingEvaluatorRegistry = new MappingEvaluatorRegistry(Optional.of(List.of(simpleMappingEvaluator)));

	}

	@Test
	public void shouldAddAttrObjToContextWhenEvalComplexMulti() throws EngineException
	{
		AttributeDefinitionWithMapping complexAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("name").withMultiValued(true)
						.withType(SCIMAttributeType.COMPLEX)

						.withSubAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
								.withAttributeDefinition(AttributeDefinition.builder().withName("familyName")
										.withMultiValued(false).withType(SCIMAttributeType.STRING)

										.build())
								.withAttributeMapping(SimpleAttributeMapping

										.builder()
										.withDataValue(DataValue.builder().withType(DataValueType.MVEL)
												.withValue("arrayObj").build())
										.withDataArray(Optional.empty()).build())
								.build()))
						.build())
				.withAttributeMapping(ComplexAttributeMapping.builder()
						.withDataArray(Optional.of(
								DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()))
						.build())
				.build();

		doReturn(List.of("f1", "f2")).when(dataArrayResolver).resolve(
				eq(DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()), any());

		evaluator.eval(complexAttr, EvaluatorContext.builder().build(), mappingEvaluatorRegistry);

		ArgumentCaptor<AttributeDefinitionWithMapping> attrWithMappingCaptor = ArgumentCaptor
				.forClass(AttributeDefinitionWithMapping.class);
		ArgumentCaptor<EvaluatorContext> contextCaptor = ArgumentCaptor.forClass(EvaluatorContext.class);

		verify(simpleMappingEvaluator, times(2)).eval(attrWithMappingCaptor.capture(), contextCaptor.capture(),
				eq(mappingEvaluatorRegistry));

		assertThat(contextCaptor.getAllValues().get(0).arrayObj, is("f1"));
		assertThat(contextCaptor.getAllValues().get(1).arrayObj, is("f2"));
	}

	@Test
	public void shouldEvalComplexSingleWithSimpleSubAttribute() throws EngineException
	{
		AttributeDefinitionWithMapping complexAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("name").withMultiValued(false)
						.withType(SCIMAttributeType.COMPLEX)
						.withSubAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
								.withAttributeDefinition(AttributeDefinition.builder().withName("familyName")
										.withMultiValued(false).withType(SCIMAttributeType.STRING).build())
								.withAttributeMapping(SimpleAttributeMapping

										.builder()
										.withDataValue(DataValue.builder().withType(DataValueType.ATTRIBUTE)
												.withValue("familyName").build())
										.withDataArray(Optional.empty()).build())
								.build()))
						.build())
				.withAttributeMapping(ComplexAttributeMapping.builder()
						.withDataArray(Optional.of(
								DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()))
						.build())
				.build();

		when(simpleMappingEvaluator.eval(eq(AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("familyName").withMultiValued(false)
						.withType(SCIMAttributeType.STRING).build())
				.withAttributeMapping(SimpleAttributeMapping.builder()
						.withDataValue(
								DataValue.builder().withType(DataValueType.ATTRIBUTE).withValue("familyName").build())
						.withDataArray(Optional.empty()).build())
				.build()), any(), eq(mappingEvaluatorRegistry))).thenReturn(Map.of("familyName", "f1"));

		Map<String, Object> value = evaluator.eval(complexAttr, EvaluatorContext.builder().build(),
				mappingEvaluatorRegistry);

		assertThat(value.get("name"), is(Map.of("familyName", "f1")));

	}

	@Test
	public void shouldEvalComplexMultiWithSimpleSubAttribute() throws EngineException
	{
		AttributeDefinitionWithMapping complexAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("name").withMultiValued(true)
						.withType(SCIMAttributeType.COMPLEX)
						.withSubAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
								.withAttributeDefinition(AttributeDefinition.builder().withName("familyName")
										.withMultiValued(false).withType(SCIMAttributeType.STRING).build())
								.withAttributeMapping(SimpleAttributeMapping

										.builder()
										.withDataValue(DataValue.builder().withType(DataValueType.ATTRIBUTE)
												.withValue("subFamily").build())
										.withDataArray(Optional.empty()).build())
								.build()))
						.build())
				.withAttributeMapping(ComplexAttributeMapping.builder()
						.withDataArray(Optional.of(
								DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()))
						.build())
				.build();

		doReturn(List.of("f1", "f2")).when(dataArrayResolver).resolve(
				eq(DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()), any());

		when(simpleMappingEvaluator.eval(eq(AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("familyName").withMultiValued(false)
						.withType(SCIMAttributeType.STRING).build())
				.withAttributeMapping(SimpleAttributeMapping.builder()
						.withDataValue(
								DataValue.builder().withType(DataValueType.ATTRIBUTE).withValue("subFamily").build())
						.withDataArray(Optional.empty()).build())
				.build()), any(), eq(mappingEvaluatorRegistry))).thenReturn(Map.of("familyName", "f1"));

		Map<String, Object> value = evaluator.eval(complexAttr, EvaluatorContext.builder().build(),
				mappingEvaluatorRegistry);

		assertThat(value.get("name"), is(List.of(Map.of("familyName", "f1"), Map.of("familyName", "f1"))));

	}

}
