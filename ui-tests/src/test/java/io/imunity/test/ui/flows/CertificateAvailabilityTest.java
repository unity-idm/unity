package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import io.imunity.test.ui.VaadinPage;

@Tag("authentication")
class CertificateAvailabilityTest extends CredentialWorkflowTestBase
{
	@Test
	void shouldProtectCertificateCredentialAndAllowPasswordFallbackWithoutClientCertificate() throws Exception
	{
		TestUser user = credentialOwner("certificate", "");
		authenticators.createAuthenticator("ui-certificate", "certificate", "", CREDENTIAL);
		deployAt("UserHomeUI", "/certificate-home", "", List.of("ui-certificate", "pwdSys"));
		driver.get(baseUrl + "/certificate-home");
		VaadinPage page = new VaadinPage(driver);
		page.visible(By.className("u-passwordUsernameField"));
		assertThat(driver.findElements(By.className("u-x509SignInButton")))
				.noneMatch(element -> element.isDisplayed() && element.isEnabled());
		page.signIn(user.username(), user.password());
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		driver.get(baseUrl + "/certificate-home/sign-in");
		By certificate = By.cssSelector("[data-testid='HomeSignIn.credential'][data-credential='" + CREDENTIAL + "']");
		page.visible(certificate);
		assertThat(driver.findElement(certificate).findElements(
				By.cssSelector("[data-testid='HomeSignIn.editCredential']"))).isEmpty();
		driver.navigate().refresh();
		page.visible(certificate);
		assertThat(driver.findElement(certificate).findElements(
				By.cssSelector("[data-testid='HomeSignIn.editCredential']"))).isEmpty();
		screenshot("Certificate credential controls");
	}
}
