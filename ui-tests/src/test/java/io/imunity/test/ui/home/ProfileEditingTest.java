package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;

@Tag("home")
class ProfileEditingTest extends UserAccountTestBase
{
	private final String endpointName = "Profile editing " + UUID.randomUUID();
	private AttributeType originalNameType;
	private boolean deployed;

	@Autowired
	@Qualifier("insecure")
	private EndpointManagement endpoints;
	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;

	@AfterEach
	void restoreConfiguration() throws Exception
	{
		try
		{
			if (deployed)
				endpoints.removeEndpoint(endpointName);
		} finally
		{
			if (originalNameType != null)
				types.updateAttributeType(originalNameType);
		}
	}

	@Test
	void shouldSaveReloadAndResetEditableProfileWhileProtectingReadOnlyRole() throws Exception
	{
		TestUser user = createUser("Profile owner");
		configureProfile();
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/profile-home", user.username(), user.password());
		driver.get(baseUrl + "/profile-home/profile");
		assertProtectedRole(page);
		assertThat(page.value(attribute("name"))).isEqualTo(user.displayName());
		page.fill(attribute("name"), "Saved profile name");
		page.click(testId("ProfileView.save"));
		waitForElementNotPresent(testId("ProfileView.save"));
		assertThat(storedName(user)).isEqualTo("Saved profile name");
		driver.navigate().refresh();
		assertThat(page.value(attribute("name"))).isEqualTo("Saved profile name");
		assertProtectedRole(page);

		page.fill(attribute("name"), "Unsaved profile name");
		page.click(testId("ProfileView.reset"));
		waitForElementNotPresent(testId("ProfileView.reset"));
		assertThat(page.value(attribute("name"))).isEqualTo("Saved profile name");
		assertThat(storedName(user)).isEqualTo("Saved profile name");
		assertProtectedRole(page);
		assertThat(attributes.getAttributes(user.entity(), "/", "sys:AuthorizationRole"))
				.singleElement().satisfies(role -> assertThat(role.getValues()).containsExactly("Regular User"));
		screenshot("Saved profile and protected role");
	}

	private void configureProfile() throws Exception
	{
		originalNameType = types.getAttributeTypesAsMap().get("name");
		AttributeType editable = new AttributeType(originalNameType.toJson());
		editable.setSelfModificable(true);
		types.updateAttributeType(editable);
		EndpointConfiguration template = endpoints.getEndpoint("Selenium Home").getConfiguration();
		String configuration = template.getConfiguration() + """

				unity.userhome.attributes.1.attribute=name
				unity.userhome.attributes.1.group=/
				unity.userhome.attributes.1.editable=true
				unity.userhome.attributes.2.attribute=sys:AuthorizationRole
				unity.userhome.attributes.2.group=/
				unity.userhome.attributes.2.editable=true
				""";
		endpoints.deploy("UserHomeUI", endpointName, "/profile-home", new EndpointConfiguration(
				new I18nString(endpointName), "Profile fixture", template.getAuthenticationOptions(),
				configuration, template.getRealm()));
		deployed = true;
	}

	private String storedName(TestUser user) throws Exception
	{
		return attributes.getAttributes(user.entity(), "/", "name").iterator().next().getValues().getFirst().toString();
	}

	private void assertProtectedRole(VaadinPage page)
	{
		assertThat(page.value(attribute("sys:AuthorizationRole"))).isEqualTo("Regular User");
		assertThat(page.visible(attribute("sys:AuthorizationRole"))
				.findElements(By.cssSelector("input:not([readonly]):not([disabled]), vaadin-button"))).isEmpty();
	}

	private By attribute(String name)
	{
		return By.cssSelector("[data-testid='ProfileView.attribute'][data-attribute-name='" + name
				+ "'][data-group-path='/']");
	}
}
