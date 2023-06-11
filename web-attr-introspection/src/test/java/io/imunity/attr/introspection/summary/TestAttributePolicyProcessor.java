/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.summary;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import io.imunity.attr.introspection.config.AttributePolicy;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;

@RunWith(MockitoJUnitRunner.class)
public class TestAttributePolicyProcessor
{

	@Mock
	private IdpGroupResolver idpGroupResolver;

	@Test
	public void shouldApplyFirstPolicyByIdp() throws EngineException
	{
		AttributePolicyProcessor processor = new AttributePolicyProcessor(getConfig(), idpGroupResolver);
		PolicyProcessingResult result = processor.applyPolicyForRemoteUser(getRemotelyAuthenticatedInput("idp"));
		assertThat(result.policy.name.get(), is("Policy2"));
	}

	@Test
	public void shouldApplyPolicyByGroup() throws EngineException
	{
		AttributePolicyProcessor processor = new AttributePolicyProcessor(getConfig(), idpGroupResolver);
		when(idpGroupResolver.resolveGroupForIdp(eq("idp1"))).thenReturn(Optional.of("federation2"));
		PolicyProcessingResult result = processor.applyPolicyForRemoteUser(getRemotelyAuthenticatedInput("idp1"));
		assertThat(result.policy.name.get(), is("Policy3"));
	}

	@Test
	public void shouldReturnMissingMandatoryAttributes() throws EngineException
	{
		AttributePolicyProcessor processor = new AttributePolicyProcessor(getConfig(), idpGroupResolver);
		PolicyProcessingResult result = processor.applyPolicyForRemoteUser(getRemotelyAuthenticatedInput("idp5"));
		assertThat(result.policy.name.get(), is("Policy5"));
		assertThat(result.missingMandatory.size(), is(2));
		assertThat(result.missingMandatory.get(0).name, is("manAttr2"));
		assertThat(result.missingMandatory.get(1).name, is("manAttr3"));
	}

	@Test
	public void shouldReturnMissingOptionalAttributes() throws EngineException
	{
		AttributePolicyProcessor processor = new AttributePolicyProcessor(getConfig(), idpGroupResolver);
		PolicyProcessingResult result = processor.applyPolicyForRemoteUser(getRemotelyAuthenticatedInput("idp6"));
		assertThat(result.policy.name.get(), is("Policy6"));
		assertThat(result.missingOptional.size(), is(2));
		assertThat(result.missingOptional.get(0).name, is("optAttr1"));
		assertThat(result.missingOptional.get(1).name, is("optAttr2"));
	}

	@Test
	public void shouldApplyPolicyByIdpFirst() throws EngineException
	{
		AttributePolicyProcessor processor = new AttributePolicyProcessor(getConfig(), idpGroupResolver);
		PolicyProcessingResult result = processor.applyPolicyForRemoteUser(getRemotelyAuthenticatedInput("idp2"));
		assertThat(result.policy.name.get(), is("Policy2"));
	}

	@Test
	public void shouldFallbackToDefaultPolicy() throws EngineException
	{
		AttributePolicyProcessor processor = new AttributePolicyProcessor(getConfig(), idpGroupResolver);
		PolicyProcessingResult result = processor.applyPolicyForRemoteUser(getRemotelyAuthenticatedInput("idpUnknown"));
		assertThat(result.policy.name.isEmpty(), is(true));
		assertThat(result.policy.attributes.get(0).name, is("defaultAttr1"));
		assertThat(result.policy.attributes.get(1).name, is("defaultAttr2"));
	}

	private RemotelyAuthenticatedPrincipal getRemotelyAuthenticatedInput(String idp)
	{
		RemotelyAuthenticatedPrincipal input = new RemotelyAuthenticatedPrincipal(idp, "profile");
		RemotelyAuthenticatedInput remotelyAuthenticatedInput = new RemotelyAuthenticatedInput(idp);
		remotelyAuthenticatedInput.addAttribute(new RemoteAttribute("attr1", List.of("v1")));
		remotelyAuthenticatedInput.addAttribute(new RemoteAttribute("attr2", List.of("v2")));
		remotelyAuthenticatedInput.addAttribute(new RemoteAttribute("manAttr1", List.of("v1")));
		input.setAuthnInput(remotelyAuthenticatedInput);
		return input;
	}

	private AttrIntrospectionAttributePoliciesConfiguration getConfig()
	{
		List<AttributePolicy> customPolicies = new ArrayList<>();
		customPolicies
				.add(new AttributePolicy("Policy1", Collections.emptyList(), List.of("idpSKip"), List.of("group")));
		customPolicies.add(new AttributePolicy("Policy2", Collections.emptyList(), List.of("idp2", "idp"),
				List.of("federation1")));
		customPolicies.add(new AttributePolicy("Policy3", Collections.emptyList(), List.of("idp3", "idp"),
				List.of("federation2")));
		customPolicies.add(new AttributePolicy("Policy5",
				List.of(getAttribute("manAttr1", true), getAttribute("manAttr2", true), getAttribute("manAttr3", true)),
				List.of("idp5"), Collections.emptyList()));
		customPolicies.add(new AttributePolicy("Policy6",
				List.of(getAttribute("optAttr1", false), getAttribute("optAttr2", false), getAttribute("attr1", false)),
				List.of("idp6"), Collections.emptyList()));
		AttrIntrospectionAttributePoliciesConfiguration config = new AttrIntrospectionAttributePoliciesConfiguration(
				List.of(getAttribute("defaultAttr1", true), getAttribute("defaultAttr2", true)), customPolicies);
		return config;
	}

	io.imunity.attr.introspection.config.Attribute getAttribute(String name, Boolean mandatory)
	{
		return new io.imunity.attr.introspection.config.Attribute(name, name, mandatory);
	}

}
