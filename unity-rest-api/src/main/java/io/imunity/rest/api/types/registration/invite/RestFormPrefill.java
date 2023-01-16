/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.invite;

import java.util.Collections;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.imunity.rest.api.types.basic.RestAttribute;
import io.imunity.rest.api.types.basic.RestIdentityParam;
import io.imunity.rest.api.types.registration.RestGroupSelection;

@JsonDeserialize(builder = RestFormPrefill.Builder.class)
public class RestFormPrefill
{
	public final String formId;
	public final Map<Integer, RestPrefilledEntry<RestIdentityParam>> identities;
	public final Map<Integer, RestPrefilledEntry<RestGroupSelection>> groupSelections;
	public final Map<Integer, RestGroupSelection> allowedGroups;
	public final Map<Integer, RestPrefilledEntry<RestAttribute>> attributes;
	public final Map<String, String> messageParams;

	private RestFormPrefill(Builder builder)
	{
		this.formId = builder.formId;
		this.identities = builder.identities;
		this.groupSelections = builder.groupSelections;
		this.allowedGroups = builder.allowedGroups;
		this.attributes = builder.attributes;
		this.messageParams = builder.messageParams;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(allowedGroups, attributes, formId, groupSelections, identities, messageParams);
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
		RestFormPrefill other = (RestFormPrefill) obj;
		return Objects.equals(allowedGroups, other.allowedGroups) && Objects.equals(attributes, other.attributes)
				&& Objects.equals(formId, other.formId)
				&& Objects.equals(groupSelections, other.groupSelections)
				&& Objects.equals(identities, other.identities) && Objects.equals(messageParams, other.messageParams);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String formId;
		private Map<Integer, RestPrefilledEntry<RestIdentityParam>> identities = Collections.emptyMap();
		private Map<Integer, RestPrefilledEntry<RestGroupSelection>> groupSelections = Collections.emptyMap();
		private Map<Integer, RestGroupSelection> allowedGroups = Collections.emptyMap();
		private Map<Integer, RestPrefilledEntry<RestAttribute>> attributes = Collections.emptyMap();
		private Map<String, String> messageParams = Collections.emptyMap();

		private Builder()
		{
		}

		public Builder withFormId(String formId)
		{
			this.formId = formId;
			return this;
		}

		public Builder withIdentities(Map<Integer, RestPrefilledEntry<RestIdentityParam>> identities)
		{
			this.identities = identities;
			return this;
		}

		public Builder withGroupSelections(Map<Integer, RestPrefilledEntry<RestGroupSelection>> groupSelections)
		{
			this.groupSelections = groupSelections;
			return this;
		}

		public Builder withAllowedGroups(Map<Integer, RestGroupSelection> allowedGroups)
		{
			this.allowedGroups = allowedGroups;
			return this;
		}

		public Builder withAttributes(Map<Integer, RestPrefilledEntry<RestAttribute>> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public Builder withMessageParams(Map<String, String> messageParams)
		{
			this.messageParams = messageParams;
			return this;
		}

		public RestFormPrefill build()
		{
			return new RestFormPrefill(this);
		}
	}

}
