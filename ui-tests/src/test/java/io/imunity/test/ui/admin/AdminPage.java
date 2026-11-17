package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;

final class AdminPage
{
	private final WebDriver driver;
	private final VaadinPage page;
	private final WebDriverWait wait;

	AdminPage(WebDriver driver)
	{
		this.driver = driver;
		page = new VaadinPage(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(SeleniumTestBase.WAIT_TIME_S));
	}

	void selectOption(By locator, String label)
	{
		page.click(locator);
		wait.ignoring(StaleElementReferenceException.class).until(ignored -> driver
				.findElements(By.cssSelector("vaadin-select-item")).stream()
				.filter(item -> item.isDisplayed() && label.equals(item.getText())).findFirst().orElse(null)).click();
		wait.until(ignored -> !Boolean.parseBoolean(page.visible(locator).getDomProperty("opened")));
	}

	void openInteractiveSettings()
	{
		page.click(By.cssSelector("vaadin-accordion-panel vaadin-accordion-heading"));
	}

	void deleteRequirement(String name)
	{
		new GridPage(driver).action("CredentialRequirementsView.requirements", "requirement-" + name, "vaadin:trash");
		page.select(testId("CredentialRequirementsView.replacement"), "ui-password");
		page.click(By.cssSelector("[data-testid='CredentialRequirementsView.remove'] [slot='confirm-button']"));
	}

	void chooseMenuItem(String label)
	{
		wait.ignoring(StaleElementReferenceException.class).until(ignored -> driver
				.findElements(By.cssSelector("vaadin-context-menu-item")).stream()
				.filter(item -> item.isDisplayed() && label.equals(item.getText())).findFirst().orElse(null)).click();
	}
}
