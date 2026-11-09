package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.EndUserTestBase;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.authn.RememberMeProcessor;
import pl.edu.icm.unity.engine.api.token.TokensManagement;

@Tag("home")
class TrustedDeviceTest extends EndUserTestBase
{
	@Autowired
	@Qualifier("insecure")
	private RealmsManagement realms;
	@Autowired
	private TokensManagement tokens;

	@Test
	void shouldRememberBrowserAndRequirePasswordAfterDeviceRevocation() throws Exception
	{
		TestUser user = createUser("Remembered device owner");
		AuthenticationRealm realm = realms.getRealm("ui");
		realm.setRememberMePolicy(RememberMePolicy.allowForWholeAuthn);
		realm.setAllowForRememberMeDays(7);
		realms.updateRealm(realm);
		deployHomeAt("/trust-home", "");
		VaadinPage page = new VaadinPage(driver);
		driver.get(baseUrl + "/trust-home");
		page.check(By.className("u-authn-rememberMe"), true);
		page.signIn(user.username(), user.password());
		page.visible(By.id("MainHeader.loggedAs"));
		Cookie initialCookie = rememberMeCookie(driver);
		assertThat(tokens.getOwnedTokens(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE, user.entity())).hasSize(1);

		WebDriver restarted = browserWithRememberedCookie(initialCookie);
		VaadinPage trusted = new VaadinPage(restarted);
		assertThat(trusted.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		restarted.get(baseUrl + "/trust-home/trusted-device");
		trusted.click(testId("TrustedDeviceView.device"));
		assertThat(trusted.visible(testId("TrustedDeviceView.device")).getText()).contains("127.0.0.1");
		Cookie rotatedCookie = rememberMeCookie(restarted);
		assertThat(rotatedCookie.getValue()).isNotEqualTo(initialCookie.getValue());
		trusted.click(testId("TrustedDeviceView.revoke"));
		waitFor(() -> restarted.findElements(testId("TrustedDeviceView.device")).isEmpty());
		assertThat(tokens.getOwnedTokens(RememberMeProcessor.REMEMBER_ME_TOKEN_TYPE, user.entity())).isEmpty();
		restarted.navigate().refresh();
		assertThat(restarted.findElements(testId("TrustedDeviceView.device"))).isEmpty();

		WebDriver revoked = browserWithRememberedCookie(rotatedCookie);
		VaadinPage revokedLogin = new VaadinPage(revoked);
		revokedLogin.visible(By.className("u-passwordUsernameField"));
		assertThat(revoked.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		revokedLogin.signIn(user.username(), user.password());
		assertThat(revokedLogin.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
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
		browser.get(baseUrl + "/trust-home");
		browser.manage().deleteAllCookies();
		browser.manage().addCookie(cookie);
		browser.get(baseUrl + "/trust-home");
		return browser;
	}
}
