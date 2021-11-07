/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.engine.api.project.ProjectInvitation;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement.IllegalInvitationException;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement.NotProjectInvitation;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.invite.ComboInvitationParam;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.types.registration.invite.FormPrefill;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

@RunWith(MockitoJUnitRunner.class)
public class TestProjectInvitationManagement extends TestProjectBase
{
	@Mock
	private PublicRegistrationURLSupport mockPublicRegistrationURLSupport;

	private ProjectInvitationsManagementImpl projectInvMan;

	@Before
	public void initProjectInvitationMan()
	{
		projectInvMan = new ProjectInvitationsManagementImpl(mockInvitationMan, mockGroupMan, mockRegistrationMan,
				mockEnquiryMan, mockIdMan, mockPublicRegistrationURLSupport, mockAuthz, mockMsg);
	}

	@Test
	public void shouldForwardToCoreManagerWithAllowedGroups() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		when(mockIdMan.getAllEntitiesWithContactEmail("demo@demo.com"))
				.thenReturn(Arrays.asList(new Entity(null, new EntityInformation(1L), null)));

		ProjectInvitationParam projectParam = new ProjectInvitationParam("/project", "demo@demo.com",
				Arrays.asList("/project/a"), true, Instant.now().plusSeconds(1000));
		projectInvMan.addInvitation(projectParam);

		ArgumentCaptor<InvitationParam> argument = ArgumentCaptor.forClass(InvitationParam.class);
		verify(mockInvitationMan).addInvitation(argument.capture());

		InvitationParam targetParam = argument.getValue();
		assertThat(targetParam.getContactAddress(), is("demo@demo.com"));
		assertThat(targetParam.getFormsPrefillData().get(0).getFormId(), is("regForm"));
		assertThat(targetParam.getFormsPrefillData().get(1).getFormId(), is("enqForm"));
		assertThat(targetParam.getType(), is(InvitationType.COMBO));
		assertThat(targetParam.getFormsPrefillData().get(0).getAllowedGroups().get(0).getSelectedGroups(),
				hasItems("/project/a"));
	}

	@Test
	public void shouldForwardToCoreManagerParamWithFixedGroups() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));
		when(mockIdMan.getAllEntitiesWithContactEmail("demo@demo.com"))
				.thenReturn(Arrays.asList(new Entity(null, new EntityInformation(1L), null)));
		ProjectInvitationParam projectParam = new ProjectInvitationParam("/project", "demo@demo.com",
				Arrays.asList("/project/a"), false, Instant.now().plusSeconds(1000));
		projectInvMan.addInvitation(projectParam);

		ArgumentCaptor<InvitationParam> argument = ArgumentCaptor.forClass(InvitationParam.class);
		verify(mockInvitationMan).addInvitation(argument.capture());

		InvitationParam targetParam = argument.getValue();
		assertThat(targetParam.getContactAddress(), is("demo@demo.com"));
		assertThat(targetParam.getFormsPrefillData().get(0).getFormId(), is("regForm"));
		assertThat(targetParam.getFormsPrefillData().get(1).getFormId(), is("enqForm"));
		assertThat(targetParam.getType(), is(InvitationType.COMBO));
		assertThat(targetParam.getFormsPrefillData().get(0).getGroupSelections().get(0).getEntry().getSelectedGroups(),
				hasItems("/project/a"));
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

		assertThat(invitations.size(), is(3));
	}

	@Test
	public void shouldForwardSendInvToCoreManager() throws EngineException
	{
		when(mockRegistrationMan.getForm("regForm")).thenReturn(
				new RegistrationFormBuilder().withDefaultCredentialRequirement("").withName("regForm").build());

		when(mockEnquiryMan.getEnquiry("enqForm")).thenReturn(new EnquiryFormBuilder().withName("enqForm")
				.withType(EnquiryType.REQUESTED_MANDATORY).withTargetGroups(new String[]
				{}).build());

		shouldForwardSendInvToCoreManager(getComboInvitation("regForm", "enqForm", Instant.now().plusSeconds(1000)),
				"code2");
	}

	private void shouldForwardSendInvToCoreManager(InvitationWithCode inv, String code) throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));

		when(mockInvitationMan.getInvitations()).thenReturn(Arrays.asList(inv));
		projectInvMan.sendInvitation("/project", code);

		verify(mockInvitationMan).sendInvitation(eq(code));
	}

	@Test
	public void shouldOverwriteExpiredInvitation() throws EngineException
	{
		when(mockEnquiryMan.getEnquiry("enqForm")).thenReturn(new EnquiryFormBuilder().withName("enqForm")
				.withType(EnquiryType.REQUESTED_MANDATORY).withTargetGroups(new String[]
				{}).build());
		when(mockRegistrationMan.getForm("regForm")).thenReturn(
				new RegistrationFormBuilder().withDefaultCredentialRequirement("").withName("regForm").build());

		shouldOverwriteExpiredInvitation(getComboInvitation("regForm", "enqForm", Instant.now().minusSeconds(1000)),
				"code2");
	}

	private void shouldOverwriteExpiredInvitation(InvitationWithCode inv, String code) throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));

		when(mockInvitationMan.getInvitations()).thenReturn(Arrays.asList(inv));
		projectInvMan.sendInvitation("/project", code);

		ArgumentCaptor<InvitationParam> argument = ArgumentCaptor.forClass(InvitationParam.class);
		verify(mockInvitationMan).addInvitation(argument.capture());

		InvitationParam param = argument.getValue();

		assertThat(param.getFormsPrefillData().get(0).getFormId(),
				is(inv.getInvitation().getFormsPrefillData().get(0).getFormId()));
		assertThat(param.getContactAddress(), is("demo@demo.com"));
		assertThat(param.getFormsPrefillData().get(0).getAllowedGroups().get(0).getSelectedGroups().get(0), is("/A"));

		verify(mockInvitationMan).removeInvitation(code);
		verify(mockInvitationMan).sendInvitation(any());
	}

	@Test
	public void shouldThrowIllegalInvitation() throws EngineException
	{

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getConfiguredGroupContents("/project"));

		when(mockInvitationMan.getInvitations())
				.thenReturn(Arrays.asList(getComboInvitation("regForm1", "enqForm", Instant.now().plusSeconds(1000))));

		Throwable exception = catchThrowable(() -> projectInvMan.sendInvitation("/project", "code"));
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
		Throwable exception = catchThrowable(() -> projectInvMan.sendInvitation("/project", "code2"));
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
}
