package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.entity.EntityParam;

final class DirectoryWidgetPage
{
	private final WebDriver driver;
	private final VaadinPage page;
	private final WebDriverWait wait;

	DirectoryWidgetPage(WebDriver driver)
	{
		this.driver = driver;
		page = new VaadinPage(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(SeleniumTestBase.WAIT_TIME_S));
	}

	void selectEntity(EntityParam entity)
	{
		clickEntityCell(entity, "vaadin-grid-tree-toggle + span");
		page.visible(By.cssSelector("[data-testid='AttributesGrid'][data-entity-id='" + entity.getEntityId() + "']"));
	}

	void expandEntity(EntityParam entity)
	{
		clickEntityCell(entity, "vaadin-grid-tree-toggle");
	}

	void addAttribute(String name)
	{
		page.click(testId("AttributesGrid.add"));
		By selector = testId("AttributeEditDialog", "AttributeTypeSelection.type");
		page.visible(selector).findElement(By.tagName("input")).sendKeys(Keys.chord(Keys.CONTROL, "a"), name);
		wait.ignoring(StaleElementReferenceException.class).until(ignored -> page.visible(selector)
				.findElements(By.tagName("vaadin-combo-box-item")).stream()
				.filter(option -> option.isDisplayed() && option.getDomProperty("textContent").contains(name))
				.findFirst().orElse(null)).click();
		page.visible(By.cssSelector("[data-testid='AttributeEditDialog'] [data-attribute-name='" + name + "']"));
	}

	void editAttribute(String name)
	{
		new GridPage(driver).action("AttributesGrid.attributes", "attribute-" + name, "vaadin:menu");
		new AdminPage(driver).chooseMenuItem("Edit");
		page.visible(testId("AttributeEditDialog"));
	}

	void saveAttribute()
	{
		page.click(testId("AttributeEditDialog", "DialogWithActionFooter.confirm"));
		wait.until(ignored -> driver.findElements(testId("AttributeEditDialog")).stream()
				.noneMatch(WebElement::isDisplayed));
	}

	void removeIdentity(EntityParam entity, String value)
	{
		By menu = By.cssSelector("[data-testid='IdentitiesTreeGrid.entityActions'][data-entity-id='"
				+ entity.getEntityId() + "'][data-identity-value='" + value + "']");
		wait.ignoring(StaleElementReferenceException.class).ignoring(ElementClickInterceptedException.class)
				.until(ignored ->
		{
			WebElement target = page.visible(menu);
			scroll(target, true);
			target.click();
			return true;
		});
		new AdminPage(driver).chooseMenuItem("Remove identity");
		page.click(By.cssSelector("vaadin-confirm-dialog [slot='confirm-button']"));
		wait.until(ignored -> driver.findElements(menu).isEmpty());
	}

	private void clickEntityCell(EntityParam entity, String selector)
	{
		wait.ignoring(StaleElementReferenceException.class).ignoring(ElementClickInterceptedException.class)
				.until(ignored ->
		{
			WebElement grid = page.visible(testId("IdentitiesTreeGrid"));
			List<WebElement> cells = entityCells(grid, entity);
			WebElement element = cells.stream().flatMap(cell -> cell.findElements(By.cssSelector(selector)).stream())
					.findFirst().orElse(null);
			if (element == null)
				return false;
			scroll(element, false);
			element.click();
			return true;
		});
	}

	@SuppressWarnings("unchecked")
	private List<WebElement> entityCells(WebElement grid, EntityParam entity)
	{
		return (List<WebElement>) ((JavascriptExecutor) driver).executeScript("""
				const target = arguments[0].shadowRoot.querySelector('[part~="entity-' + arguments[1] + '"]');
				const row = target && target.closest('tr, [role="row"]');
				return row ? [...row.querySelectorAll('slot')].flatMap(slot => slot.assignedElements()) : [];
				""", grid, entity.getEntityId());
	}

	private void scroll(WebElement element, boolean right)
	{
		((JavascriptExecutor) driver).executeScript("""
				const scroller = arguments[0].closest('vaadin-grid').shadowRoot.querySelector('#table');
				scroller.scrollLeft = arguments[1] ? scroller.scrollWidth : 0;
				const target = arguments[0].getBoundingClientRect();
				const viewport = scroller.getBoundingClientRect();
				scroller.scrollTop += target.top + target.height / 2 - viewport.top - viewport.height / 2;
				""", element, right);
	}
}
