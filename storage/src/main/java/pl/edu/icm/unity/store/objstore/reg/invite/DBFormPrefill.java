/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import pl.edu.icm.unity.store.impl.attribute.DBAttribute;
import pl.edu.icm.unity.store.objstore.reg.common.DBGroupSelection;
import pl.edu.icm.unity.store.objstore.reg.common.DBIdentityParam;



@JsonDeserialize(builder = DBFormPrefill.Builder.class)
public class DBFormPrefill
{
	public final String formId;
	public final Map<Integer, DBPrefilledEntry<DBIdentityParam>> identities;
	public final Map<Integer, DBPrefilledEntry<DBGroupSelection>> groupSelections;
	public final Map<Integer, DBGroupSelection> allowedGroups;
	public final Map<Integer, DBPrefilledEntry<DBAttribute>> attributes;
	public final Map<String, String> messageParams;

	private DBFormPrefill(Builder builder)
	{
		this.formId = builder.formId;
		this.identities = Optional.ofNullable(builder.identities)
				.map(HashMap::new)
				.map(Collections::unmodifiableMap)
				.orElse(null);
		this.groupSelections = Optional.ofNullable(builder.groupSelections)
				.map(HashMap::new)
				.map(Collections::unmodifiableMap)
				.orElse(null);
		this.allowedGroups = Optional.ofNullable(builder.allowedGroups)
				.map(HashMap::new)
				.map(Collections::unmodifiableMap)
				.orElse(null);
		this.attributes = Optional.ofNullable(builder.attributes)
				.map(HashMap::new)
				.map(Collections::unmodifiableMap)
				.orElse(null);
		this.messageParams = Optional.ofNullable(builder.messageParams)
				.map(HashMap::new)
				.map(Collections::unmodifiableMap)
				.orElse(null);
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
		DBFormPrefill other = (DBFormPrefill) obj;
		return Objects.equals(allowedGroups, other.allowedGroups) && Objects.equals(attributes, other.attributes)
				&& Objects.equals(formId, other.formId) && Objects.equals(groupSelections, other.groupSelections)
				&& Objects.equals(identities, other.identities) && Objects.equals(messageParams, other.messageParams);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String formId;
		private Map<Integer, DBPrefilledEntry<DBIdentityParam>> identities = Collections.emptyMap();
		private Map<Integer, DBPrefilledEntry<DBGroupSelection>> groupSelections = Collections.emptyMap();
		private Map<Integer, DBGroupSelection> allowedGroups = Collections.emptyMap();
		private Map<Integer, DBPrefilledEntry<DBAttribute>> attributes = Collections.emptyMap();
		private Map<String, String> messageParams = Collections.emptyMap();

		private Builder()
		{
		}

		public Builder withFormId(String formId)
		{
			this.formId = formId;
			return this;
		}

		public Builder withIdentity(Map<Integer, DBPrefilledEntry<DBIdentityParam>> identities)
		{
			this.identities = identities;
			return this;
		}

		public Builder withIdentity(DBIdentityParam identityParam, String mode)
		{
			int idx = identities.size();
			identities.put(idx, new DBPrefilledEntry.Builder<DBIdentityParam>().withEntry(identityParam)
					.withMode(mode)
					.build());
			return this;
		}

		public Builder withIdentities(Map<Integer, DBPrefilledEntry<DBIdentityParam>> identities)
		{
			this.identities = identities;
			return this;
		}

		public Builder withGroup(String group, String mode)
		{
			int idx = groupSelections.size();
			groupSelections.put(idx,
					new DBPrefilledEntry.Builder<DBGroupSelection>().withEntry(DBGroupSelection.builder()
							.withSelectedGroups(List.of(group))
							.build())
							.withMode(mode)
							.build());
			return this;
		}

		public Builder withGroup(List<String> groups, String mode)
		{
			int idx = groupSelections.size();
			groupSelections.put(idx,
					new DBPrefilledEntry.Builder<DBGroupSelection>().withEntry(DBGroupSelection.builder()
							.withSelectedGroups(groups)
							.build())
							.withMode(mode)
							.build());
			return this;
		}

		public Builder withGroupSelections(Map<Integer, DBPrefilledEntry<DBGroupSelection>> groupSelections)
		{
			this.groupSelections = groupSelections;
			return this;
		}

		public Builder withAllowedGroups(Map<Integer, DBGroupSelection> allowedGroups)
		{
			this.allowedGroups = allowedGroups;
			return this;
		}

		public Builder withAttribute(DBAttribute attribute, String mode)
		{
			int idx = attributes.size();
			attributes.put(idx, new DBPrefilledEntry.Builder<DBAttribute>().withEntry(attribute)
					.withMode(mode)
					.build());
			return this;
		}

		public Builder withAttributes(Map<Integer, DBPrefilledEntry<DBAttribute>> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public Builder withMessageParams(Map<String, String> messageParams)
		{
			this.messageParams = messageParams;
			return this;
		}

		public DBFormPrefill build()
		{
			return new DBFormPrefill(this);
		}
	}

}
