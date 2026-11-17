package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import io.imunity.test.ui.VaadinPage;

public final class DirectoryPage
{
	private static final String ENTITY_DIALOG = "[data-testid='EntityCreationDialog']";
	private static final String PASSWORD_PANEL = ENTITY_DIALOG + " [data-credential='sys:password']";
	private final VaadinPage page;

	public DirectoryPage(WebDriver driver)
	{
		page = new VaadinPage(driver);
	}

	public void selectGroup(String path)
	{
		page.click(By.cssSelector("[data-testid='GroupsTreeGrid'] [data-group-path='" + path + "'] vaadin-icon"));
		page.visible(testId("IdentitiesPanel"));
	}

	public void createUser(String username, String password, String name)
	{
		page.click(By.cssSelector("[data-testid='IdentitiesPanel'] vaadin-icon[icon='vaadin:menu']"));
		page.click(testId("IdentitiesPanel.createEntity"));
		page.select(testId("EntityCreationDialog", "IdentityCreationDialog.identityType"), "userName");
		page.fill(By.cssSelector(ENTITY_DIALOG + " [id='UsernameIdentityEditor.username']"), username);
		page.fill(By.cssSelector(ENTITY_DIALOG + " [data-attribute-name='name']"), name);
		page.click(testId("EntityCreationDialog", "EntityCreationDialog.credentialsTab"));
		page.select(testId("EntityCreationDialog", "EntityCreationDialog.credentialRequirement"), "ui-password");
		page.check(credentialControl("setInitialCredential"), true);
		page.fill(By.cssSelector(PASSWORD_PANEL + " .u-password-setup"), password);
		page.fill(By.cssSelector(PASSWORD_PANEL + " .u-password-repeat"), password);
		page.check(credentialControl("requireChange"), false);
		page.click(testId("EntityCreationDialog", "DialogWithActionFooter.confirm"));
	}

	public String directoryText()
	{
		return page.visible(testId("IdentitiesPanel")).getText();
	}

	private static By credentialControl(String name)
	{
		return By.cssSelector(PASSWORD_PANEL + " [data-testid='SingleCredentialPanel." + name + "']");
	}
}
