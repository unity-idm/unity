package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;

abstract class ProjectInvitationTestBase extends ProjectWorkflowTestBase
{
	static final String RECIPIENT = "invitee@example.org";
	static final String PASSWORD = "Invited!73-Sailboat";
	final String username = "ui-invited-" + UUID.randomUUID();
	String registrationForm;
	String enquiryForm;

	@Autowired
	@Qualifier("insecure")
	RegistrationsManagement registrations;
	@Autowired
	@Qualifier("insecure")
	EnquiryManagement enquiries;
	@Autowired
	private GroupDelegationConfigGenerator generator;

	@AfterEach
	void removeProjectForms() throws Exception
	{
		if (registrationForm != null)
			registrations.removeFormWithoutDependencyChecking(registrationForm);
		if (enquiryForm != null)
			enquiries.removeEnquiryWithoutDependencyChecking(enquiryForm);
	}

	final Project prepareProject() throws Exception
	{
		Project project = createProject("Invitation project", false);
		messageTemplate("ui-project-invitation", "InvitationWithCode", "${url}", "default_email");
		RegistrationFormNotifications regNotifications = new RegistrationFormNotifications();
		regNotifications.setInvitationTemplate("ui-project-invitation");
		EnquiryFormNotifications enqNotifications = new EnquiryFormNotifications();
		enqNotifications.setInvitationTemplate("ui-project-invitation");
		RegistrationForm generated = generator.generateProjectRegistrationForm(
				project.path(), null, List.of(), List.of());
		RegistrationForm registration = new RegistrationFormBuilder(generated)
				.withByInvitationOnly(true)
				.withDefaultCredentialRequirement("ui-password")
				.withAttributeParams(List.of())
				.withAddedIdentityParam().withIdentityType("userName")
						.withRetrievalSettings(ParameterRetrievalSettings.interactive).endIdentityParam()
				.withNotificationsConfiguration(regNotifications)
				.withTranslationProfile(membershipProfile(project, true)).build();
		registrationForm = registration.getName();
		registrations.addForm(registration);
		EnquiryForm enquiry = new EnquiryFormBuilder(
				generator.generateProjectJoinEnquiryForm(project.path(), null, List.of()))
				.withNotificationsConfiguration(enqNotifications)
				.withTranslationProfile(membershipProfile(project, false)).build();
		enquiryForm = enquiry.getName();
		enquiries.addEnquiry(enquiry);
		Group group = storedGroup(project);
		group.setDelegationConfiguration(GroupDelegationConfiguration.builder()
				.withRegistrationForm(registrationForm).withSignupEnquiryForm(enquiryForm).build());
		groups.updateGroup(project.path(), group);
		return project;
	}

	final void invite(VaadinPage page, String email)
	{
		driver.get(baseUrl + "/upman/invitations");
		page.click(testId("InvitationsView.invite"));
		page.fill(testId("InvitationForm.emails"), email);
		page.click(testId("InvitationsView.send"));
		notificationTo(email);
	}

	final RegistrationRequestState submitInvitedRegistration(WebDriver browser, String link) throws Exception
	{
		VaadinPage page = new VaadinPage(browser);
		browser.get(link);
		page.fill(By.id("UsernameIdentityEditor.username"), username);
		page.fill(By.className("u-password-setup"), PASSWORD);
		page.fill(By.className("u-password-repeat"), PASSWORD);
		page.click(testId("StandaloneRegistration.submit"));
		waitFor(() -> !registrationRequests().isEmpty());
		return registrationRequests().getFirst();
	}

	final List<RegistrationRequestState> registrationRequests()
	{
		try
		{
			return registrations.getRegistrationRequests().stream()
					.filter(request -> registrationForm.equals(request.getRequest().getFormId())).toList();
		} catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	final void decide(VaadinPage page, String request, boolean accept)
	{
		driver.get(baseUrl + "/upman/user-updates");
		By actions = By.cssSelector("[data-testid='UserUpdatesView.actions'][data-request-id='" + request + "']");
		((JavascriptExecutor) driver).executeScript("""
				const grid = arguments[0].closest('vaadin-grid');
				const scroller = grid.shadowRoot.querySelector('#table');
				scroller.scrollLeft = scroller.scrollWidth;
				""", page.visible(actions));
		page.click(actions);
		page.click(testId(accept ? "UserUpdatesView.accept" : "UserUpdatesView.reject"));
	}

	private TranslationProfile membershipProfile(Project project, boolean registration)
	{
		List<TranslationRule> rules = new ArrayList<>();
		rules.add(new TranslationRule("true", new TranslationAction(AddToGroupActionFactory.NAME,
				"'" + project.path() + "'")));
		if (registration)
		{
			rules.add(new TranslationRule("true", new TranslationAction(AddAttributeActionFactory.NAME,
					"name", "/", "'Invited project member'")));
			rules.add(new TranslationRule("true", new TranslationAction(AddAttributeActionFactory.NAME,
					"sys:AuthorizationRole", "/", "'Regular User'")));
		}
		return new TranslationProfile("ui-membership", "", ProfileType.REGISTRATION, rules);
	}
}
