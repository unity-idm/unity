package io.imunity.test.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

class VaadinPageTest
{
	@Test
	void shouldFindAndClickVisibleControlWhenAnotherMenuIsHidden()
	{
		WebDriver browser = mock(WebDriver.class);
		WebElement hiddenMenuItem = mock(WebElement.class);
		WebElement visibleMenuItem = mock(WebElement.class);
		By locator = VaadinPage.testId("Members.addToGroup");
		when(browser.findElements(locator)).thenReturn(List.of(hiddenMenuItem, visibleMenuItem));
		when(visibleMenuItem.isDisplayed()).thenReturn(true);
		when(visibleMenuItem.isEnabled()).thenReturn(true);
		VaadinPage page = new VaadinPage(browser);

		assertThat(page.visible(locator)).isSameAs(visibleMenuItem);
		page.click(locator);

		verify(visibleMenuItem).click();
		verify(hiddenMenuItem, never()).click();
	}

	@Test
	void shouldNotUseOriginalInputAfterSelectionReplacesView()
	{
		WebDriver browser = mock(WebDriver.class);
		WebElement originalField = mock(WebElement.class);
		WebElement replacementField = mock(WebElement.class);
		WebElement originalInput = mock(WebElement.class);
		WebElement replacementInput = mock(WebElement.class);
		WebElement option = mock(WebElement.class);
		By locator = By.className("u-authn-languageSelector");
		when(browser.findElements(locator)).thenReturn(List.of(originalField));
		when(originalField.isDisplayed()).thenReturn(true);
		when(originalField.getTagName()).thenReturn("vaadin-combo-box");
		when(originalField.findElement(By.cssSelector("input, textarea"))).thenReturn(originalInput);
		when(originalInput.isDisplayed()).thenReturn(true);
		when(originalInput.isEnabled()).thenReturn(true);
		when(originalField.findElements(By.cssSelector("vaadin-combo-box-item"))).thenReturn(List.of(option));
		when(option.isDisplayed()).thenReturn(true);
		when(option.getText()).thenReturn("Polski");
		doAnswer(ignored ->
		{
			when(browser.findElements(locator)).thenReturn(List.of(replacementField));
			return null;
		}).when(option).click();
		when(replacementField.isDisplayed()).thenReturn(true);
		when(replacementField.getTagName()).thenReturn("vaadin-combo-box");
		when(replacementField.findElement(By.cssSelector("input, textarea"))).thenReturn(replacementInput);
		when(replacementInput.isDisplayed()).thenReturn(true);
		when(replacementInput.isEnabled()).thenReturn(true);
		when(replacementInput.getDomProperty("value")).thenReturn("Polski");
		when(replacementField.getDomProperty("opened")).thenReturn("false");
		VaadinPage page = new VaadinPage(browser);

		page.select(locator, "Polski");

		verify(option).click();
		verify(originalInput, never()).sendKeys(Keys.TAB);
		verify(replacementInput).getDomProperty("value");
	}

	@Test
	void shouldFillReplacementInputWhenViewRendersAgain()
	{
		WebDriver browser = mock(WebDriver.class);
		WebElement field = mock(WebElement.class);
		WebElement original = mock(WebElement.class);
		WebElement replacement = mock(WebElement.class);
		By locator = VaadinPage.testId("Login.username");
		when(browser.findElements(locator)).thenReturn(List.of(field));
		when(field.isDisplayed()).thenReturn(true);
		when(field.getTagName()).thenReturn("vaadin-text-field");
		when(field.findElement(By.cssSelector("input, textarea"))).thenReturn(original, replacement);
		when(original.isDisplayed()).thenReturn(true);
		when(original.isEnabled()).thenReturn(true);
		when(replacement.isDisplayed()).thenReturn(true);
		when(replacement.isEnabled()).thenReturn(true);
		doThrow(new StaleElementReferenceException("The view replaced its input"))
				.when(original).sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE, "username", Keys.TAB);
		VaadinPage page = new VaadinPage(browser);

		page.fill(locator, "username");

		verify(original).sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE, "username", Keys.TAB);
		verify(replacement).sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE, "username", Keys.TAB);
	}
}
