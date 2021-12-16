/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.summary;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.imunity.attr.introspection.config.Attribute;

class PolicyProcessingResult
{
	final ResolvedPolicy policy;
	final List<ReceivedAttribute> allReceivedAttributes;
	final List<Attribute> missingOptional;
	final List<Attribute> missingMandatory;

	PolicyProcessingResult(ResolvedPolicy policy, List<ReceivedAttribute> allReceivedAttributes,
			List<Attribute> missingOptional, List<Attribute> missingMandatory)
	{
		this.policy = policy;
		this.allReceivedAttributes = allReceivedAttributes != null ? Collections.unmodifiableList(allReceivedAttributes)
				: null;
		this.missingOptional = missingOptional != null ? Collections.unmodifiableList(missingOptional) : null;
		this.missingMandatory = missingMandatory != null ? Collections.unmodifiableList(missingMandatory) : null;

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(allReceivedAttributes, missingMandatory, missingOptional, policy);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PolicyProcessingResult other = (PolicyProcessingResult) obj;
		return Objects.equals(allReceivedAttributes, other.allReceivedAttributes)
				&& Objects.equals(missingMandatory, other.missingMandatory)
				&& Objects.equals(missingOptional, other.missingOptional) && Objects.equals(policy, other.policy);
	}

}