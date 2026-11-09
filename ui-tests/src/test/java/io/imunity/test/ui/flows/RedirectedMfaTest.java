package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import io.imunity.otp.HashFunction;
import io.imunity.otp.OTPCredential;
import io.imunity.otp.OTPCredentialDefinition;
import io.imunity.otp.OTPGenerationParams;
import io.imunity.otp.OTPResetSettings;
import io.imunity.otp.TOTPCodeGenerator;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.json.JsonUtil;

@Tag("authentication")
class RedirectedMfaTest extends SamlWorkflowTestBase
{
	private static final String SECRET = "JBSWY3DPEHPK3PXP";
	private static final OTPGenerationParams PARAMETERS = new OTPGenerationParams(6, HashFunction.SHA1, 30);
	private static final By CODE = By.className("u-otpCodeField");
	private static final By SUBMIT = By.className("u-otpSignInButton");

	@Test
	void shouldGateRedirectedLoginOnOtpAndAllowRestartAfterMissingAndWrongCodes() throws Exception
	{
		TestUser user = credentialOwner("otp", JsonUtil.toJsonString(new OTPCredentialDefinition(PARAMETERS,
				"Selenium", 1, new OTPResetSettings(false, 6, null, null, OTPResetSettings.ConfirmationMode.EMAIL),
				Optional.empty())));
		userCredentials.setEntityCredential(user.entity(), CREDENTIAL,
				JsonUtil.toJsonString(new OTPCredential(SECRET, PARAMETERS)));
		deploySaml("", List.of());
		authenticators.createAuthenticator("ui-otp", "otp", "", CREDENTIAL);
		flows.addAuthenticationFlow(new AuthenticationFlowDefinition("ui-redirected-mfa", Policy.REQUIRE,
				Set.of("ui-saml"), List.of("ui-otp")));
		deployAt("UserHomeUI", "/redirected-mfa", "", List.of("ui-redirected-mfa"));
		driver.get(baseUrl + "/redirected-mfa");
		VaadinPage page = new VaadinPage(driver);
		page.click(PROVIDER);
		page.signIn(user.username(), user.password());
		page.click(By.className("u-consentConfirmButton"));
		page.visible(CODE);
		page.click(SUBMIT);
		page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		page.fill(CODE, "invalid");
		page.click(SUBMIT);
		waitForUi();
		assertThat(page.value(CODE)).isEmpty();
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();

		page.click(By.className("u-authn-resetMFAButton"));
		page.visible(PROVIDER);
		assertThat(driver.findElements(CODE)).noneMatch(element -> element.isDisplayed());
		page.click(PROVIDER);
		page.click(By.className("u-consentConfirmButton"));
		page.fill(CODE, TOTPCodeGenerator.generateTOTP(SECRET, Instant.now().getEpochSecond(), PARAMETERS));
		page.click(SUBMIT);
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}
}
