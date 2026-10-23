package io.imunity.test.ui.upman;

import static io.imunity.test.ui.VaadinPage.testId;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;

final class ProjectPage
{
	private final VaadinPage page;
	private final WebDriverWait wait;

	ProjectPage(WebDriver driver)
	{
		page = new VaadinPage(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(SeleniumTestBase.WAIT_TIME_S));
	}

	void selectProject(String label)
	{
		page.select(testId("UpManMenu.project"), label);
	}

	void selectGroup(String label)
	{
		page.select(testId("UpManMembers.group"), label);
	}

	void filter(String value)
	{
		page.fill(testId("UpManMembers.search"), value);
	}

	WebElement grid()
	{
		return page.visible(testId("UpManMembers.grid"));
	}

	void action(long entityId, String action)
	{
		page.click(memberActions(entityId));
		page.click(testId("UpManMembers." + action));
	}

	void setRole(long entityId, boolean subproject, String role)
	{
		action(entityId, subproject ? "setSubprojectRole" : "setProjectRole");
		String owner = subproject ? "UpManMembers.subprojectRoleDialog" : "UpManMembers.projectRoleDialog";
		page.visible(testId(owner, "UpManMembers.role")).findElements(By.tagName("vaadin-radio-button"))
				.stream().filter(option -> option.getText().equals(role)).findFirst().orElseThrow().click();
		page.click(testId(owner, "UpManMembers.confirmRole"));
		waitForDialogClose(owner);
	}

	void groupAction(String path, String action)
	{
		page.click(By.cssSelector("[data-testid='UpManGroups.actions'][data-group-path='" + path + "']"));
		page.click(testId("UpManGroups." + action));
	}

	void cancel(String owner)
	{
		page.click(testId(owner, "BaseDialog.cancel"));
		waitForDialogClose(owner);
	}

	private void waitForDialogClose(String owner)
	{
		wait.ignoring(StaleElementReferenceException.class).until(browser -> browser.findElements(testId(owner))
				.stream().noneMatch(WebElement::isDisplayed));
	}

	static By memberActions(long entityId)
	{
		return By.cssSelector("[data-testid='UpManMembers.grid'] "
				+ "[data-testid='UpManMembers.memberActions'][data-member-id='" + entityId + "']");
	}

	static By groupRow(String path)
	{
		return By.cssSelector("[data-testid='UpManGroups.group'][data-group-path='" + path + "']");
	}
}
