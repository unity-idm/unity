/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.summary;

import io.imunity.attr.introspection.config.AttrIntrospectionAttributePoliciesConfiguration;
import io.imunity.attr.introspection.config.Attribute;
import io.imunity.attr.introspection.config.AttributePolicy;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.exceptions.EngineException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class AttributePolicyProcessor
{
	private AttrIntrospectionAttributePoliciesConfiguration configuration;
	private IdpGroupResolver groupResolver;

	AttributePolicyProcessor(AttrIntrospectionAttributePoliciesConfiguration configuration,
			AuthenticatorSupportService authenticatorSupportService)
	{
		this.configuration = configuration;
		this.groupResolver = new IdpGroupResolver(authenticatorSupportService);
	}

	AttributePolicyProcessor(AttrIntrospectionAttributePoliciesConfiguration configuration,
			IdpGroupResolver idpGroupResolver)
	{
		this.configuration = configuration;
		this.groupResolver = idpGroupResolver;
	}

	PolicyProcessingResult applyPolicyForRemoteUser(RemotelyAuthenticatedPrincipal remotelyAuthenticatedPrincipal) throws EngineException
	{
		ResolvedPolicy policy = resolvePolicyFallbackToDefault(
				remotelyAuthenticatedPrincipal.getAuthnInput().getIdpName());
		List<ReceivedAttribute> allReceivedAttributes = remotelyAuthenticatedPrincipal.getAuthnInput().getAttributes()
				.values().stream()
				.map(a -> new ReceivedAttribute(a.getName(), policy.attributes.stream()
						.filter(at -> at.name.equals(a.getName())).map(at -> at.description).findFirst(),
						a.getValues()))
				.collect(Collectors.toList());
		List<String> receivedNames = allReceivedAttributes.stream().map(a -> a.name).toList();
		List<Attribute> missingMandatory = policy.attributes.stream()
				.filter(a -> !receivedNames.contains(a.name) && a.mandatory).collect(Collectors.toList());
		List<Attribute> missingOptional = policy.attributes.stream()
				.filter(a -> !receivedNames.contains(a.name) && !a.mandatory).collect(Collectors.toList());

		return new PolicyProcessingResult(policy, allReceivedAttributes, missingOptional, missingMandatory);
	}

	private ResolvedPolicy resolvePolicyFallbackToDefault(String idp) throws EngineException
	{
		if (idp == null || idp.isEmpty())
		{
			return getDefaultPolicy();
		}

		Optional<AttributePolicy> byIdp = findByIdp(idp);
		if (!byIdp.isEmpty())
		{
			return mapToResolvedPolicy(byIdp.get());
		}

		Optional<String> group = groupResolver.resolveGroupForIdp(idp);
		if (!group.isEmpty())
		{
			Optional<AttributePolicy> byGroup = findByGroup(group.get());
			if (!byGroup.isEmpty())
			{
				return mapToResolvedPolicy(byGroup.get());
			}
		}
		return getDefaultPolicy();
	}

	private ResolvedPolicy mapToResolvedPolicy(AttributePolicy policy)
	{
		return new ResolvedPolicy(Optional.of(policy.name), policy.attributes);
	}

	private ResolvedPolicy getDefaultPolicy()
	{
		return new ResolvedPolicy(Optional.empty(), configuration.getDefaultPolicyAttributes());
	}

	private Optional<AttributePolicy> findByGroup(String group)
	{
		return configuration.getCustomPolicies().stream().filter(cp -> cp.targetFederations.contains(group))
				.findFirst();
	}

	Optional<AttributePolicy> findByIdp(String idp)
	{
		return configuration.getCustomPolicies().stream().filter(cp -> cp.targetIdps.contains(idp)).findFirst();
	}

}
