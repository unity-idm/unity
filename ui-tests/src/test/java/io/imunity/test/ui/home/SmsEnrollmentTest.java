package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.CredentialWorkflowTestBase;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredential;

@Tag("home")
class SmsEnrollmentTest extends CredentialWorkflowTestBase
{
	private static final String MOBILE = "+48600123456";
	private static final By EDIT = By.cssSelector("[data-testid='HomeSignIn.credential']"
			+ "[data-credential='" + CREDENTIAL + "'] [data-testid='HomeSignIn.editCredential']");
	private static final By CONFIRM = By.cssSelector("[data-testid='MobileNumberConfirmationDialog']"
			+ " vaadin-button[slot='confirm-button']");

	@Test
	void shouldVerifyMobileEnrollCredentialAndAuthenticateWithCapturedSms() throws Exception
	{
		messageTemplate("ui-mobile-verification", "MobileNumberConfirmation", "${confirmationCode}", "default_sms");
		messageTemplate("ui-sms-login", "SMSAuthn", "${code}", "default_sms");
		SMSCredential configuration = new SMSCredential();
		configuration.setMessageTemplate("ui-sms-login");
		configuration.setMobileNumberConfirmationConfiguration(
				new MobileNumberConfirmationConfiguration("ui-mobile-verification"));
		TestUser user = credentialOwner("sms", configuration.getSerializedConfiguration().toString());
		authenticators.createAuthenticator("ui-sms", "sms", "", CREDENTIAL);
		deployAt("UserHomeUI", "/sms-home", "", List.of("ui-sms"));
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/home", user.username(), user.password());
		driver.get(baseUrl + "/home/sign-in");
		page.click(By.cssSelector("vaadin-details:not([opened])"));
		page.click(EDIT);
		page.fill(testId("SMSCredentialEditor.mobile"), MOBILE);
		page.click(testId("HomeSignIn.saveCredential"));
		page.visible(By.cssSelector("[data-testid='SMSCredentialEditor.mobile'] vaadin-text-field[invalid]"));
		assertThat(state(user)).isEqualTo(LocalCredentialState.notSet);
		page.click(By.cssSelector("[data-testid='SMSCredentialEditor.mobile'] .u-verify-button-icon"));
		String confirmationCode = notificationTo(MOBILE).body().strip();
		page.fill(testId("MobileNumberConfirmationDialog.code"), "incorrect");
		page.click(CONFIRM);
		page.visible(By.cssSelector("[data-testid='MobileNumberConfirmationDialog.code'][invalid]"));
		assertThat(state(user)).isEqualTo(LocalCredentialState.notSet);
		screenshot("Mobile verification dialog");
		page.fill(testId("MobileNumberConfirmationDialog.code"), confirmationCode);
		page.click(CONFIRM);
		waitFor(() -> driver.findElements(testId("MobileNumberConfirmationDialog")).stream()
				.noneMatch(element -> element.isDisplayed()));
		page.click(testId("HomeSignIn.saveCredential"));
		waitForElementNotPresent(testId("HomeSignIn.saveCredential"));
		assertThat(state(user)).isEqualTo(LocalCredentialState.correct);
		page.click(EDIT);
		page.fill(testId("SMSCredentialEditor.mobile"), "+48600987654");
		page.click(testId("HomeSignIn.cancelCredential"));
		driver.navigate().refresh();

		WebDriver fresh = newBrowser();
		VaadinPage login = new VaadinPage(fresh);
		fresh.get(baseUrl + "/sms-home");
		int before = notifications.sentTo(MOBILE).size();
		login.fill(By.className("u-smsUsernameField"), user.username());
		login.click(By.className("u-smsSendCodeButton"));
		waitFor(() -> notifications.sentTo(MOBILE).size() > before);
		login.fill(By.className("u-smsCodeField"), "incorrect");
		login.click(By.className("u-smsSignInButton"));
		login.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(fresh.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		int previousMessages = notifications.sentTo(MOBILE).size();
		login.fill(By.className("u-smsUsernameField"), user.username());
		login.click(By.className("u-smsSendCodeButton"));
		waitFor(() -> notifications.sentTo(MOBILE).size() > previousMessages);
		login.fill(By.className("u-smsCodeField"), notificationTo(MOBILE).body().strip());
		login.click(By.className("u-smsSignInButton"));
		assertThat(login.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(notifications.sentTo("+48600987654")).isEmpty();
	}

	private LocalCredentialState state(TestUser user) throws Exception
	{
		return entities.getEntity(user.entity()).getCredentialInfo().getCredentialsState().get(CREDENTIAL).getState();
	}
}
