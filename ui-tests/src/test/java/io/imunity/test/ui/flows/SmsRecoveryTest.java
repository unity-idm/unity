package io.imunity.test.ui.flows;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.base.verifiable.VerifiableMobileNumber;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttribute;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredential;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;

@Tag("authentication")
class SmsRecoveryTest extends CredentialWorkflowTestBase
{
	private static final String OLD_MOBILE = "+48600123456";
	private static final String NEW_MOBILE = "+48600987654";
	private static final String EMAIL = "recovery@example.org";

	@Test
	void shouldRecoverMobileUsingCapturedEmailAndAuthenticateWithReplacementNumber() throws Exception
	{
		messageTemplate("ui-sms-reset", "EmailPasswordResetCode", "${code}", "default_email");
		messageTemplate("ui-sms-authentication", "SMSAuthn", "${code}", "default_sms");
		SMSCredentialRecoverySettings recovery = new SMSCredentialRecoverySettings();
		recovery.setEnabled(true);
		recovery.setCapchaRequire(false);
		recovery.setEmailSecurityCodeMsgTemplate("ui-sms-reset");
		SMSCredential configuration = new SMSCredential();
		configuration.setMessageTemplate("ui-sms-authentication");
		configuration.setRecoverySettings(recovery);
		TestUser user = credentialOwner("sms", configuration.getSerializedConfiguration().toString());
		userCredentials.setEntityCredential(user.entity(), CREDENTIAL, OLD_MOBILE);
		attributes.setAttribute(user.entity(), VerifiableEmailAttribute.of("email", "/",
				new VerifiableEmail(EMAIL, new ConfirmationInfo(true))));
		attributes.setAttribute(user.entity(), VerifiableMobileNumberAttribute.of("mobile", "/",
				new VerifiableMobileNumber(NEW_MOBILE, new ConfirmationInfo(true))));
		authenticators.createAuthenticator("ui-sms-recovery", "sms", "", CREDENTIAL);
		deployAt("UserHomeUI", "/recover-mobile", "", List.of("ui-sms-recovery"));
		driver.get(baseUrl + "/recover-mobile");
		VaadinPage page = new VaadinPage(driver);
		page.click(By.xpath("//span[normalize-space(.)='Lost phone?']"));
		page.fill(testId("SMSResetStep1Captcha.username"), user.username());
		page.click(By.className("u-cred-reset-proceed"));
		String code = notificationTo(EMAIL).body().strip();
		page.fill(testId("CredentialResetCodeVerificationUI.code"), "incorrect");
		page.click(By.className("u-cred-reset-proceed"));
		page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(driver.findElements(testId("SMSCredentialEditor.existingMobile"))).isEmpty();
		page.fill(testId("CredentialResetCodeVerificationUI.code"), code);
		page.click(By.className("u-cred-reset-proceed"));
		assertThat(page.value(testId("SMSCredentialEditor.existingMobile"))).isEqualTo(NEW_MOBILE);
		page.click(By.className("u-cred-reset-proceed"));
		waitFor(() -> page.visible(By.className("u-passwordResetTitle")).getText().contains("Update was successful"));

		WebDriver fresh = newBrowser();
		VaadinPage login = new VaadinPage(fresh);
		fresh.get(baseUrl + "/recover-mobile");
		login.fill(By.className("u-smsUsernameField"), user.username());
		login.click(By.className("u-smsSendCodeButton"));
		login.fill(By.className("u-smsCodeField"), notificationTo(NEW_MOBILE).body().strip());
		login.click(By.className("u-smsSignInButton"));
		assertThat(login.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(notifications.sentTo(OLD_MOBILE)).isEmpty();
	}
}
