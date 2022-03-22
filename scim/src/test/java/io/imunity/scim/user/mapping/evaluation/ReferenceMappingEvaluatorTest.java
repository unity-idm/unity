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

import java.net.URI;
import java.util.Collections;
import java.util.List;
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
import io.imunity.scim.config.ReferenceAttributeMapping;
import io.imunity.scim.config.ReferenceAttributeMapping.ReferenceType;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.schema.SCIMAttributeType;
import pl.edu.icm.unity.exceptions.EngineException;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceMappingEvaluatorTest
{

	private ReferenceMappingEvaluator evaluator;

	@Mock
	private DataArrayResolver dataArrayResolver;

	@Mock
	private MVELEvaluator mvelEvaluator;

	@Mock
	private MappingEvaluatorRegistry mappingEvaluatorRegistry;

	@Before
	public void init()
	{
		evaluator = new ReferenceMappingEvaluator(mvelEvaluator, dataArrayResolver);
	}

	@Test
	public void shouldAddAttrObjToContextWhenEvalReferenceMulti() throws EngineException
	{
		AttributeDefinitionWithMapping refAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("link").withMultiValued(true)
						.withType(SCIMAttributeType.REFERENCE).build())
				.withAttributeMapping(ReferenceAttributeMapping.builder().withType(ReferenceType.GENERIC)
						.withExpression("arrayObj")
						.withDataArray(Optional.of(
								DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()))
						.build())
				.build();

		doReturn(List.of("f1")).when(dataArrayResolver).resolve(
				eq(DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("familyName").build()), any());
		evaluator.eval(refAttr, EvaluatorContext.builder().build(), mappingEvaluatorRegistry);
		ArgumentCaptor<EvaluatorContext> contextCaptor = ArgumentCaptor.forClass(EvaluatorContext.class);
		verify(mvelEvaluator).evalMVEL(eq("arrayObj"), contextCaptor.capture());
		assertThat(contextCaptor.getAllValues().get(0).arrayObj, is("f1"));
	}

	@Test
	public void shouldEvalToGroupReference() throws EngineException
	{
		AttributeDefinitionWithMapping refAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("link").withMultiValued(false)
						.withType(SCIMAttributeType.REFERENCE).build())
				.withAttributeMapping(ReferenceAttributeMapping.builder().withType(ReferenceType.GROUP)
						.withExpression("attrObj['x']").withDataArray(Optional.empty()).build())
				.build();

		when(mvelEvaluator.evalMVEL(eq("attrObj['x']"), any())).thenReturn("g1");

		EvaluationResult eval = evaluator.eval(refAttr,
				EvaluatorContext.builder()
						.withScimEndpointDescription(new SCIMEndpointDescription(URI.create("https://localhost"), null,
								Collections.emptyList(), Collections.emptyList()))
						.build(),
				mappingEvaluatorRegistry);

		assertThat(eval.attributeName, is("link"));
		assertThat(eval.value.get().toString(), is("https://localhost/Groups/g1"));

	}

	@Test
	public void shouldEvalToUserReference() throws EngineException
	{
		AttributeDefinitionWithMapping refAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("link").withMultiValued(false)
						.withType(SCIMAttributeType.REFERENCE).build())
				.withAttributeMapping(ReferenceAttributeMapping.builder().withType(ReferenceType.USER)
						.withExpression("attrObj['x']").withDataArray(Optional.empty()).build())
				.build();

		when(mvelEvaluator.evalMVEL(eq("attrObj['x']"), any())).thenReturn("u1");

		EvaluationResult eval = evaluator.eval(refAttr,
				EvaluatorContext.builder()
						.withScimEndpointDescription(new SCIMEndpointDescription(URI.create("https://localhost"), null,
								Collections.emptyList(), Collections.emptyList()))
						.build(),
				mappingEvaluatorRegistry);

		assertThat(eval.attributeName, is("link"));
		assertThat(eval.value.get().toString(), is("https://localhost/Users/u1"));
	}

	@Test
	public void shouldEvalToGenericReference() throws EngineException
	{
		AttributeDefinitionWithMapping refAttr = AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withName("link").withMultiValued(false)
						.withType(SCIMAttributeType.REFERENCE).build())
				.withAttributeMapping(ReferenceAttributeMapping.builder().withType(ReferenceType.GENERIC)
						.withExpression("attrObj['x']").withDataArray(Optional.empty()).build())
				.build();
		when(mvelEvaluator.evalMVEL(eq("attrObj['x']"), any())).thenReturn("linkValue");

		EvaluationResult eval = evaluator.eval(refAttr, EvaluatorContext.builder().build(), mappingEvaluatorRegistry);

		assertThat(eval.attributeName, is("link"));
		assertThat(eval.value.get().toString(), is("linkValue"));
	}

}
