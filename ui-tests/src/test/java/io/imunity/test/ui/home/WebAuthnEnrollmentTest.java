package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.virtualauthenticator.Credential;
import org.openqa.selenium.virtualauthenticator.HasVirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions.Protocol;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions.Transport;

import io.imunity.fido.credential.FidoCredential;
import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.CredentialWorkflowTestBase;
import pl.edu.icm.unity.base.authn.LocalCredentialState;

@Tag("home")
class WebAuthnEnrollmentTest extends CredentialWorkflowTestBase
{
	private static final By EDIT = By.cssSelector("[data-testid='HomeSignIn.credential']"
			+ "[data-credential='" + CREDENTIAL + "'] [data-testid='HomeSignIn.editCredential']");

	@Test
	void shouldEnrollAuthenticateCancelChangesAndRevokeVirtualSecurityKey() throws Exception
	{
		FidoCredential configuration = new FidoCredential();
		configuration.setAttestationConveyance(com.yubico.webauthn.data.AttestationConveyancePreference.NONE.name());
		configuration.setUserVerification(com.yubico.webauthn.data.UserVerificationRequirement.PREFERRED.name());
		TestUser user = credentialOwner("fido", configuration.serialize());
		authenticators.createAuthenticator("ui-fido", "fido", "fido.name=Security key\n", CREDENTIAL);
		deployAt("UserHomeUI", "/key-home", "", List.of("ui-fido"));
		VirtualAuthenticator firstKey = authenticator(driver);
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/home", user.username(), user.password());
		driver.get(baseUrl + "/home/sign-in");
		page.click(By.cssSelector("vaadin-details:not([opened])"));
		page.click(EDIT);
		page.click(testId("FidoEditorComponent.register"));
		page.fill(testId("FidoPreviewComponent.description"), "First virtual key");
		page.click(testId("HomeSignIn.saveCredential"));
		waitForElementNotPresent(testId("HomeSignIn.saveCredential"));
		assertThat(entities.getEntity(user.entity()).getCredentialInfo().getCredentialsState()
				.get(CREDENTIAL).getState()).isEqualTo(LocalCredentialState.correct);
		List<Credential> firstCredentials = firstKey.getCredentials();
		assertThat(firstCredentials).hasSize(1);
		assertKeyLogin(user, firstCredentials);

		page.click(EDIT);
		page.fill(testId("FidoPreviewComponent.description"), "Unsaved key description");
		page.click(testId("HomeSignIn.cancelCredential"));
		driver.navigate().refresh();
		page.click(EDIT);
		assertThat(page.value(testId("FidoPreviewComponent.description"))).isEqualTo("First virtual key");
		((HasVirtualAuthenticator) driver).removeVirtualAuthenticator(firstKey);
		VirtualAuthenticator secondKey = authenticator(driver);
		page.click(testId("FidoEditorComponent.register"));
		waitFor(() -> driver.findElements(testId("FidoPreviewComponent.description")).size() == 2);
		page.click(testId("HomeSignIn.saveCredential"));
		waitForElementNotPresent(testId("HomeSignIn.saveCredential"));
		List<Credential> secondCredentials = secondKey.getCredentials();
		assertThat(secondCredentials).hasSize(1);
		page.click(EDIT);
		page.click(testId("FidoPreviewComponent.remove"));
		page.click(testId("HomeSignIn.saveCredential"));
		waitForElementNotPresent(testId("HomeSignIn.saveCredential"));
		driver.navigate().refresh();
		page.click(EDIT);
		page.visible(testId("FidoPreviewComponent.description"));
		assertThat(driver.findElements(testId("FidoPreviewComponent.description"))).hasSize(1);
		assertKeyLogin(user, secondCredentials);

		WebDriver revoked = newBrowser();
		VirtualAuthenticator removedKey = authenticator(revoked);
		firstCredentials.forEach(removedKey::addCredential);
		VaadinPage login = new VaadinPage(revoked);
		revoked.get(baseUrl + "/key-home");
		login.fill(By.className("u-passwordUsernameField"), user.username());
		login.click(By.className("u-fidoSignInButton"));
		login.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(revoked.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
	}

	private void assertKeyLogin(TestUser user, List<Credential> credentials)
	{
		WebDriver browser = newBrowser();
		VirtualAuthenticator key = authenticator(browser);
		credentials.forEach(key::addCredential);
		VaadinPage login = new VaadinPage(browser);
		browser.get(baseUrl + "/key-home");
		login.fill(By.className("u-passwordUsernameField"), user.username());
		login.click(By.className("u-fidoSignInButton"));
		assertThat(login.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}

	private VirtualAuthenticator authenticator(WebDriver browser)
	{
		return ((HasVirtualAuthenticator) browser).addVirtualAuthenticator(new VirtualAuthenticatorOptions()
				.setProtocol(Protocol.CTAP2).setTransport(Transport.INTERNAL).setHasResidentKey(true)
				.setHasUserVerification(true).setIsUserVerified(true).setIsUserConsenting(true));
	}
}
