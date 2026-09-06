/*
 * Copyright (c) 2026 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration.invite;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pl.edu.icm.unity.base.registration.invitation.ComboInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;

public class InvitationCloningBuilderTest
{
	private static final Instant ORIGINAL_EXPIRATION = Instant.parse("2026-09-05T12:00:00Z");
	private static final Instant NEW_EXPIRATION = Instant.parse("2026-09-08T12:00:00Z");

	@ParameterizedTest(name = "{0}")
	@MethodSource("cloningCases")
	public void shouldCreateIndependentCopyWithNewExpiration(CloningCase testCase)
	{
		InvitationParam original = testCase.invitation;

		InvitationParam copy = copyWithExpiration(original, NEW_EXPIRATION);

		assertThat(copy).isNotSameAs(original);
		assertThat(copy.getExpiration()).isEqualTo(NEW_EXPIRATION);
		assertThat(original.getExpiration()).isEqualTo(ORIGINAL_EXPIRATION);
		assertThat(copy.getType()).isEqualTo(original.getType());
		assertThat(copy.getContactAddress()).isEqualTo(original.getContactAddress());
		assertThat(copy.getFormsPrefillData()).isEqualTo(original.getFormsPrefillData());
	}

	private static Stream<CloningCase> cloningCases()
	{
		return Stream.of(
				CloningCase.builder()
						.withName("registration invitation")
						.withInvitation(RegistrationInvitationParam.builder()
								.withForm("registrationForm")
								.withContactAddress("registration@example.com")
								.withExpiration(ORIGINAL_EXPIRATION)
								.build())
						.build(),
				CloningCase.builder()
						.withName("enquiry invitation")
						.withInvitation(EnquiryInvitationParam.builder()
								.withForm("enquiryForm")
								.withEntity(1L)
								.withContactAddress("enquiry@example.com")
								.withExpiration(ORIGINAL_EXPIRATION)
								.build())
						.build(),
				CloningCase.builder()
						.withName("combo invitation")
						.withInvitation(new ComboInvitationParam("registrationForm", "enquiryForm",
								ORIGINAL_EXPIRATION, "combo@example.com"))
						.build());
	}

	private static InvitationParam copyWithExpiration(InvitationParam invitation, Instant expiration)
	{
		return switch (invitation.getType())
		{
		case REGISTRATION -> ((RegistrationInvitationParam) invitation).cloningBuilder()
				.withExpiration(expiration).build();
		case ENQUIRY -> ((EnquiryInvitationParam) invitation).cloningBuilder()
				.withExpiration(expiration).build();
		case COMBO -> ((ComboInvitationParam) invitation).cloningBuilder()
				.withExpiration(expiration).build();
		};
	}

	private record CloningCase(String name, InvitationParam invitation)
	{
		private static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return name;
		}

		private static class Builder
		{
			private String name;
			private InvitationParam invitation;

			private Builder withName(String name)
			{
				this.name = name;
				return this;
			}

			private Builder withInvitation(InvitationParam invitation)
			{
				this.invitation = invitation;
				return this;
			}

			private CloningCase build()
			{
				return new CloningCase(name, invitation);
			}
		}
	}
}
