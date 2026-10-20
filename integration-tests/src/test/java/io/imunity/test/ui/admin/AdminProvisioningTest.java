package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;

@Tag("admin-directory")
class AdminProvisioningTest extends SeleniumTestBase
{
	private final String suffix = UUID.randomUUID().toString();
	private final String group = "/ui-directory-" + suffix;
	private final String username = "ui-user-" + suffix;
	private static final String PASSWORD = "Provisioned!58-Harbor";
	private static final String DISPLAY_NAME = "UI provisioned member";
	private EntityParam createdUser;
	private boolean groupCreated;

	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groups;
	@Autowired
	@Qualifier("insecure")
	private EntityManagement entities;
	@Autowired
	@Qualifier("insecure")
	private AttributesManagement attributes;

	@BeforeEach
	void prepareGroup() throws Exception
	{
		groups.addGroup(new Group(group));
		groupCreated = true;
	}

	@AfterEach
	void removeFixture() throws Exception
	{
		try
		{
			if (createdUser != null)
				entities.removeEntity(createdUser);
		} finally
		{
			if (groupCreated)
				groups.removeGroup(group, true);
		}
	}

	@Test
	void shouldProvisionUserWithCredentialGroupAndAttribute() throws Exception
	{
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin/directory-browser", "ui-admin", "Ui-admin!42");
		DirectoryPage directory = new DirectoryPage(driver);
		directory.selectGroup(group);

		directory.createUser(username, PASSWORD, DISPLAY_NAME);
		waitForElementNotPresent(testId("EntityCreationDialog"));
		createdUser = new EntityParam(new IdentityTaV("userName", username));
		driver.navigate().refresh();
		directory.selectGroup(group);
		waitFor(() -> directory.directoryText().contains(DISPLAY_NAME));

		Entity storedUser = entities.getEntity(createdUser);
		assertThat(storedUser.getIdentities()).anySatisfy(identity ->
		{
			assertThat(identity.getTypeId()).isEqualTo("userName");
			assertThat(identity.getValue()).isEqualTo(username);
		});
		assertThat(entities.getGroups(createdUser)).containsKey(group);
		assertThat(attributes.getAttributes(createdUser, "/", "name"))
				.singleElement().satisfies(attribute ->
						assertThat(attribute.getValues()).containsExactly(DISPLAY_NAME));
		assertThat(storedUser.getCredentialInfo().getCredentialsState().get("sys:password").getState())
				.isEqualTo(LocalCredentialState.correct);

		WebDriver userBrowser = newBrowser();
		VaadinPage userHome = new VaadinPage(userBrowser);
		userHome.login(baseUrl, "/home", username, PASSWORD);
		assertThat(userHome.visible(By.id("MainHeader.loggedAs")).getText()).contains(DISPLAY_NAME);
	}
}
