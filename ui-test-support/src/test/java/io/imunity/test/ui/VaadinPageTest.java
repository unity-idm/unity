package io.imunity.test.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
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
}
