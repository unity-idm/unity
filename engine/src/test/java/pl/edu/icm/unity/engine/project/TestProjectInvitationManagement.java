/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.invitation.ComboInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.api.project.ProjectInvitation;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement.IllegalInvitationException;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement.NotProjectInvitation;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;

@ExtendWith(MockitoExtension.class)
public class TestProjectInvitationManagement extends TestProjectBase
{
	@Mock
	private PublicRegistrationURLSupport mockPublicRegistrationURLSupport;

	private ProjectInvitationsManagementImpl projectInvMan;

	@BeforeEach
	public void initProjectInvitationMan()
	{
		projectInvMan = new ProjectInvitationsManagementImpl(mockInvitationMan, mockGroupMan, mockRegistrationMan,
				mockEnquiryMan, mockIdMan, mockPublicRegistrationURLSupport, mockAuthz, mockMsg);
	}

	@Test
	public void shouldForwardToCoreManagerWithAllowedGroups() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		when(mockIdMan.getAllEntitiesWithContactEmails(Set.of("demo@demo.com")))
				.thenReturn(Sets.newHashSet(new EntityWithContactInfo(new Entity(null, new EntityInformation(1L), null), "demo@demo.com", Set.of("/"))));
		
		ProjectInvitationParam projectParam = new ProjectInvitationParam("/project", "demo@demo.com",
				Arrays.asList("/project/a"), true, Instant.now().plusSeconds(1000));
		projectInvMan.addInvitations(Set.of(projectParam));

		ArgumentCaptor<InvitationParam> argument = ArgumentCaptor.forClass(InvitationParam.class);
		verify(mockInvitationMan).addInvitation(argument.capture());

