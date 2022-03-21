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
import io.imunity.scim.config.DataArray;
import io.imunity.scim.config.DataArray.DataArrayType;
import io.imunity.scim.config.DataValue;
import io.imunity.scim.config.DataValue.DataValueType;
import io.imunity.scim.config.SimpleAttributeMapping;
import io.imunity.scim.schema.SCIMAttributeType;
import pl.edu.icm.unity.exceptions.EngineException;

@RunWith(MockitoJUnitRunner.class)
public class SimpleMappingEvaluatorTest
{
	private SimpleMappingEvaluator evaluator;

	@Mock
	private DataArrayResolver dataArrayResolver;
	@Mock
	private UnityToSCIMDataConverter targetDataConverter;
	@Mock
	private MVELEvaluator mvelEvaluator;

	@Mock
	private MappingEvaluatorRegistry mappingEvaluatorRegistry;

	@Before
	public void init()
	{
		evaluator = new SimpleMappingEvaluator(dataArrayResolver, targetDataConverter, mvelEvaluator);
	}

	@Test
	public void shouldAddAttrObjToContextWhenEvalSimpleMulti() throws EngineException
	{
		AttributeDefinitionWithMapping simpleAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("familyName").withMultiValued(true)
						.withType(SCIMAttributeType.STRING).build())
				.withAttributeMapping(SimpleAttributeMapping.builder()
						.withDataValue(DataValue.builder().withType(DataValueType.MVEL).withValue("arrayObj").build())
						.withDataArray(Optional.of(
								DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()))
						.build())
				.build();

		doReturn(List.of("f1")).when(dataArrayResolver).resolve(
				eq(DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()), any());
		evaluator.eval(simpleAttr, EvaluatorContext.builder().build(), mappingEvaluatorRegistry);
		ArgumentCaptor<EvaluatorContext> contextCaptor = ArgumentCaptor.forClass(EvaluatorContext.class);
		verify(mvelEvaluator).evalMVEL(eq("arrayObj"), contextCaptor.capture());
		assertThat(contextCaptor.getAllValues().get(0).arrayObj, is("f1"));
	}

	@Test
	public void shouldEvalSimpleSingleAttributeWithMvelMapping() throws EngineException
	{
		AttributeDefinitionWithMapping simpleAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("familyName").withMultiValued(false)
						.withType(SCIMAttributeType.STRING).build())
				.withAttributeMapping(SimpleAttributeMapping.builder()
						.withDataValue(DataValue.builder().withType(DataValueType.MVEL).withValue("arrayObj").build())
						.withDataArray(Optional.empty()).build())
				.build();

		when(mvelEvaluator.evalMVEL(eq("arrayObj"), any())).thenReturn("attributeValue");
		Map<String, Object> eval = evaluator.eval(simpleAttr, EvaluatorContext.builder().build(),
				mappingEvaluatorRegistry);
		assertThat(eval.get("familyName"), is("attributeValue"));
	}

	@Test
	public void shouldEvalSimpleMultiAttributeWithMvelMappingAndAttributeDataArray() throws EngineException
	{
		AttributeDefinitionWithMapping simpleAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("familyName").withMultiValued(true)
						.withType(SCIMAttributeType.STRING).build())
				.withAttributeMapping(SimpleAttributeMapping.builder()
						.withDataValue(DataValue.builder().withType(DataValueType.MVEL).withValue("arrayObj").build())
						.withDataArray(Optional.of(
								DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()))
						.build())
				.build();

		doReturn(List.of("f1", "f2")).when(dataArrayResolver).resolve(
				eq(DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()), any());
		when(mvelEvaluator.evalMVEL(eq("arrayObj"), any())).thenReturn("attributeValue");
		Map<String, Object> eval = evaluator.eval(simpleAttr, EvaluatorContext.builder().build(),
				mappingEvaluatorRegistry);
		assertThat(eval.get("familyName"), is(List.of("attributeValue", "attributeValue")));
	}

	@Test
	public void shouldEvalSimpleMultiAttributeWithArrayValueMappingAndAttributeDataArray() throws EngineException
	{
		AttributeDefinitionWithMapping simpleAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("familyName").withMultiValued(true)
						.withType(SCIMAttributeType.STRING).build())
				.withAttributeMapping(SimpleAttributeMapping.builder()
						.withDataValue(DataValue.builder().withType(DataValueType.ARRAY).build())
						.withDataArray(Optional.of(
								DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()))
						.build())
				.build();

		doReturn(List.of("f1", "f2")).when(dataArrayResolver).resolve(
				eq(DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()), any());
		when(targetDataConverter.convertToType(eq("f1"), eq(SCIMAttributeType.STRING))).thenReturn("f1");
		when(targetDataConverter.convertToType(eq("f2"), eq(SCIMAttributeType.STRING))).thenReturn("f2");

		Map<String, Object> eval = evaluator.eval(simpleAttr, EvaluatorContext.builder().build(),
				mappingEvaluatorRegistry);
		assertThat(eval.get("familyName"), is(List.of("f1", "f2")));
	}

	@Test
	public void shouldEvalSimpleSingleAttributeWithDatatimeValueMappingAndAttributeDataValue() throws EngineException
	{
		AttributeDefinitionWithMapping simpleAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("time").withMultiValued(false)
						.withType(SCIMAttributeType.DATETIME).build())
				.withAttributeMapping(SimpleAttributeMapping.builder()
						.withDataValue(
								DataValue.builder().withType(DataValueType.ATTRIBUTE).withValue("timeAttr").build())
						.withDataArray(Optional.empty()).build())
				.build();

		when(targetDataConverter.convertUserAttributeToType(any(), eq("timeAttr"), eq(SCIMAttributeType.DATETIME)))
				.thenReturn(Optional.of("datetime"));

		Map<String, Object> eval = evaluator.eval(simpleAttr, EvaluatorContext.builder().build(),
				mappingEvaluatorRegistry);
		assertThat(eval.get("time"), is("datetime"));
	}

}
