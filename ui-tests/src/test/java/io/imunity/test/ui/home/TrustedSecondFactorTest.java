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
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.otp.HashFunction;
import io.imunity.otp.OTPCredential;
import io.imunity.otp.OTPCredentialDefinition;
import io.imunity.otp.OTPGenerationParams;
import io.imunity.otp.OTPResetSettings;
import io.imunity.otp.TOTPCodeGenerator;
import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.CredentialWorkflowTestBase;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.engine.api.RealmsManagement;

@Tag("home")
class TrustedSecondFactorTest extends CredentialWorkflowTestBase
{
	private static final String SECRET = "JBSWY3DPEHPK3PXP";
	private static final OTPGenerationParams PARAMETERS = new OTPGenerationParams(6, HashFunction.SHA1, 30);

	@Autowired
	@Qualifier("insecure")
	private RealmsManagement realms;

	@Test
	void shouldRequireOtpAgainAfterRevokingRememberedSecondFactor() throws Exception
	{
		TestUser user = credentialOwner("otp", JsonUtil.toJsonString(new OTPCredentialDefinition(PARAMETERS,
				"Selenium", 1, new OTPResetSettings(false, 6, null, null, OTPResetSettings.ConfirmationMode.EMAIL),
				Optional.empty())));
		userCredentials.setEntityCredential(user.entity(), CREDENTIAL,
				JsonUtil.toJsonString(new OTPCredential(SECRET, PARAMETERS)));
		authenticators.createAuthenticator("ui-otp", "otp", "", CREDENTIAL);
		flows.addAuthenticationFlow(new AuthenticationFlowDefinition("ui-trusted-mfa", Policy.REQUIRE,
				Set.of("pwdSys"), List.of("ui-otp")));
		var realm = realms.getRealm("ui");
		realm.setRememberMePolicy(RememberMePolicy.allowFor2ndFactor);
		realm.setAllowForRememberMeDays(7);
		realms.updateRealm(realm);
		deployAt("UserHomeUI", "/trusted-mfa", "", List.of("ui-trusted-mfa"));
		driver.get(baseUrl + "/trusted-mfa");
		VaadinPage page = new VaadinPage(driver);
		page.signIn(user.username(), user.password());
		page.check(By.className("u-authn-rememberMe"), true);
		page.fill(By.className("u-otpCodeField"), code());
		page.click(By.className("u-otpSignInButton"));
		page.visible(By.id("MainHeader.loggedAs"));
		Cookie initialCookie = rememberMeCookie(driver);
		WebDriver remembered = browserWithRememberedCookie(initialCookie);
		VaadinPage trusted = new VaadinPage(remembered);
		trusted.signIn(user.username(), user.password());
		assertThat(trusted.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(remembered.findElements(By.className("u-otpCodeField"))).isEmpty();
		remembered.get(baseUrl + "/trusted-mfa/trusted-device");
		trusted.click(testId("TrustedDeviceView.device"));
		Cookie rotatedCookie = rememberMeCookie(remembered);
		assertThat(rotatedCookie.getValue()).isNotEqualTo(initialCookie.getValue());
		trusted.click(testId("TrustedDeviceView.revoke"));
		waitFor(() -> remembered.findElements(testId("TrustedDeviceView.device")).isEmpty());

		WebDriver revoked = browserWithRememberedCookie(rotatedCookie);
		VaadinPage login = new VaadinPage(revoked);
		login.signIn(user.username(), user.password());
		login.visible(By.className("u-otpCodeField"));
		assertThat(revoked.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		screenshot(revoked, "Second factor after device revocation");
	}

	private Cookie rememberMeCookie(WebDriver browser)
	{
		List<Cookie> cookies = browser.manage().getCookies().stream()
				.filter(cookie -> cookie.getName().startsWith("REMEMBERME_")).toList();
		assertThat(cookies).hasSize(1);
		return cookies.getFirst();
	}

	private WebDriver browserWithRememberedCookie(Cookie cookie)
	{
		WebDriver browser = newBrowser();
		browser.get(baseUrl + "/trusted-mfa");
		browser.manage().deleteAllCookies();
		browser.manage().addCookie(cookie);
		browser.get(baseUrl + "/trusted-mfa");
		return browser;
	}

	private String code()
	{
		return TOTPCodeGenerator.generateTOTP(SECRET, Instant.now().getEpochSecond(), PARAMETERS);
	}
}
