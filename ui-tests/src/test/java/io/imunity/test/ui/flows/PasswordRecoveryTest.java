package io.imunity.test.ui.flows;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.PasswordRecoveryConfiguration.CapturedCaptcha;
import io.imunity.test.ui.flows.PasswordRecoveryConfiguration.RecoveryCodeValidity;
import nl.captcha.Captcha;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings.ConfirmationMode;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;

@Tag("authentication")
@ContextConfiguration(initializers = PasswordRecoveryConfiguration.Initializer.class)
class PasswordRecoveryTest extends CredentialWorkflowTestBase
{
	private static final String EMAIL = "password-recovery@example.org";
	private static final String PATH = "/recover-password";
	private static final String REPLACEMENT = "Recovered!79-Seashell";
	private static final By USERNAME = testId("PasswordResetStep1Captcha.username");
	private static final By CAPTCHA = testId("CaptchaComponent.answer");
	private static final By CODE = testId("CredentialResetCodeVerificationUI.code");
	private static final By PROCEED = By.className("u-cred-reset-proceed");
	private static final By ERROR = By.cssSelector("vaadin-notification-card[theme~='error']");

	@Autowired
	private CapturedCaptcha captchas;
	@Autowired
	private RecoveryCodeValidity codeValidity;

	@ParameterizedTest(name = "{0}")
	@MethodSource("codeOutcomes")
	void shouldApplyPasswordRecoveryOnlyAfterValidEmailCode(RecoveryCase scenario) throws Exception
	{
		// given
		if (scenario.outcome() == CodeOutcome.EXPIRED)
			codeValidity.expireImmediately();
		TestUser user = prepareRecovery();
		TestUser control = createUser("Unrelated password owner");
		userCredentials.setEntityCredentialRequirements(control.entity(), "ui-multiple");
		userCredentials.setEntityCredential(control.entity(), CREDENTIAL, new PasswordToken(control.password()).toJson());
		VaadinPage page = openRecovery();
		page.fill(USERNAME, user.username());
		page.fill(CAPTCHA, captchas.latest().getAnswer().toUpperCase(Locale.ROOT));

		// when
		page.click(PROCEED);
		String code = notificationTo(EMAIL).body().strip();
		assertThat(code).isNotBlank();
		page.visible(CODE);
		page.click(PROCEED);
		waitFor(() -> Boolean.parseBoolean(page.visible(CODE).getDomProperty("invalid")));
		if (scenario.outcome() == CodeOutcome.INVALID_THEN_VALID)
		{
			page.fill(CODE, "incorrect");
			page.click(PROCEED);
			assertError("The entered code is invalid");
			assertThat(page.value(CODE)).isEmpty();
			assertThat(driver.findElements(By.className("u-password-setup"))).isEmpty();
		}
		page.fill(CODE, code);
		page.click(PROCEED);

		// then
		if (scenario.outcome() == CodeOutcome.EXPIRED)
		{
			assertError("The code is expired");
			page.visible(By.className("u-passwordUsernameField"));
			assertThat(driver.findElements(By.className("u-password-setup"))).isEmpty();
			assertRejectedLogin(user, REPLACEMENT);
			assertLogin(user, user.password());
		} else
		{
			setReplacementPassword(page);
			assertRejectedLogin(user, user.password());
			assertLogin(user, REPLACEMENT);
		}
		assertLogin(control, control.password());
	}

	@Test
	void shouldRequireCurrentCaptchaAndPreservePasswordWhenRecoveryIsCancelled() throws Exception
	{
		// given
		TestUser user = prepareRecovery();
		VaadinPage page = openRecovery();
		Captcha initial = captchas.latest();
		page.visible(testId("CaptchaComponent.image"));
		waitFor(() -> Integer.parseInt(page.visible(testId("CaptchaComponent.image"))
				.getDomProperty("naturalWidth")) > 0);
		assertThat(driver.getPageSource()).doesNotContain(initial.getAnswer());

		// when
		page.click(PROCEED);
		waitFor(() -> Boolean.parseBoolean(page.visible(USERNAME).getDomProperty("invalid")));
		assertThat(captchas.latest()).isSameAs(initial);
		page.fill(USERNAME, user.username());
		page.click(PROCEED);
		waitFor(() -> Boolean.parseBoolean(page.visible(CAPTCHA).getDomProperty("invalid")));
		assertThat(captchas.latest()).isNotSameAs(initial);
		Captcha beforeRefresh = captchas.latest();
		page.fill(CAPTCHA, "partial");
		page.click(testId("CaptchaComponent.refresh"));
		waitFor(() -> captchas.latest() != beforeRefresh && page.value(CAPTCHA).isEmpty());
		waitFor(() -> !Boolean.parseBoolean(page.visible(CAPTCHA).getDomProperty("invalid")));
		Captcha refreshed = captchas.latest();
		page.fill(CAPTCHA, "incorrect");
		page.click(PROCEED);
		waitFor(() -> Boolean.parseBoolean(page.visible(CAPTCHA).getDomProperty("invalid")));
		assertThat(captchas.latest()).isNotSameAs(refreshed);
		assertThat(notifications.sentTo(EMAIL)).isEmpty();
		assertThat(driver.findElements(CODE)).isEmpty();
		page.fill(CAPTCHA, captchas.latest().getAnswer());
		page.click(PROCEED);
		page.visible(CODE);
		String code = notificationTo(EMAIL).body().strip();
		page.click(By.className("u-credreset-resend-button"));
		waitFor(() -> notifications.sentTo(EMAIL).size() == 2);
		assertThat(notificationTo(EMAIL).body().strip()).isEqualTo(code);
		page.click(By.className("u-cred-reset-cancel"));

		// then
		page.visible(By.className("u-passwordUsernameField"));
		assertThat(driver.findElements(CODE)).isEmpty();
		assertRejectedLogin(user, REPLACEMENT);
		assertLogin(user, user.password());
	}

