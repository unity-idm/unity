/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.Arrays;

import org.junit.Test;

import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.ConfirmationMode;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.PrefilledEntryMode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

public class InvitationValidatorTest
{
	@Test
	public void shouldNotAcceptTooManyPrefilledAttributes()
	{
		InvitationParam invitation = getMinimalInvitationBuilder()
				.withAttribute(StringAttribute.of("attr", "/", Arrays.asList("value")), PrefilledEntryMode.HIDDEN)
				.build();
		
		RegistrationForm form = getMinimalRegFormBuilder()
				.build();
		
		Throwable exception = catchThrowable(() -> InvitationValidator.validate(invitation, form));

		assertThat(exception).isNotNull().isInstanceOf(IllegalArgumentException.class);	
	}

	@Test
	public void shouldNotAcceptPrefilledAttributeOfWrongType()
	{
		InvitationParam invitation = getMinimalInvitationBuilder()
				.withAttribute(StringAttribute.of("attr", "/", Arrays.asList("value")), PrefilledEntryMode.HIDDEN)
				.build();
		
		RegistrationForm form = getMinimalRegFormBuilder()
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.EMAIL_ATTR)
					.withGroup("/")
					.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
					.withConfirmationMode(ConfirmationMode.CONFIRMED)
				.endAttributeParam()
				.build();
		
		Throwable exception = catchThrowable(() -> InvitationValidator.validate(invitation, form));

		assertThat(exception).isNotNull().isInstanceOf(IllegalArgumentException.class);	
		
	}

	@Test
	public void shouldNotAcceptTooManyPrefilledIdentities()
	{
		InvitationParam invitation = getMinimalInvitationBuilder()
				.withIdentity(new IdentityParam(UsernameIdentity.ID, "wrong"), PrefilledEntryMode.READ_ONLY)
				.build();
		
		RegistrationForm form = getMinimalRegFormBuilder()
				.build();
		
		Throwable exception = catchThrowable(() -> InvitationValidator.validate(invitation, form));

		assertThat(exception).isNotNull().isInstanceOf(IllegalArgumentException.class);	

	}

	@Test
	public void shouldNotAcceptPrefilledIdentityOfWrongType()
	{
		InvitationParam invitation = getMinimalInvitationBuilder()
				.withIdentity(new IdentityParam(UsernameIdentity.ID, "wrong"), PrefilledEntryMode.READ_ONLY)
				.build();
		
		RegistrationForm form = getMinimalRegFormBuilder()
				.withAddedIdentityParam().withIdentityType(EmailIdentity.ID).endIdentityParam()
				.build();
		
		Throwable exception = catchThrowable(() -> InvitationValidator.validate(invitation, form));

		assertThat(exception).isNotNull().isInstanceOf(IllegalArgumentException.class);	

	}

	@Test
	public void shouldNotAcceptTooManyPrefilledGroups()
	{
		InvitationParam invitation = getMinimalInvitationBuilder()
				.withGroup("/some/group", PrefilledEntryMode.READ_ONLY)
				.build();
		
		RegistrationForm form = getMinimalRegFormBuilder()
				.build();
		
		Throwable exception = catchThrowable(() -> InvitationValidator.validate(invitation, form));

		assertThat(exception).isNotNull().isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void shouldNotAcceptMultiplePrefilledGroupsWhenOneIsAllowed()
	{
		InvitationParam invitation = getMinimalInvitationBuilder()
				.withGroups(Arrays.asList("/some/group", "/other"), PrefilledEntryMode.READ_ONLY)
				.build();
		
		RegistrationForm form = getMinimalRegFormBuilder()
				.withAddedGroupParam()
					.withGroupPath("/**")
					.withMultiselect(false)
				.endGroupParam()
				.build();
		
		Throwable exception = catchThrowable(() -> InvitationValidator.validate(invitation, form));

		assertThat(exception).isNotNull().isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void shouldNotAcceptPrefilledGroupNotMatchingWildcard()
	{
		InvitationParam invitation = getMinimalInvitationBuilder()
				.withGroup("/other", PrefilledEntryMode.READ_ONLY)
				.build();
		
		RegistrationForm form = getMinimalRegFormBuilder()
				.withAddedGroupParam()
					.withGroupPath("/prefix/**")
					.withMultiselect(false)
				.endGroupParam()
				.build();
		
		Throwable exception = catchThrowable(() -> InvitationValidator.validate(invitation, form));

		assertThat(exception).isNotNull().isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void shouldAcceptMatching()
	{
		InvitationParam invitation = getMinimalInvitationBuilder()
				.withGroup("/prefix/some", PrefilledEntryMode.READ_ONLY)
				.withIdentity(new IdentityParam(UsernameIdentity.ID, "good"), PrefilledEntryMode.READ_ONLY)
				.withAttribute(StringAttribute.of(InitializerCommon.EMAIL_ATTR, "/", Arrays.asList("value")), PrefilledEntryMode.HIDDEN)
				.build();
		
		RegistrationForm form = getMinimalRegFormBuilder()
				.withAddedGroupParam()
					.withGroupPath("/prefix/**")
					.withMultiselect(false)
				.endGroupParam()
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.EMAIL_ATTR)
					.withGroup("/")
					.withRetrievalSettings(ParameterRetrievalSettings.automaticOrInteractive)
					.withConfirmationMode(ConfirmationMode.CONFIRMED)
				.endAttributeParam()
				.withAddedIdentityParam().withIdentityType(UsernameIdentity.ID).endIdentityParam()
				.build();
		
		Throwable exception = catchThrowable(() -> InvitationValidator.validate(invitation, form));

		assertThat(exception).isNull();
	}

	
	private RegistrationFormBuilder getMinimalRegFormBuilder()
	{
		return new RegistrationFormBuilder()
				.withName("form")
				.withPubliclyAvailable(true)
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
	}
	
	private RegistrationInvitationParam.Builder getMinimalInvitationBuilder()
	{
		return RegistrationInvitationParam.builder()
				.withForm("form")
				.withExpiration(Instant.now().plusSeconds(1000));
	}
}