		InvitationParam targetParam = argument.getValue();
		assertThat(targetParam.getContactAddress()).isEqualTo("demo@demo.com");
		assertThat(targetParam.getFormsPrefillData().get(0).getFormId()).isEqualTo("regForm");
		assertThat(targetParam.getFormsPrefillData().get(1).getFormId()).isEqualTo("enqForm");
		assertThat(targetParam.getType()).isEqualTo(InvitationType.COMBO);
		assertThat(targetParam.getFormsPrefillData().get(0).getAllowedGroups().get(0).getSelectedGroups()
				).contains("/project/a");
	}
	
	@Test
	public void shouldForwardToCoreManagerWhenEntitiesIsEmpty() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		when(mockIdMan.getAllEntitiesWithContactEmails(Set.of("demo@demo.com")))
				.thenReturn(null);

		ProjectInvitationParam projectParam = new ProjectInvitationParam("/project", "demo@demo.com",
				Arrays.asList("/project/a"), true, Instant.now().plusSeconds(1000));
		projectInvMan.addInvitations(Set.of(projectParam));

		ArgumentCaptor<InvitationParam> argument = ArgumentCaptor.forClass(InvitationParam.class);
		verify(mockInvitationMan).addInvitation(argument.capture());

		InvitationParam targetParam = argument.getValue();
		assertThat(targetParam.getContactAddress()).isEqualTo("demo@demo.com");
		assertThat(targetParam.getFormsPrefillData().get(0).getFormId()).isEqualTo("regForm");
		assertThat(targetParam.getFormsPrefillData().get(1).getFormId()).isEqualTo("enqForm");
		assertThat(targetParam.getType()).isEqualTo(InvitationType.COMBO);
		assertThat(targetParam.getFormsPrefillData().get(0).getAllowedGroups().get(0).getSelectedGroups()
				).contains("/project/a");
	}

	@Test
	public void shouldForwardToCoreManagerParamWithFixedGroups() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		when(mockIdMan.getAllEntitiesWithContactEmails(Set.of("demo@demo.com")))
		.thenReturn(Sets.newHashSet(new EntityWithContactInfo(new Entity(null, new EntityInformation(1L), null), "demo@demo.com", Set.of("/"))));

		ProjectInvitationParam projectParam = new ProjectInvitationParam("/project", "demo@demo.com",
				Arrays.asList("/project/a"), false, Instant.now()
						.plusSeconds(1000));
		projectInvMan.addInvitations(Set.of(projectParam));

		ArgumentCaptor<InvitationParam> argument = ArgumentCaptor.forClass(InvitationParam.class);
		verify(mockInvitationMan).addInvitation(argument.capture());

		InvitationParam targetParam = argument.getValue();
		assertThat(targetParam.getContactAddress()).isEqualTo("demo@demo.com");
		assertThat(targetParam.getFormsPrefillData()
				.get(0)
				.getFormId()).isEqualTo("regForm");
		assertThat(targetParam.getFormsPrefillData()
				.get(1)
				.getFormId()).isEqualTo("enqForm");
		assertThat(targetParam.getType()).isEqualTo(InvitationType.COMBO);
		assertThat(targetParam.getFormsPrefillData()
				.get(0)
				.getGroupSelections()
				.get(0)
				.getEntry()
				.getSelectedGroups()).contains("/project/a");
	}

	@Test
	public void shouldGetProjectInvitations() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		RegistrationForm rform = new RegistrationFormBuilder().withByInvitationOnly(false).withName("regForm")
				.withDefaultCredentialRequirement("").build();
		when(mockRegistrationMan.getForms()).thenReturn(Arrays.asList(rform));

		EnquiryForm eform = new EnquiryFormBuilder().withByInvitationOnly(false).withName("enqForm")
				.withType(EnquiryType.REQUESTED_OPTIONAL).withTargetGroups(new String[]
				{ "/" }).build();
		when(mockEnquiryMan.getEnquires()).thenReturn(Arrays.asList(eform));

		RegistrationInvitationParam inv1 = RegistrationInvitationParam.builder().withForm("regForm")
				.withExpiration(Instant.now().plusSeconds(1000)).build();

		EnquiryInvitationParam inv2 = EnquiryInvitationParam.builder().withForm("enqForm")
				.withExpiration(Instant.now().plusSeconds(1000)).build();

		ComboInvitationParam inv3 = ComboInvitationParam.builder()
				.withRegistrationForm(FormPrefill.builder().withForm("regForm").build())
				.withEnquiryForm(FormPrefill.builder().withForm("enqForm").build())
				.withExpiration(Instant.now().plusSeconds(1000)).build();

		when(mockInvitationMan.getInvitations()).thenReturn(Arrays.asList(new InvitationWithCode(inv1, "code1"),
				new InvitationWithCode(inv2, "code2"), new InvitationWithCode(inv3, "code3")));

		List<ProjectInvitation> invitations = projectInvMan.getInvitations("/project");

		assertThat(invitations.size()).isEqualTo(3);
	}

	@Test
	public void shouldResendInvitationValidForMoreThanEightHours() throws EngineException
	{
		InvitationWithCode invitation = getComboInvitation("regForm", "enqForm",
				Instant.now().plus(9, ChronoUnit.HOURS));
		prepareProjectInvitation(invitation);

		projectInvMan.resendInvitation("/project", "code2");

		verify(mockInvitationMan).sendInvitation("code2");
		verify(mockInvitationMan, never()).addInvitation(any());
		verify(mockInvitationMan, never()).removeInvitation(any());
	}

	@Test
	public void shouldRejectResendWhenInvitationIsValidForLessThanEightHours() throws EngineException
	{
		InvitationWithCode invitation = getComboInvitation("regForm", "enqForm",
				Instant.now().plus(7, ChronoUnit.HOURS));
		prepareProjectInvitation(invitation);

		Throwable exception = catchThrowable(() -> projectInvMan.resendInvitation("/project", "code2"));

		assertExceptionType(exception, WrongArgumentException.class);
		verify(mockInvitationMan, never()).sendInvitation(any());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("reinvitationCases")
	public void shouldReinviteExpiredInvitationOfEveryType(ReinvitationCase testCase) throws EngineException
	{
		Instant referenceTime = Instant.now();
		Instant originalExpiration = referenceTime.minus(1, ChronoUnit.DAYS);
		InvitationParam originalInvitation = copyWithExpiration(testCase.invitation, originalExpiration);
		InvitationWithCode original = new InvitationWithCode(originalInvitation, "code");
		original.setCreationTime(referenceTime.minus(4, ChronoUnit.DAYS));
		prepareProjectInvitation(original);
		when(mockInvitationMan.addInvitation(any())).thenReturn("newCode");
		Instant beforeReinvite = Instant.now();

		projectInvMan.reinvite("/project", "code");

		ArgumentCaptor<InvitationParam> newInvitationCaptor = ArgumentCaptor.forClass(InvitationParam.class);
		verify(mockInvitationMan).addInvitation(newInvitationCaptor.capture());
		InvitationParam newInvitation = newInvitationCaptor.getValue();
		assertThat(newInvitation).isNotSameAs(original.getInvitation());
		assertThat(newInvitation.getType()).isEqualTo(original.getInvitation().getType());
		assertThat(newInvitation.getContactAddress()).isEqualTo(original.getInvitation().getContactAddress());
		assertThat(newInvitation.getFormsPrefillData()).isEqualTo(original.getInvitation().getFormsPrefillData());
		assertThat(newInvitation.getExpiration()).isBetween(
				beforeReinvite.plus(Duration.ofDays(3)), Instant.now().plus(Duration.ofDays(3)));
		assertThat(original.getInvitation().getExpiration()).isEqualTo(originalExpiration);
		verify(mockInvitationMan).sendInvitation("newCode");
		verify(mockInvitationMan).removeInvitation("code");
	}

	@Test
	public void shouldReinviteInvitationThatIsStillValid() throws EngineException
	{
		Instant referenceTime = Instant.now();
		InvitationWithCode original = new InvitationWithCode(new ComboInvitationParam("regForm", "enqForm",
				referenceTime.plus(2, ChronoUnit.DAYS), "combo@example.com"), "code");
		original.setCreationTime(referenceTime.minus(1, ChronoUnit.DAYS));
		prepareProjectInvitation(original);
		when(mockInvitationMan.addInvitation(any())).thenReturn("newCode");

		projectInvMan.reinvite("/project", "code");

		ArgumentCaptor<InvitationParam> newInvitationCaptor = ArgumentCaptor.forClass(InvitationParam.class);
		verify(mockInvitationMan).addInvitation(newInvitationCaptor.capture());
		assertThat(newInvitationCaptor.getValue().getExpiration()).isAfter(original.getInvitation().getExpiration());
		verify(mockInvitationMan).sendInvitation("newCode");
		verify(mockInvitationMan).removeInvitation("code");
	}

	@Test
	public void shouldRemoveReplacementInvitationWhenSendingFails() throws EngineException
	{
		InvitationWithCode original = getComboInvitation("regForm", "enqForm", Instant.now().minusSeconds(1));
		prepareProjectInvitation(original);
		when(mockInvitationMan.addInvitation(any())).thenReturn("newCode");
		doThrow(new EngineException("send failed")).when(mockInvitationMan).sendInvitation("newCode");

		Throwable exception = catchThrowable(() -> projectInvMan.reinvite("/project", "code2"));

		assertThat(exception).hasMessage("send failed");
		verify(mockInvitationMan).removeInvitation("newCode");
		verify(mockInvitationMan, never()).removeInvitation("code2");
	}

	@Test
	public void shouldRemoveReplacementInvitationWhenRemovingOriginalFails() throws EngineException
	{
		InvitationWithCode original = getComboInvitation("regForm", "enqForm", Instant.now().minusSeconds(1));
		prepareProjectInvitation(original);
		when(mockInvitationMan.addInvitation(any())).thenReturn("newCode");
		doThrow(new EngineException("remove failed")).when(mockInvitationMan).removeInvitation("code2");

		Throwable exception = catchThrowable(() -> projectInvMan.reinvite("/project", "code2"));

		assertThat(exception).hasMessage("remove failed");
		verify(mockInvitationMan).sendInvitation("newCode");
		verify(mockInvitationMan).removeInvitation("newCode");
	}

	@Test
	public void shouldThrowIllegalInvitation() throws EngineException
	{

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));

		when(mockInvitationMan.getInvitations())
				.thenReturn(Arrays.asList(getComboInvitation("regForm1", "enqForm", Instant.now().plusSeconds(1000))));

		Throwable exception = catchThrowable(() -> projectInvMan.resendInvitation("/project", "code"));
		assertExceptionType(exception, IllegalInvitationException.class);
	}

	@Test
	public void shouldBlockSendNotProjectInvitation() throws EngineException
	{
		when(mockRegistrationMan.getForm("regForm")).thenReturn(
				new RegistrationFormBuilder().withDefaultCredentialRequirement("").withName("regForm").build());
		
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		when(mockInvitationMan.getInvitations())
				.thenReturn(Arrays.asList(getComboInvitation("regForm1", "enqForm", Instant.now().plusSeconds(1000))));
		Throwable exception = catchThrowable(() -> projectInvMan.resendInvitation("/project", "code2"));
		assertExceptionType(exception, NotProjectInvitation.class);
	}

	private InvitationWithCode getComboInvitation(String regForm, String enqForm, Instant exp)
	{
		return new InvitationWithCode(ComboInvitationParam.builder().withExpiration(exp)
				.withContactAddress("demo@demo.com")
				.withRegistrationForm(
						FormPrefill.builder().withForm(regForm).withAllowedGroups(Arrays.asList("/A")).build())
				.withEnquiryForm(FormPrefill.builder().withForm(enqForm).withAllowedGroups(Arrays.asList("/A")).build())
				.build(), "code2");
	}

	private void prepareProjectInvitation(InvitationWithCode invitation) throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		when(mockRegistrationMan.getForm("regForm")).thenReturn(
				new RegistrationFormBuilder().withDefaultCredentialRequirement("").withName("regForm").build());
		when(mockEnquiryMan.getEnquiry("enqForm")).thenReturn(new EnquiryFormBuilder().withName("enqForm")
				.withType(EnquiryType.REQUESTED_MANDATORY).withTargetGroups(new String[] {}).build());
		when(mockInvitationMan.getInvitations()).thenReturn(List.of(invitation));
	}

	private static Stream<ReinvitationCase> reinvitationCases()
	{
		return Stream.of(
				ReinvitationCase.builder()
						.withName("registration invitation")
						.withInvitation(RegistrationInvitationParam.builder()
								.withForm("regForm")
								.withContactAddress("registration@example.com")
								.withExpiration(Instant.EPOCH)
								.build())
						.build(),
				ReinvitationCase.builder()
						.withName("enquiry invitation")
						.withInvitation(EnquiryInvitationParam.builder()
								.withForm("enqForm")
								.withEntity(1L)
								.withContactAddress("enquiry@example.com")
								.withExpiration(Instant.EPOCH)
								.build())
						.build(),
				ReinvitationCase.builder()
						.withName("combo invitation")
						.withInvitation(new ComboInvitationParam("regForm", "enqForm", Instant.EPOCH,
								"combo@example.com"))
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

	private record ReinvitationCase(String name, InvitationParam invitation)
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

			private ReinvitationCase build()
			{
				return new ReinvitationCase(name, invitation);
			}
		}
	}
}
