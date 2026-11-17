package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;

final class GridPage
{
	private final WebDriver driver;
	private final VaadinPage page;
	private final WebDriverWait wait;

	GridPage(WebDriver driver)
	{
		this.driver = driver;
		page = new VaadinPage(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(SeleniumTestBase.WAIT_TIME_S));
	}

	void action(String grid, String part, String icon)
	{
		wait.ignoring(StaleElementReferenceException.class).ignoring(ElementClickInterceptedException.class)
				.until(ignored ->
		{
			WebElement action = cells(grid, part).stream()
					.flatMap(cell -> cell.findElements(By.cssSelector(
							"vaadin-icon[icon='" + icon + "']")).stream())
					.findFirst().orElse(null);
			if (action == null)
				return false;
			((JavascriptExecutor) driver).executeScript(
					"""
					const grid = arguments[0].closest('vaadin-grid');
					const scroller = grid.shadowRoot.querySelector('#table');
					scroller.scrollLeft = scroller.scrollWidth;
					""", action);
			action.click();
			return true;
		});
	}

	String text(String grid, String part)
	{
		return wait.ignoring(StaleElementReferenceException.class).until(ignored ->
		{
			List<WebElement> cells = cells(grid, part);
			String text = String.join(" ", cells.stream().map(WebElement::getText).toList());
			return text.isBlank() ? null : text;
		});
	}

	@SuppressWarnings("unchecked")
	private List<WebElement> cells(String grid, String part)
	{
		return (List<WebElement>) ((JavascriptExecutor) driver).executeScript("""
				const target = arguments[0].shadowRoot.querySelector('[part~="' + arguments[1] + '"]');
				const row = target && (target.closest('tr, [role="row"]') || target);
				return row ? [...row.querySelectorAll('slot')].flatMap(slot => slot.assignedElements()) : [];
				""", page.visible(testId(grid)), part);
	}
}
