package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.EndUserTestBase;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;

@Tag("admin-registration")
class RegistrationFormEditorTest extends EndUserTestBase
{
	private static final String FORM = "ui-edited-signup";

	@Autowired
	@Qualifier("insecure")
	private RegistrationsManagement registrations;

	@Test
	void shouldCreateReopenAndEditPublicFormAndCollectSubmittedComment() throws Exception
	{
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/forms/registration");
		page.fill(testId("RegistrationFormEditor.name"), FORM);
		page.fill(testId("RegistrationFormEditor.description"), "Initial browser-created form");
		page.check(testId("RegistrationFormEditor.publiclyAvailable"), true);
		page.fill(testId("RegistrationFormEditor.captcha"), "0");
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/forms");
		driver.get(baseUrl + "/admin/forms/registration/" + FORM);
		assertThat(page.value(testId("RegistrationFormEditor.name"))).isEqualTo(FORM);
		assertThat(page.value(testId("RegistrationFormEditor.description"))).isEqualTo("Initial browser-created form");
		page.fill(testId("RegistrationFormEditor.description"), "Updated browser-created form");
		page.click(By.xpath("//vaadin-tab[normalize-space(.)='Collected data']"));
		page.check(testId("RegistrationFormEditor.collectComments"), true);
		page.click(By.xpath("//vaadin-tab[normalize-space(.)='Visual settings']"));
		page.fill(testId("RegistrationFormEditor.pageTitle"), "Edited Selenium signup");
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/forms");
		var saved = registrations.getForms().stream().filter(form -> form.getName().equals(FORM)).findFirst()
				.orElseThrow();
		assertThat(saved.getDescription()).isEqualTo("Updated browser-created form");
		assertThat(saved.isCollectComments()).isTrue();
		assertThat(saved.getPageTitle().getValue("en")).isEqualTo("Edited Selenium signup");
		driver.get(baseUrl + "/admin/forms/registration/" + FORM);
		page.click(By.xpath("//vaadin-tab[normalize-space(.)='Collected data']"));
		assertThat(page.visible(testId("RegistrationFormEditor.collectComments")).getDomProperty("checked"))
				.isEqualTo("true");
		page.click(By.xpath("//vaadin-tab[normalize-space(.)='Visual settings']"));
		assertThat(page.value(testId("RegistrationFormEditor.pageTitle"))).isEqualTo("Edited Selenium signup");
		var applicant = newBrowser();
		VaadinPage signup = new VaadinPage(applicant);
		applicant.get(baseUrl + "/unitygw/pub/registration/" + FORM);
		signup.fill(By.cssSelector("vaadin-text-area"), "Submitted through the edited public form");
		assertThat(applicant.getTitle()).isEqualTo("Edited Selenium signup");
		signup.click(testId("StandaloneRegistration.submit"));
		waitFor(() ->
		{
			try
			{
				return registrations.getRegistrationRequests().stream()
						.anyMatch(request -> request.getRequest().getFormId().equals(FORM));
			} catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		});
		assertThat(registrations.getRegistrationRequests()).singleElement().satisfies(request ->
		{
			assertThat(request.getRequest().getComments()).isEqualTo("Submitted through the edited public form");
			assertThat(request.getStatus()).isEqualTo(RegistrationRequestStatus.pending);
			assertThat(request.getCreatedEntityId()).isNull();
		});
	}
}
