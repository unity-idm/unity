package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import io.imunity.otp.HashFunction;
import io.imunity.otp.OTPCredentialDefinition;
import io.imunity.otp.OTPGenerationParams;
import io.imunity.otp.OTPResetSettings;
import io.imunity.otp.TOTPCodeGenerator;
import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.CredentialWorkflowTestBase;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.json.JsonUtil;

@Tag("home")
class OtpEnrollmentTest extends CredentialWorkflowTestBase
{
	private static final OTPGenerationParams PARAMETERS = new OTPGenerationParams(6, HashFunction.SHA1, 30);
	private static final By EDIT = By.cssSelector("[data-testid='HomeSignIn.credential']"
			+ "[data-credential='" + CREDENTIAL + "'] [data-testid='HomeSignIn.editCredential']");

	@Test
	void shouldEnrollOtpAndEnforceUserMfaOptInAtNextLogin() throws Exception
	{
		TestUser user = credentialOwner("otp", JsonUtil.toJsonString(new OTPCredentialDefinition(PARAMETERS,
				"Selenium", 1, new OTPResetSettings(false, 6, null, null, OTPResetSettings.ConfirmationMode.EMAIL),
				Optional.empty())));
		authenticators.createAuthenticator("ui-otp", "otp", "", CREDENTIAL);
		flows.addAuthenticationFlow(new AuthenticationFlowDefinition("ui-optin", Policy.USER_OPTIN,
				Set.of("pwdSys"), List.of("ui-otp")));
		deployAt("UserHomeUI", "/mfa-home", "", List.of("ui-optin"));
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/mfa-home", user.username(), user.password());
		driver.get(baseUrl + "/mfa-home/sign-in");
		page.click(By.cssSelector("vaadin-details:not([opened])"));
		page.click(EDIT);
		page.click(testId("OTPEditorComponent.showSecret"));
		String secret = page.visible(testId("OTPEditorComponent.secret")).getText().replace(" ", "");
		page.fill(By.className("u-otp-verification-code"), "invalid");
		page.click(By.className("u-otp-verification-button"));
		page.visible(By.cssSelector(".u-otp-verification-code[invalid]"));
		assertThat(state(user)).isEqualTo(LocalCredentialState.notSet);
		page.fill(By.className("u-otp-verification-code"), code(secret));
		page.click(By.className("u-otp-verification-button"));
		page.click(testId("HomeSignIn.saveCredential"));
		waitForElementNotPresent(testId("HomeSignIn.saveCredential"));
		assertThat(state(user)).isEqualTo(LocalCredentialState.correct);
		assertThat(userCredentials.getUserMFAOptIn(user.entity())).isFalse();
		page.check(testId("HomeSignIn.mfaOptIn"), true);
		waitForUi();
		assertThat(userCredentials.getUserMFAOptIn(user.entity())).isTrue();
		driver.navigate().refresh();
		assertThat(page.visible(testId("HomeSignIn.mfaOptIn")).getDomProperty("checked")).isEqualTo("true");

		WebDriver fresh = newBrowser();
		VaadinPage login = new VaadinPage(fresh);
		fresh.get(baseUrl + "/mfa-home");
		login.signIn(user.username(), user.password());
		login.visible(By.className("u-otpCodeField"));
		assertThat(fresh.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		login.fill(By.className("u-otpCodeField"), "invalid");
		login.click(By.className("u-otpSignInButton"));
		login.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(fresh.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		login.fill(By.className("u-otpCodeField"), code(secret));
		login.click(By.className("u-otpSignInButton"));
		assertThat(login.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());

		fresh.get(baseUrl + "/mfa-home/sign-in");
		login.check(testId("HomeSignIn.mfaOptIn"), false);
		waitForUi();
		assertThat(userCredentials.getUserMFAOptIn(user.entity())).isFalse();
		new VaadinPage(newBrowser()).login(baseUrl, "/mfa-home", user.username(), user.password());
	}

	private LocalCredentialState state(TestUser user) throws Exception
	{
		return entities.getEntity(user.entity()).getCredentialInfo().getCredentialsState().get(CREDENTIAL).getState();
	}

	private String code(String secret)
	{
		return TOTPCodeGenerator.generateTOTP(secret, Instant.now().getEpochSecond(), PARAMETERS);
	}
}
