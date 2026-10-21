package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;

@Tag("admin-authentication")
class RealmTest extends UserAccountTestBase
{
	private final String name = "ui-realm-" + UUID.randomUUID();
	private final String endpointName = "Realm service " + UUID.randomUUID();
	private boolean deployed;

	@Autowired
	@Qualifier("insecure")
	private RealmsManagement realms;
	@Autowired
	@Qualifier("insecure")
	private EndpointManagement endpoints;

	@AfterEach
	void removeFixtures() throws Exception
	{
		try
		{
			if (deployed)
				endpoints.removeEndpoint(endpointName);
		} finally
		{
			if (realms.getRealms().stream().anyMatch(realm -> realm.getName().equals(name)))
				realms.removeRealm(name);
		}
	}

	@ParameterizedTest(name = "remember-me policy: {0}")
	@MethodSource("realmCases")
	void shouldPersistRealmSettingsAndShowAssociatedService(RealmCase scenario) throws Exception
	{
		TestUser user = createUser("Realm member");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/realms/edit");
		page.fill(testId("RealmEditView.name"), name);
		page.fill(testId("RealmEditView.description"), "Disposable authentication realm");
		page.fill(testId("RealmEditView.blockAfterUnsuccessfulLogins"), "4");
		page.fill(testId("RealmEditView.blockFor"), "9");
		page.fill(testId("RealmEditView.allowForRememberMeDays"), "7");
		page.fill(testId("RealmEditView.maxInactivity"), "420");
		page.select(testId("RealmEditView.rememberMePolicy"), scenario.policy().name());
		save();
		assertStored(scenario.policy(), 420);

		EndpointConfiguration template = endpoints.getEndpoint("Selenium Home").getConfiguration();
		endpoints.deploy("UserHomeUI", endpointName, "/realm-home", new EndpointConfiguration(
				new I18nString(endpointName), "Disposable service", template.getAuthenticationOptions(),
				template.getConfiguration(), name));
		deployed = true;
		driver.get(baseUrl + "/admin/realms/edit/" + name);
		assertThat(page.visible(testId("RealmEditView.endpoints")).getText()).contains(endpointName);
		assertThat(page.value(testId("RealmEditView.name"))).isEqualTo(name);
		assertThat(page.value(testId("RealmEditView.blockAfterUnsuccessfulLogins"))).isEqualTo("4");
		assertThat(page.value(testId("RealmEditView.blockFor"))).isEqualTo("9");
		assertThat(page.value(testId("RealmEditView.allowForRememberMeDays"))).isEqualTo("7");
		assertThat(page.value(testId("RealmEditView.maxInactivity"))).isEqualTo("420");
		assertThat(page.value(testId("RealmEditView.rememberMePolicy"))).isEqualTo(scenario.policy().name());
		page.fill(testId("RealmEditView.maxInactivity"), "540");
		save();
		driver.get(baseUrl + "/admin/realms/edit/" + name);
		assertThat(page.value(testId("RealmEditView.maxInactivity"))).isEqualTo("540");
		assertStored(scenario.policy(), 540);
		assertThat(endpoints.getEndpoint(endpointName).getConfiguration().getRealm()).isEqualTo(name);

		WebDriver browser = newBrowser();
		VaadinPage home = new VaadinPage(browser);
		home.login(baseUrl, "/realm-home", user.username(), user.password());
		assertThat(home.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}

	private void save()
	{
		new VaadinPage(driver).click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/realms");
	}

	private void assertStored(RememberMePolicy policy, int inactivity) throws Exception
	{
		AuthenticationRealm stored = realms.getRealm(name);
		assertThat(stored.getDescription()).isEqualTo("Disposable authentication realm");
		assertThat(stored.getBlockAfterUnsuccessfulLogins()).isEqualTo(4);
		assertThat(stored.getBlockFor()).isEqualTo(9);
		assertThat(stored.getAllowForRememberMeDays()).isEqualTo(7);
		assertThat(stored.getMaxInactivity()).isEqualTo(inactivity);
		assertThat(stored.getRememberMePolicy()).isEqualTo(policy);
	}

	private static Stream<RealmCase> realmCases()
	{
		return Stream.of(RememberMePolicy.values()).map(policy -> RealmCase.builder().policy(policy).build());
	}

	record RealmCase(RememberMePolicy policy)
	{
		static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return policy.name();
		}

		static final class Builder
		{
			private RememberMePolicy policy;

			Builder policy(RememberMePolicy policy)
			{
				this.policy = policy;
				return this;
			}

			RealmCase build()
			{
				return new RealmCase(policy);
			}
		}
	}
}
