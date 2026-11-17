package io.imunity.test.ui.home;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.EndpointManagement;

abstract class HomeWorkflowTestBase extends UserAccountTestBase
{
	static final String HOME_PATH = "/workflow-home";
	private final String endpointName = "Home workflow " + UUID.randomUUID();
	private boolean deployed;

	@Autowired
	@Qualifier("insecure")
	private EndpointManagement endpoints;

	@AfterEach
	void removeEndpoint() throws Exception
	{
		if (deployed)
			endpoints.removeEndpoint(endpointName);
	}

	final void deployHome(String settings) throws Exception
	{
		EndpointConfiguration template = endpoints.getEndpoint("Selenium Home").getConfiguration();
		endpoints.deploy("UserHomeUI", endpointName, HOME_PATH, new EndpointConfiguration(
				new I18nString(endpointName), "End-user workflow fixture", template.getAuthenticationOptions(),
				template.getConfiguration() + "\n" + settings, template.getRealm()));
		deployed = true;
	}

	final VaadinPage openProfile(TestUser user)
	{
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, HOME_PATH, user.username(), user.password());
		driver.get(baseUrl + HOME_PATH + "/profile");
		return page;
	}

	final void assertAccepted(TestUser user)
	{
		VaadinPage page = new VaadinPage(newBrowser());
		page.login(baseUrl, "/home", user.username(), user.password());
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}

	final void assertRejected(TestUser user)
	{
		WebDriver browser = newBrowser();
		VaadinPage page = new VaadinPage(browser);
		browser.get(baseUrl + "/home");
		page.signIn(user.username(), user.password());
		page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(browser.getCurrentUrl()).contains("/home/authentication");
		assertThat(browser.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
	}
}
