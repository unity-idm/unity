package io.imunity.test.ui.flows;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.base.registration.layout.FormLayoutSettings;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;

@Tag("public")
class WorkflowCompletionTest extends EndUserTestBase
{
	@Autowired
	@Qualifier("insecure")
	private RegistrationsManagement registrations;

	@ParameterizedTest(name = "{0}")
	@MethodSource("outcomes")
	void shouldShowConfiguredCompletionAndReturnToProtectedHome(Outcome scenario) throws Exception
	{
		String form = "ui-workflow-completion";
		boolean missingInvitation = scenario.state() == TriggeringState.INVITATION_MISSING;
		boolean cancelled = scenario.state() == TriggeringState.CANCELLED;
		registrations.addForm(new RegistrationFormBuilder().withName(form).withPubliclyAvailable(true)
				.withDefaultCredentialRequirement("ui-password").withCaptchaLength(0).withCollectComments(true)
				.withByInvitationOnly(missingInvitation)
				.withFormLayoutSettings(new FormLayoutSettings(false, 21, "em", true, null))
				.withWrapUpConfig(List.of(new RegistrationWrapUpConfig(scenario.state(),
						new I18nString("Selenium " + scenario.state()), new I18nString("Your workflow has completed"),
						new I18nString("Return to account"), scenario.automatic(), baseUrl + "/home", null))).build());
		driver.get(baseUrl + "/unitygw/pub/registration/" + form
				+ (missingInvitation ? "?regcode=unknown-invitation" : ""));
		VaadinPage page = new VaadinPage(driver);
		if (!missingInvitation)
		{
			page.fill(By.cssSelector("vaadin-text-area"), "Workflow completion request");
			if (cancelled)
				page.click(By.xpath("//*[self::span or self::a][normalize-space(.)='Cancel']"));
			else
				page.click(testId("StandaloneRegistration.submit"));
		}
		if (!scenario.automatic())
		{
			boolean error = missingInvitation || cancelled;
			assertThat(page.visible(By.className(error ? "u-final-error" : "u-final-info")).getText())
					.isEqualTo("Selenium " + scenario.state());
			By information = By.className(error ? "u-final-ext-error" : "u-final-ext-info");
			assertThat(page.visible(information).getText())
					.isEqualTo("Your workflow has completed");
			assertThat(page.visible(By.className("u-final-redirect")).getText()).isEqualTo("Return to account");
			page.click(By.className("u-final-redirect"));
		}
		if (scenario.automatic())
		{
			waitFor(() -> driver.getWindowHandles().size() > 1 || driver.getCurrentUrl().contains("/home"));
			if (driver.getWindowHandles().size() > 1)
			{
				String source = driver.getWindowHandle();
				driver.switchTo().window(driver.getWindowHandles().stream().filter(handle -> !handle.equals(source))
						.findFirst().orElseThrow());
			}
		}
		page.visible(By.className("u-passwordUsernameField"));
		assertThat(driver.getCurrentUrl()).contains("/home/authentication");
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		if (cancelled || missingInvitation)
			assertThat(registrations.getRegistrationRequests()).isEmpty();
		else
			assertThat(registrations.getRegistrationRequests()).singleElement().satisfies(request ->
			{
				assertThat(request.getStatus()).isEqualTo(RegistrationRequestStatus.pending);
				assertThat(request.getRequest().getComments()).isEqualTo("Workflow completion request");
				assertThat(request.getCreatedEntityId()).isNull();
			});
	}

	private static Stream<Outcome> outcomes()
	{
		return Stream.of(Outcome.builder().state(TriggeringState.SUBMITTED).build(),
				Outcome.builder().state(TriggeringState.CANCELLED).build(),
				Outcome.builder().state(TriggeringState.INVITATION_MISSING).build(),
				Outcome.builder().state(TriggeringState.SUBMITTED).automatic(true).build());
	}

	private record Outcome(TriggeringState state, boolean automatic)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private TriggeringState state;
			private boolean automatic;

			Builder state(TriggeringState value)
			{
				state = value;
				return this;
			}

			Builder automatic(boolean value)
			{
				automatic = value;
				return this;
			}

			Outcome build()
			{
				return new Outcome(state, automatic);
			}
		}
	}
}
