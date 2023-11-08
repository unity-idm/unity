/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.Set;
import java.util.Collections;
import java.util.Optional;

public class ProjectAddInvitationResult
{
	public final Set<String> projectAlreadyMemberEmails;

	private ProjectAddInvitationResult(Builder builder)
	{
		this.projectAlreadyMemberEmails = Optional.ofNullable(builder.projectAlreadyMemberEmails)
				.map(Set::copyOf)
				.orElse(Set.of());
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Set<String> projectAlreadyMemberEmails = Collections.emptySet();

		private Builder()
		{
		}

		public Builder withProjectAlreadyMemberEmails(Set<String> projectAlreadyMemberEmails)
		{
			this.projectAlreadyMemberEmails = projectAlreadyMemberEmails;
			return this;
		}

		public ProjectAddInvitationResult build()
		{
			return new ProjectAddInvitationResult(this);
		}
	}
	
	
}
