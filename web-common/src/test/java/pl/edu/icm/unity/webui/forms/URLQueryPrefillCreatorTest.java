/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.Test;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.entity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.URLQueryPrefillConfig;
import pl.edu.icm.unity.base.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;

public class URLQueryPrefillCreatorTest
{
	@Test
	public void shouldPrefillGivenAttributesAsDefault() throws IllegalAttributeValueException
	{
		shouldPrefillGivenAttributesWithMode(PrefilledEntryMode.DEFAULT);
	}

	@Test
	public void shouldPrefillGivenAttributesAsHidden() throws IllegalAttributeValueException
	{
		shouldPrefillGivenAttributesWithMode(PrefilledEntryMode.HIDDEN);
	}
	
	@Test
	public void shouldPrefillGivenAttributesAsRO() throws IllegalAttributeValueException
	{
		shouldPrefillGivenAttributesWithMode(PrefilledEntryMode.READ_ONLY);
	}

	private void shouldPrefillGivenAttributesWithMode(PrefilledEntryMode mode) throws IllegalAttributeValueException
	{
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("form")
				.withDefaultCredentialRequirement("cr")
				.withAddedAttributeParam()
					.withAttributeType("attr1")
					.withGroup("/A")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withURLQueryPrefill(new URLQueryPrefillConfig("param1", mode))
				.endAttributeParam()
				.build();
		Attribute attr1 = new Attribute("attr1", "syntax", "/A", newArrayList("parsedval1"));
		
		ExternalDataParser externalDataParser = mock(ExternalDataParser.class);
		when(externalDataParser.parseAsAttribute(eq("attr1"), eq("/A"), eq(newArrayList("value1")))).thenReturn(attr1);
		
		@SuppressWarnings("unchecked")
		Function<String, String> urlParamAccessor = mock(Function.class);
		when(urlParamAccessor.apply(eq("param1"))).thenReturn("value1");
		
		URLQueryPrefillCreator prefilCreator = new URLQueryPrefillCreator(externalDataParser, urlParamAccessor);
		
		PrefilledSet prefilledSet = prefilCreator.create(form);
		
		assertThat(prefilledSet.attributes.get(0)).isNotNull();
		assertThat(prefilledSet.attributes.get(0).getMode()).isEqualTo(mode);
		assertThat(prefilledSet.attributes.get(0).getEntry()).isEqualTo(attr1);
	}
	
	@Test
	public void shouldPrefillGivenIdentitiesAsDefault() throws IllegalIdentityValueException
	{
		shouldPrefillGivenIdentitiesWithMode(PrefilledEntryMode.DEFAULT);
	}

	@Test
	public void shouldPrefillGivenIdentitiesAsHidden() throws IllegalIdentityValueException
	{
		shouldPrefillGivenIdentitiesWithMode(PrefilledEntryMode.HIDDEN);
	}

	@Test
	public void shouldPrefillGivenIdentitiesAsRO() throws IllegalIdentityValueException
	{
		shouldPrefillGivenIdentitiesWithMode(PrefilledEntryMode.READ_ONLY);
	}

	private void shouldPrefillGivenIdentitiesWithMode(PrefilledEntryMode mode) throws IllegalIdentityValueException
	{
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("form")
				.withDefaultCredentialRequirement("cr")
				.withAddedIdentityParam()
					.withIdentityType("id1")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withURLQueryPrefill(new URLQueryPrefillConfig("param1", mode))
				.endIdentityParam()
				.build();
		IdentityParam id1 = new IdentityParam("identity", "parsedval1");
		
		ExternalDataParser externalDataParser = mock(ExternalDataParser.class);
		when(externalDataParser.parseAsIdentity(eq("id1"), eq("value1"))).thenReturn(id1);
		
		@SuppressWarnings("unchecked")
		Function<String, String> urlParamAccessor = mock(Function.class);
		when(urlParamAccessor.apply(eq("param1"))).thenReturn("value1");
		
		URLQueryPrefillCreator prefilCreator = new URLQueryPrefillCreator(externalDataParser, urlParamAccessor);
		
		PrefilledSet prefilledSet = prefilCreator.create(form);
		
		assertThat(prefilledSet.identities.get(0)).isNotNull();
		assertThat(prefilledSet.identities.get(0).getMode()).isEqualTo(mode);
		assertThat(prefilledSet.identities.get(0).getEntry()).isEqualTo(id1);
	}

	
	@Test
	public void shouldNotPrefillNotGivenAttribute()
	{
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("form")
				.withDefaultCredentialRequirement("cr")
				.withAddedAttributeParam()
					.withAttributeType("attr1")
					.withGroup("/A")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withURLQueryPrefill(new URLQueryPrefillConfig("param1", PrefilledEntryMode.DEFAULT))
				.endAttributeParam()
				.build();
		
		ExternalDataParser externalDataParser = mock(ExternalDataParser.class);
		@SuppressWarnings("unchecked")
		Function<String, String> urlParamAccessor = mock(Function.class);
		
		URLQueryPrefillCreator prefilCreator = new URLQueryPrefillCreator(externalDataParser, urlParamAccessor);
		
		PrefilledSet prefilledSet = prefilCreator.create(form);
		
		assertThat(prefilledSet.attributes.get(0)).isNull();
	}

	@Test
	public void shouldNotPrefillNotGivenIdentity()
	{
		RegistrationForm form = new RegistrationFormBuilder()
				.withName("form")
				.withDefaultCredentialRequirement("cr")
				.withAddedIdentityParam()
					.withIdentityType("id1")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withURLQueryPrefill(new URLQueryPrefillConfig("param1", PrefilledEntryMode.DEFAULT))
				.endIdentityParam()
				.build();
		
		ExternalDataParser externalDataParser = mock(ExternalDataParser.class);
		@SuppressWarnings("unchecked")
		Function<String, String> urlParamAccessor = mock(Function.class);
		
		URLQueryPrefillCreator prefilCreator = new URLQueryPrefillCreator(externalDataParser, urlParamAccessor);
		
		PrefilledSet prefilledSet = prefilCreator.create(form);
		
		assertThat(prefilledSet.identities.get(0)).isNull();
	}
}
