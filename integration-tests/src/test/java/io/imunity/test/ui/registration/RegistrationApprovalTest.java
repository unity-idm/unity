package io.imunity.test.ui.registration;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;

@Tag("registration")
class RegistrationApprovalTest extends UserAccountTestBase
{
	private final String suffix = UUID.randomUUID().toString();
	private final String form = "ui-registration-" + suffix;
	private final String group = "/ui-registered-" + suffix;
	private final String username = "ui-applicant-" + suffix;
	private static final String PASSWORD = "Registered!69-Harbor";
	private static final String DISPLAY_NAME = "Registered Selenium member";
	private boolean groupCreated;
	private boolean formCreated;

	@Autowired
	@Qualifier("insecure")
	private RegistrationsManagement registrations;
	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groups;

	@BeforeEach
	void prepareRegistrationForm() throws Exception
	{
		groups.addGroup(new Group(group));
		groupCreated = true;
		TranslationProfile profile = new TranslationProfile(form, "", ProfileType.REGISTRATION, List.of(
				new TranslationRule("true", new TranslationAction(AddToGroupActionFactory.NAME, "'" + group + "'")),
				new TranslationRule("true", new TranslationAction(AddAttributeActionFactory.NAME,
						"name", "/", "'" + DISPLAY_NAME + "'")),
				new TranslationRule("true", new TranslationAction(AddAttributeActionFactory.NAME,
						"sys:AuthorizationRole", "/", "'Regular User'"))));
		registrations.addForm(new RegistrationFormBuilder()
				.withName(form)
				.withPageTitle(new I18nString("Selenium registration"))
				.withDefaultCredentialRequirement("ui-password")
				.withPubliclyAvailable(true)
				.withAddedCredentialParam(new CredentialRegistrationParam("sys:password", null, null))
				.withAddedIdentityParam().withIdentityType("userName")
						.withRetrievalSettings(ParameterRetrievalSettings.interactive).endIdentityParam()
				.withTranslationProfile(profile).build());
		formCreated = true;
	}

	@AfterEach
	void removeRegistrationFixture() throws Exception
	{
		try
		{
			if (formCreated)
			{
				for (RegistrationRequestState request : requests())
					if (request.getCreatedEntityId() != null)
						trackUser(new EntityParam(request.getCreatedEntityId()));
				registrations.removeForm(form, true);
			}
		} finally
		{
			if (groupCreated)
				groups.removeGroup(group, true);
		}
	}

	@Test
	void shouldAuthenticateRegisteredUserAfterAdministratorApproval() throws Exception
	{
		RegistrationRequestState submitted = submitRegistration();
		assertThat(requests()).hasSize(1);
		assertThat(submitted.getStatus()).isEqualTo(RegistrationRequestStatus.pending);
		assertThat(submitted.getCreatedEntityId()).isNull();

		RegistrationRequestState approved = approveRequest(submitted.getRequestId());
		assertThat(approved.getStatus()).isEqualTo(RegistrationRequestStatus.accepted);
		assertThat(approved.getCreatedEntityId()).isNotNull();
		EntityParam account = new EntityParam(approved.getCreatedEntityId());
		assertThat(entities.getEntity(account).getIdentities()).anySatisfy(identity ->
		{
			assertThat(identity.getTypeId()).isEqualTo("userName");
			assertThat(identity.getValue()).isEqualTo(username);
		});
		assertThat(entities.getGroups(account)).containsKey(group);

		VaadinPage freshLogin = new VaadinPage(newBrowser());
		freshLogin.login(baseUrl, "/home", username, PASSWORD);
		assertThat(freshLogin.visible(By.id("MainHeader.loggedAs")).getText()).contains(DISPLAY_NAME);
	}

	private RegistrationRequestState submitRegistration()
	{
		VaadinPage registration = new VaadinPage(driver);
		driver.get(baseUrl + "/unitygw/pub/registration/" + form);
		registration.fill(By.id("UsernameIdentityEditor.username"), username);
		registration.fill(By.className("u-password-setup"), PASSWORD);
		registration.fill(By.className("u-password-repeat"), PASSWORD);
		registration.click(testId("StandaloneRegistration.submit"));
		waitFor(() -> !requests().isEmpty());
		return requests().getFirst();
	}

	private RegistrationRequestState approveRequest(String requestId) throws EngineException
	{
		WebDriver administrator = newBrowser();
		VaadinPage review = new VaadinPage(administrator);
		review.login(baseUrl, "/admin/requests", "ui-admin", "Ui-admin!42");
		review.click(By.cssSelector("[data-testid='RequestsGrid.form'][data-request-id='"
				+ requestId + "']"));
		assertThat(review.visible(testId("RequestProcessingPanel.status")).getText()).isEqualTo("PENDING");
		review.click(testId("RequestProcessingPanel.accept"));
		waitFor(() -> review.visible(testId("RequestProcessingPanel.status")).getText().equals("ACCEPTED"));
		screenshot(administrator, "Approved registration request");

		return registrations.getRegistrationRequest(requestId);
	}

	private List<RegistrationRequestState> requests()
	{
		try
		{
			return registrations.getRegistrationRequests().stream()
					.filter(request -> form.equals(request.getRequest().getFormId())).toList();
		} catch (EngineException e)
		{
			throw new IllegalStateException("Cannot read registration requests", e);
		}
	}
}
