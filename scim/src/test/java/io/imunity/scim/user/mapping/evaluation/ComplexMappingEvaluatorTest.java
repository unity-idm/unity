/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.ComplexAttributeMapping;
import io.imunity.scim.config.DataArray;
import io.imunity.scim.config.DataArray.DataArrayType;
import io.imunity.scim.config.DataValue;
import io.imunity.scim.config.DataValue.DataValueType;
import io.imunity.scim.config.SimpleAttributeMapping;
import io.imunity.scim.schema.SCIMAttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;

@ExtendWith(MockitoExtension.class)
public class ComplexMappingEvaluatorTest
{
	private ComplexMappingEvaluator evaluator;

	@Mock
	private DataArrayResolver dataArrayResolver;

	@Mock
	private SimpleMappingEvaluator simpleMappingEvaluator;

	private MappingEvaluatorRegistry mappingEvaluatorRegistry;

	@BeforeEach
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

		ArgumentCaptor<AttributeDefinitionWithMapping> attrWithMappingCaptor = ArgumentCaptor
				.forClass(AttributeDefinitionWithMapping.class);
		ArgumentCaptor<EvaluatorContext> contextCaptor = ArgumentCaptor.forClass(EvaluatorContext.class);

		when(simpleMappingEvaluator.eval(attrWithMappingCaptor.capture(), contextCaptor.capture(),
				eq(mappingEvaluatorRegistry))).thenReturn(EvaluationResult.builder().build());
		
		
		evaluator.eval(complexAttr, EvaluatorContext.builder().build(), mappingEvaluatorRegistry);

		

		assertThat(contextCaptor.getAllValues().get(0).arrayObj).isEqualTo("f1");
		assertThat(contextCaptor.getAllValues().get(1).arrayObj).isEqualTo("f2");
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
				.build()), any(), eq(mappingEvaluatorRegistry))).thenReturn(EvaluationResult.builder().withAttributeName("familyName").withValue(Optional.of("f1")).build());

		EvaluationResult value = evaluator.eval(complexAttr, EvaluatorContext.builder().build(),
				mappingEvaluatorRegistry);

		assertThat(value.attributeName).isEqualTo("name");
		assertThat(value.value.get()).isEqualTo(Map.of("familyName", "f1"));
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
				.build()), any(), eq(mappingEvaluatorRegistry))).thenReturn(EvaluationResult.builder().withAttributeName("familyName")
						.withValue(Optional.of("f1")).build());

		EvaluationResult value = evaluator.eval(complexAttr, EvaluatorContext.builder().build(),
				mappingEvaluatorRegistry);

		assertThat(value.attributeName).isEqualTo("name");
		assertThat(value.value.get()).isEqualTo(List.of(Map.of("familyName", "f1"), Map.of("familyName", "f1")));
	}
	
	@Test
	public void shouldEvalComplexMultiWithSimpleSubAttributeWithArrayMapping() throws EngineException
	{
		AttributeDefinitionWithMapping complexAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("name").withMultiValued(true)
						.withType(SCIMAttributeType.COMPLEX)
						.withSubAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
								.withAttributeDefinition(AttributeDefinition.builder().withName("familyName")
										.withMultiValued(false).withType(SCIMAttributeType.STRING).build())
								.withAttributeMapping(SimpleAttributeMapping

										.builder()
										.withDataValue(DataValue.builder().withType(DataValueType.ARRAY)
												.build())
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
								DataValue.builder().withType(DataValueType.ARRAY).build())
						.withDataArray(Optional.empty()).build())
				.build()), any(), eq(mappingEvaluatorRegistry))).then(i -> EvaluationResult.builder().withAttributeName("familyName")
						.withValue(Optional.of(i.getArgument(1, EvaluatorContext.class).arrayObj)).build());

		EvaluationResult value = evaluator.eval(complexAttr, EvaluatorContext.builder().build(),
				mappingEvaluatorRegistry);

		assertThat(value.attributeName).isEqualTo("name");
		assertThat(value.value.get()).isEqualTo(List.of(Map.of("familyName", "f1"), Map.of("familyName", "f2")));
	}

}
