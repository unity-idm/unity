package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.rest.mappers.registration.EnquiryFormMapper;
import io.imunity.rest.mappers.registration.RegistrationFormMapper;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.webui.common.FormValidationException;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RestProjectFormServiceTest
{
	@Mock
	private GroupsManagement groupMan;
	@Mock
	private GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	@Mock
	private UpmanRestAuthorizationManager authz;
	@Mock
	private RegistrationsManagement registrationsManagement;
	@Mock
	private EnquiryManagement enquiryManagement;
	@Mock
	private ProjectFormsValidator validator;

	private RestProjectFormService service;

	@BeforeEach
	void setUp()
	{
		service = new RestProjectFormService(authz, new RestProjectFormServiceNoAuthz(groupDelegationConfigGenerator,
				groupMan, registrationsManagement, enquiryManagement, validator, "/A"), "/A/B");
	}

	@Test
	public void shouldAddRegistrationForm() throws EngineException
	{
		setUpGroupContent(new GroupDelegationConfiguration(true));
		RestRegistrationForm form = RestRegistrationForm.builder()
				.withName("form")
				.withDefaultCredentialRequirement("cred")
				.build();
		service.addRegistrationForm("A", form);
		Group group2 = new Group("/A/A");
		group2.setDisplayedName(new I18nString());
		group2.setDelegationConfiguration(GroupDelegationConfiguration.builder()
				.withRegistrationForm("form")
				.withEnabled(true)
				.build());
		verify(groupMan).updateGroup("/A/A", group2);
		verify(registrationsManagement).addForm(RegistrationFormMapper.map(form));
	}

	@Test
	public void shouldNotAddRegistrationFormWhenValidationFailed() throws EngineException, FormValidationException
	{
		setUpGroupContent(new GroupDelegationConfiguration(true));
		RestRegistrationForm form = RestRegistrationForm.builder()
				.withName("form")
				.withDefaultCredentialRequirement("cred")
				.build();
		doThrow(FormValidationException.class).when(validator)
				.assertRegistrationFormIsRestrictedToProjectGroup(RegistrationFormMapper.map(form), "/A/A");
		Assertions.assertThrows(IllegalArgumentException.class, () -> service.addRegistrationForm("A", form));
	}

	@Test
	public void shouldUpdateRegistrationForm() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withRegistrationForm("form")
				.withEnabled(true)
				.build());

		RestRegistrationForm form = RestRegistrationForm.builder()
				.withName("form")
				.withDefaultCredentialRequirement("cred")
				.build();
		service.updateRegistrationForm("A", form, true);
		verify(registrationsManagement).updateForm(RegistrationFormMapper.map(form), true);
	}

	@Test
	public void shouldNotUpdateRegistrationFormWhenValidationFailed() throws EngineException, FormValidationException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withRegistrationForm("form")
				.withEnabled(true)
				.build());
		RestRegistrationForm form = RestRegistrationForm.builder()
				.withName("form")
				.withDefaultCredentialRequirement("cred")
				.build();
		doThrow(FormValidationException.class).when(validator)
				.assertRegistrationFormIsRestrictedToProjectGroup(RegistrationFormMapper.map(form), "/A/A");
		Assertions.assertThrows(IllegalArgumentException.class, () -> service.updateRegistrationForm("A", form, true));
	}

	@Test
	public void shouldRemoveRegistrationForm() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withRegistrationForm("form")
				.withEnabled(true)
				.build());
		service.removeRegistrationForm("A", true);
		verify(registrationsManagement).removeForm("form", true);
	}

	@Test
	public void shouldGetRegistrationForm() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withRegistrationForm("form")
				.withEnabled(true)
				.build());

		RestRegistrationForm form = RestRegistrationForm.builder()
				.withName("form")
				.withDefaultCredentialRequirement("cred")
				.build();
		RegistrationForm mappedForm = RegistrationFormMapper.map(form);
		when(registrationsManagement.getForm("form")).thenReturn(mappedForm);

		RestRegistrationForm registrationForm = service.getRegistrationForm("A");
		assertThat(RegistrationFormMapper.map(mappedForm)).isEqualTo(registrationForm);
	}

	@Test
	public void shouldAddJoinEnquiryForm() throws EngineException
	{
		setUpGroupContent(new GroupDelegationConfiguration(true));
		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		service.addSignupEnquiryForm("A", form);
		Group group2 = new Group("/A/A");
		group2.setDisplayedName(new I18nString());
		group2.setDelegationConfiguration(GroupDelegationConfiguration.builder()
				.withSignupEnquiryForm("form")
				.withEnabled(true)
				.build());
		verify(groupMan).updateGroup("/A/A", group2);
		verify(enquiryManagement).addEnquiry(EnquiryFormMapper.map(form));
	}

	@Test
	public void shouldNotAddJoinEnquiryFormWhenValidationFailed() throws EngineException
	{
		setUpGroupContent(new GroupDelegationConfiguration(true));
		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		doThrow(IllegalArgumentException.class).when(validator)
				.assertCommonPartOfFormIsRestrictedToProjectGroup(EnquiryFormMapper.map(form), "/A/A");
		Assertions.assertThrows(IllegalArgumentException.class, () -> service.addSignupEnquiryForm("A", form));
	}

	@Test
	public void shouldUpdateJoinEnquiryForm() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withSignupEnquiryForm("form")
				.withEnabled(true)
				.build());
		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		service.updateSignupEnquiryForm("A", form, true);
		verify(enquiryManagement).updateEnquiry(EnquiryFormMapper.map(form), true);
	}

	@Test
	public void shouldNotUpdateJoinEnquiryFormWhenValidationFailed() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withSignupEnquiryForm("form")
				.withEnabled(true)
				.build());
		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		doThrow(IllegalArgumentException.class).when(validator)
				.assertCommonPartOfFormIsRestrictedToProjectGroup(EnquiryFormMapper.map(form), "/A/A");
		Assertions.assertThrows(IllegalArgumentException.class, () -> service.updateSignupEnquiryForm("A", form, true));
	}

	@Test
	public void shouldRemoveJoinEnquiryForm() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withSignupEnquiryForm("form")
				.withEnabled(true)
				.build());
		service.removeSignupEnquiryForm("A", true);
		verify(enquiryManagement).removeEnquiry("form", true);
	}

	@Test
	public void shouldGetJoinEnquiryForm() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withSignupEnquiryForm("form")
				.withEnabled(true)
				.build());
		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		EnquiryForm mappedForm = EnquiryFormMapper.map(form);
		when(enquiryManagement.getEnquiry("form")).thenReturn(mappedForm);

		RestEnquiryForm enquiryForm = service.getSignupEnquiryForm("A");
		assertThat(EnquiryFormMapper.map(mappedForm)).isEqualTo(enquiryForm);
	}

	@Test
	public void shouldAddMembershipUpdateEnquiryForm() throws EngineException
	{
		setUpGroupContent(new GroupDelegationConfiguration(true));

		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		service.addMembershipUpdateEnquiryForm("A", form);
		Group group2 = new Group("/A/A");
		group2.setDisplayedName(new I18nString());
		group2.setDelegationConfiguration(GroupDelegationConfiguration.builder()
				.withMembershipUpdateEnquiryForm("form")
				.withEnabled(true)
				.build());
		verify(groupMan).updateGroup("/A/A", group2);
		verify(enquiryManagement).addEnquiry(EnquiryFormMapper.map(form));
	}

	@Test
	public void shouldNotAddMembershipUpdateEnquiryFormWhenValidationFailed() throws EngineException
	{
		setUpGroupContent(new GroupDelegationConfiguration(true));
		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		doThrow(IllegalArgumentException.class).when(validator)
				.assertCommonPartOfFormIsRestrictedToProjectGroup(EnquiryFormMapper.map(form), "/A/A");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> service.addMembershipUpdateEnquiryForm("A", form));
	}

	@Test
	public void shouldUpdateMembershipUpdateEnquiryForm() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withMembershipUpdateEnquiryForm("form")
				.withEnabled(true)
				.build());
		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		service.updateMembershipUpdateEnquiryForm("A", form, true);
		verify(enquiryManagement).updateEnquiry(EnquiryFormMapper.map(form), true);
	}

	@Test
	public void shouldNotUpdateMembershipUpdateFormWhenValidationFailed() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withMembershipUpdateEnquiryForm("form")
				.withEnabled(true)
				.build());
		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		doThrow(IllegalArgumentException.class).when(validator)
				.assertCommonPartOfFormIsRestrictedToProjectGroup(EnquiryFormMapper.map(form), "/A/A");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> service.updateMembershipUpdateEnquiryForm("A", form, true));
	}

	@Test
	public void shouldRemoveMembershipUpdateEnquiryForm() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withMembershipUpdateEnquiryForm("form")
				.withEnabled(true)
				.build());
		service.removeMembershipUpdateEnquiryForm("A", true);
		verify(enquiryManagement).removeEnquiry("form", true);
	}

	@Test
	public void shouldGetMembershipUpdateEnquiryForm() throws EngineException
	{
		setUpGroupContent(GroupDelegationConfiguration.builder()
				.withMembershipUpdateEnquiryForm("form")
				.withEnabled(true)
				.build());

		RestEnquiryForm form = RestEnquiryForm.builder()
				.withName("form")
				.withTargetCondition("true")
				.withType(EnquiryType.REQUESTED_MANDATORY.name())
				.withTargetGroups(List.of("/"))
				.build();
		EnquiryForm mappedForm = EnquiryFormMapper.map(form);
		when(enquiryManagement.getEnquiry("form")).thenReturn(mappedForm);

		RestEnquiryForm enquiryForm = service.getMembershipUpdateEnquiryForm("A");
		assertThat(EnquiryFormMapper.map(mappedForm)).isEqualTo(enquiryForm);
	}

	private void setUpGroupContent(GroupDelegationConfiguration config) throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(config);
		content.setGroup(group);
		when(groupMan.getContents("/A/A", GroupContents.METADATA)).thenReturn(content);
	}

}
