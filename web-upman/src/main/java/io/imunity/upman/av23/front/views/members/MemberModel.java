/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.members;

import com.vaadin.flow.component.icon.VaadinIcon;
import org.apache.commons.lang3.tuple.Pair;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;

import java.util.Map;
import java.util.Objects;

public class MemberModel
{
	public final long entityId;
	public final String name;
	public final Map<String, String> attributes;
	public final Pair<String, VaadinIcon> role;
	public final Pair<String, VaadinIcon> email;

	private MemberModel(long entityId, String name, Map<String, String> attributes, Pair<String, VaadinIcon> role, Pair<String, VaadinIcon> email)
	{
		this.entityId = entityId;
		this.name = name;
		this.attributes = attributes;
		this.role = role;
		this.email = email;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MemberModel that = (MemberModel) o;
		return entityId == that.entityId && Objects.equals(name, that.name) && Objects.equals(attributes, that.attributes) && Objects.equals(role, that.role) && Objects.equals(email, that.email);
	}

	public boolean anyFieldContains(String value) {
		String lowerCaseValue = value.toLowerCase();
		return value.isEmpty()
				|| name.toLowerCase().contains(lowerCaseValue)
				|| role.getKey().toLowerCase().contains(lowerCaseValue)
				|| email.getKey().toLowerCase().contains(lowerCaseValue)
				|| attributes.values().stream().anyMatch(attrValue -> attrValue.toLowerCase().contains(lowerCaseValue));
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(entityId, name, attributes, role, email);
	}

	public static MembersGridModelBuilder builder()
	{
		return new MembersGridModelBuilder();
	}

	public static final class MembersGridModelBuilder
	{
		public long entityId;
		public String name;
		public Map<String, String> attributes;
		public Pair<String, VaadinIcon> role;
		public Pair<String, VaadinIcon> email;

		private MembersGridModelBuilder()
		{
		}

		public MembersGridModelBuilder entityId(long entityId)
		{
			this.entityId = entityId;
			return this;
		}

		public MembersGridModelBuilder name(String name)
		{
			this.name = name;
			return this;
		}

		public MembersGridModelBuilder attributes(Map<String, String> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public MembersGridModelBuilder role(GroupAuthorizationRole role)
		{
			VaadinIcon icon = null;
			if (role.equals(GroupAuthorizationRole.manager))
				icon = VaadinIcon.STAR_O;
			if (role.equals(GroupAuthorizationRole.projectsAdmin))
				icon = VaadinIcon.STAR;

			this.role = Pair.of(role.name(), icon);
			return this;
		}

		public MembersGridModelBuilder email(VerifiableElementBase email)
		{
			this.email = Pair.of(email.getValue(), email.getConfirmationInfo().isConfirmed() ? VaadinIcon.CHECK_CIRCLE_O : VaadinIcon.EXCLAMATION_CIRCLE_O);
			return this;
		}

		public MemberModel build()
		{
			return new MemberModel(entityId, name, attributes, role, email);
		}
	}
}
