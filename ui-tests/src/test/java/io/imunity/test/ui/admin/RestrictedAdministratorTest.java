package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;

@Tag("admin-directory")
class RestrictedAdministratorTest extends UserAccountTestBase
{
	@Autowired
	@Qualifier("insecure")
	private EndpointManagement endpoints;

	@Test
	void shouldPermitContentsManagementAndDenyDirectServiceConfiguration() throws Exception
	{
		TestUser manager = createUser("Restricted contents manager");
		TestUser target = createUser("Restricted administrator target");
		attributes.setAttribute(manager.entity(), EnumAttribute.of("sys:AuthorizationRole", "/", "Contents Manager"));
		var configuration = endpoints.getEndpoint("Selenium Home").getConfiguration();
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin/directory-browser", manager.username(), manager.password());
		new DirectoryPage(driver).selectGroup("/");
		AccountAdministrationPage directory = new AccountAdministrationPage(driver);
		directory.changeState(target.entity(), "DISABLED");
		waitForElementNotPresent(testId("ChangeEntityStateDialog"));
		assertThat(entities.getEntity(target.entity()).getState()).isEqualTo(EntityState.disabled);

		driver.get(baseUrl + "/admin/services/edit/Selenium%20Home");
		page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(driver.findElements(testId("EditViewActionLayoutFactory.save")))
				.noneMatch(element -> element.isDisplayed() && element.isEnabled());
		assertThat(endpoints.getEndpoint("Selenium Home").getConfiguration()).isEqualTo(configuration);
		driver.get(baseUrl + "/admin/directory-browser");
		new DirectoryPage(driver).selectGroup("/");
		directory.changeState(target.entity(), "ENABLED");
		waitForElementNotPresent(testId("ChangeEntityStateDialog"));
		assertThat(entities.getEntity(target.entity()).getState()).isEqualTo(EntityState.valid);
	}
}
