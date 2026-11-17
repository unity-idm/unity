package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.entity.EntityParam;

final class AccountAdministrationPage
{
	private final WebDriver driver;
	private final VaadinPage page;

	AccountAdministrationPage(WebDriver driver)
	{
		this.driver = driver;
		page = new VaadinPage(driver);
	}

	void open(String baseUrl)
	{
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/directory-browser");
		new DirectoryPage(driver).selectGroup("/");
	}

	void action(EntityParam entity, String label)
	{
		By menu = By.cssSelector("[data-testid='IdentitiesTreeGrid.entityActions'][data-entity-id='"
				+ entity.getEntityId() + "']");
		new WebDriverWait(driver, Duration.ofSeconds(SeleniumTestBase.WAIT_TIME_S))
				.ignoring(StaleElementReferenceException.class).until(ignored ->
				{
					WebElement target = driver.findElements(menu).stream().findFirst().orElse(null);
					if (target == null)
						return false;
					((JavascriptExecutor) driver).executeScript(
							"""
							const grid = arguments[0].closest('vaadin-grid');
							const scroller = grid.shadowRoot.querySelector('#table');
							scroller.scrollLeft = scroller.scrollWidth;
							""", target);
					return target.isDisplayed();
				});
		page.click(menu);
		new AdminPage(driver).chooseMenuItem(label);
	}

	void changeState(EntityParam entity, String state)
	{
		action(entity, "Change status");
		new AdminPage(driver).selectOption(testId("ChangeEntityStateDialog.state"), state);
		page.click(testId("ChangeEntityStateDialog", "DialogWithActionFooter.confirm"));
	}

	void changeRequirement(EntityParam entity, String requirement)
	{
		action(entity, "Change credential requirements");
		new AdminPage(driver).selectOption(testId("ChangeCredentialRequirementDialog.requirement"),
				requirement);
		page.click(testId("ChangeCredentialRequirementDialog", "DialogWithActionFooter.confirm"));
	}

	void confirmDeletion(boolean confirm)
	{
		page.click(By.cssSelector("vaadin-confirm-dialog [slot='" + (confirm ? "confirm" : "cancel") + "-button']"));
	}
}