	private void setReplacementPassword(VaadinPage page)
	{
		page.fill(By.className("u-password-setup"), REPLACEMENT);
		page.fill(By.className("u-password-repeat"), "does-not-match");
		page.click(PROCEED);
		assertError("Passwords do not match");
		page.fill(By.className("u-password-setup"), REPLACEMENT);
		page.fill(By.className("u-password-repeat"), REPLACEMENT);
		page.click(PROCEED);
		waitFor(() -> page.visible(By.className("u-passwordResetTitle")).getText()
				.contains("Update was successful"));
	}

	private TestUser prepareRecovery() throws Exception
	{
		messageTemplate("ui-password-reset", "EmailPasswordResetCode", "${code}", "default_email");
		PasswordCredentialResetSettings recovery = new PasswordCredentialResetSettings();
		recovery.setEnabled(true);
		recovery.setConfirmationMode(ConfirmationMode.RequireEmail);
		recovery.setEmailSecurityCodeMsgTemplate("ui-password-reset");
		PasswordCredential configuration = new PasswordCredential();
		configuration.setMinLength(12);
		configuration.setPasswordResetSettings(recovery);
		TestUser user = credentialOwner("password", configuration.getSerializedConfiguration().toString());
		userCredentials.setEntityCredential(user.entity(), CREDENTIAL, new PasswordToken(user.password()).toJson());
		attributes.setAttribute(user.entity(), VerifiableEmailAttribute.of("email", "/",
				new VerifiableEmail(EMAIL, new ConfirmationInfo(true))));
		authenticators.createAuthenticator("ui-password-recovery", "password", "", CREDENTIAL);
		deployAt("UserHomeUI", PATH, "", List.of("ui-password-recovery"));
		return user;
	}

	private VaadinPage openRecovery()
	{
		driver.get(baseUrl + PATH);
		VaadinPage page = new VaadinPage(driver);
		page.click(By.cssSelector(".u-authn-forgotPassword span"));
		page.visible(USERNAME);
		return page;
	}

	private void assertLogin(TestUser user, String password)
	{
		WebDriver fresh = newBrowser();
		VaadinPage page = new VaadinPage(fresh);
		page.login(baseUrl, PATH, user.username(), password);
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}

	private void assertRejectedLogin(TestUser user, String password)
	{
		WebDriver fresh = newBrowser();
		VaadinPage page = new VaadinPage(fresh);
		fresh.get(baseUrl + PATH);
		page.signIn(user.username(), password);
		page.visible(ERROR);
		assertThat(fresh.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
	}

	private void assertError(String message)
	{
		waitFor(() -> driver.findElements(ERROR).stream()
				.anyMatch(element -> element.isDisplayed() && element.getText().contains(message)));
	}

	private static Stream<RecoveryCase> codeOutcomes()
	{
		return Stream.of(RecoveryCase.builder().outcome(CodeOutcome.VALID).build(),
				RecoveryCase.builder().outcome(CodeOutcome.INVALID_THEN_VALID).build(),
				RecoveryCase.builder().outcome(CodeOutcome.EXPIRED).build());
	}

	private enum CodeOutcome {VALID, INVALID_THEN_VALID, EXPIRED}

	private record RecoveryCase(CodeOutcome outcome)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private CodeOutcome outcome;

			Builder outcome(CodeOutcome value)
			{
				outcome = value;
				return this;
			}

			RecoveryCase build()
			{
				return new RecoveryCase(outcome);
			}
		}
	}
}
