/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;

public class AttrIntrospectionAttributePoliciesConfiguration
{
	private List<Attribute> defaultPolicyAttributes;
	private List<AttributePolicy> customPolicies;

	public AttrIntrospectionAttributePoliciesConfiguration()
	{
		this(new ArrayList<>(), new ArrayList<>());
	}

	public AttrIntrospectionAttributePoliciesConfiguration(List<Attribute> defaultPolicyAttributes,
			List<AttributePolicy> customPolicies)
	{
		this.defaultPolicyAttributes = defaultPolicyAttributes;
		this.customPolicies = customPolicies;
	}

	public String toProperties(MessageSource msg) throws JsonProcessingException
	{
		Properties raw = new Properties();

		if (defaultPolicyAttributes != null)
		{
			putAttributesToProperties(raw, defaultPolicyAttributes, AttrIntrospectionEndpointProperties.PREFIX
					+ AttrIntrospectionEndpointProperties.DEFAULT_POLICY_ATTRIBUTES);
		}

		if (customPolicies != null)
		{
			for (AttributePolicy policy : customPolicies)
			{
				String key = AttrIntrospectionEndpointProperties.CUSTOM_ATTRIBUTE_POLICIES
						+ (customPolicies.indexOf(policy) + 1) + ".";
				raw.put(AttrIntrospectionEndpointProperties.PREFIX + key
						+ AttrIntrospectionEndpointProperties.ATTRIBUTE_POLICY_NAME, policy.name);
				raw.put(AttrIntrospectionEndpointProperties.PREFIX + key
						+ AttrIntrospectionEndpointProperties.ATTRIBUTE_POLICY_TARGET_IDPS,
						String.join(";", policy.targetIdps));
				raw.put(AttrIntrospectionEndpointProperties.PREFIX + key
						+ AttrIntrospectionEndpointProperties.ATTRIBUTE_POLICY_TARGET_FEDERATIONS,
						String.join(";", policy.targetFederations));

				List<Attribute> attributes = policy.attributes;
				if (attributes != null)
				{
					putAttributesToProperties(raw, attributes, AttrIntrospectionEndpointProperties.PREFIX + key
							+ AttrIntrospectionEndpointProperties.ATTRIBUTE_POLICY_ATTRIBUTES);
				}
			}
		}

		AttrIntrospectionEndpointProperties attrIntroProperties = new AttrIntrospectionEndpointProperties(raw);
		return attrIntroProperties.getAsString();

	}

	private void putAttributesToProperties(Properties properties, List<Attribute> attributes, String key)
			throws JsonProcessingException
	{
		for (Attribute attr : attributes)
		{
			String attrkey = key + (attributes.indexOf(attr) + 1);
			properties.put(attrkey, Constants.MAPPER.writeValueAsString(attr));
		}
	}

	public void fromProperties(String vaadinProperties, MessageSource msg)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(vaadinProperties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the attribute introspection service", e);
		}

		AttrIntrospectionEndpointProperties vProperties = new AttrIntrospectionEndpointProperties(raw);
		fromProperties(vProperties, msg);
	}

	public void fromProperties(AttrIntrospectionEndpointProperties vProperties, MessageSource msg)
	{

		Set<String> policiesKeys = vProperties
				.getStructuredListKeys(AttrIntrospectionEndpointProperties.CUSTOM_ATTRIBUTE_POLICIES);

		defaultPolicyAttributes = vProperties
				.getListOfValues(AttrIntrospectionEndpointProperties.DEFAULT_POLICY_ATTRIBUTES).stream()
				.map(a -> getAttributeFromString(a)).collect(Collectors.toList());

		for (String policyKey : policiesKeys)
		{
			String name = vProperties.getValue(policyKey + AttrIntrospectionEndpointProperties.ATTRIBUTE_POLICY_NAME);
			String targetIdpsValue = vProperties
					.getValue(policyKey + AttrIntrospectionEndpointProperties.ATTRIBUTE_POLICY_TARGET_IDPS);
			String targetFederationsValue = vProperties
					.getValue(policyKey + AttrIntrospectionEndpointProperties.ATTRIBUTE_POLICY_TARGET_FEDERATIONS);

			List<String> targetIdps = new ArrayList<>();
			List<String> targetFederations = new ArrayList<>();

			if (targetIdpsValue != null && !targetIdpsValue.isEmpty())
			{
				CollectionUtils.addAll(targetIdps, targetIdpsValue.split(";"));
			}

			if (targetFederationsValue != null && !targetFederationsValue.isEmpty())
			{
				CollectionUtils.addAll(targetFederations, targetFederationsValue.split(";"));
			}

			List<Attribute> attributes = new ArrayList<>();
			for (String attr : vProperties
					.getListOfValues(policyKey + AttrIntrospectionEndpointProperties.ATTRIBUTE_POLICY_ATTRIBUTES))
			{
				attributes.add(getAttributeFromString(attr));
			}

			customPolicies.add(new AttributePolicy(name, attributes, targetIdps, targetFederations));

		}

	}

	private Attribute getAttributeFromString(String attr)
	{
		try
		{
			return Constants.MAPPER.readValue(attr, Attribute.class);
		} catch (IOException e)
		{
			throw new InternalException("Can't deserialize attribute from JSON", e);
		}
	}

	public List<AttributePolicy> getCustomPolicies()
	{
		return customPolicies;
	}

	public void setCustomPolicies(List<AttributePolicy> policies)
	{
		this.customPolicies = policies;
	}

	public List<Attribute> getDefaultPolicyAttributes()
	{
		return defaultPolicyAttributes;
	}

	public void setDefaultPolicyAttributes(List<Attribute> defaultPolicy)
	{
		this.defaultPolicyAttributes = defaultPolicy;
	}

}
