package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;

final class AttributeWidgetPage
{
	private static final String DIALOG = "[data-testid='AttributeEditDialog'] ";
	private static final String VALUE = DIALOG + "[data-testid='InternalAttributeValueEditor.value']";
	private static final By ZONE = By.cssSelector(DIALOG
			+ "vaadin-combo-box[data-testid='InternalAttributeValueEditor.value']");
	private final VaadinPage page;
	private final WebDriverWait wait;

	AttributeWidgetPage(WebDriver driver)
	{
		page = new VaadinPage(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(SeleniumTestBase.WAIT_TIME_S));
	}

	void fill(AttributeWidgetCase scenario, String value)
	{
		switch (scenario.widget())
		{
			case BOOLEAN -> page.check(field(scenario), Boolean.parseBoolean(value));
			case ENUM ->
			{
				if (value.equals("blue") || value.equals("green"))
					page.select(field(scenario), value);
				else
				{
					page.visible(field(scenario)).findElement(By.tagName("input"))
							.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE, value, Keys.ENTER, Keys.TAB);
				}
			}
			case DATETIME, ZONED_DATETIME -> fillDateTime(scenario, value);
			case DATE -> fillPicker(field(scenario), value);
			case IMAGE, LINKED_IMAGE -> throw new IllegalArgumentException("Use upload for image widgets");
			case IMAGE_URL ->
			{
				chooseExternalImage();
				page.fill(field(scenario), value);
			}
			default -> page.fill(field(scenario), value);
		}
	}

	void assertValue(AttributeWidgetCase scenario, String expected)
	{
		switch (scenario.widget())
		{
			case BOOLEAN -> assertThat(page.visible(field(scenario)).getDomProperty("checked"))
					.isEqualTo(expected);
			case DATETIME -> assertThat(LocalDateTime.parse(page.visible(field(scenario)).getDomProperty("value")))
					.isEqualTo(LocalDateTime.parse(expected));
			case ZONED_DATETIME ->
			{
				ZonedDateTime dateTime = ZonedDateTime.parse(expected);
				assertThat(LocalDateTime.parse(page.visible(field(scenario)).getDomProperty("value")))
						.isEqualTo(dateTime.toLocalDateTime());
				assertThat(page.value(ZONE))
						.isEqualTo(dateTime.getZone().toString());
			}
			case IMAGE, LINKED_IMAGE -> wait.until(ignored ->
					Boolean.parseBoolean(image().getDomProperty("complete"))
							&& Integer.parseInt(image().getDomProperty("naturalWidth"))
							== (expected.equals("initial") ? 32 : 24));
			default -> assertThat(page.value(field(scenario))).isEqualTo(expected);
		}
	}

	void upload(Path file, boolean replace)
	{
		if (replace)
			clearImage();
		page.visible(testId("AttributeEditDialog", "UnityImageValueComponent.upload")).getShadowRoot()
				.findElement(By.cssSelector("input[type='file']")).sendKeys(file.toAbsolutePath().toString());
	}

	void clearImage()
	{
		page.click(testId("AttributeEditDialog", "UnityImageValueComponent.clear"));
	}

	WebElement image()
	{
		return page.visible(testId("AttributeEditDialog", "UnityImageValueComponent.preview"));
	}

	By field(AttributeWidgetCase scenario)
	{
		String tag = switch (scenario.widget())
		{
			case BOOLEAN -> "vaadin-checkbox";
			case ENUM -> "vaadin-combo-box";
			case DATE -> "vaadin-date-picker";
			case DATETIME, ZONED_DATETIME -> "vaadin-date-time-picker";
			case MULTILINE -> "vaadin-text-area";
			case EMAIL, MOBILE -> "vaadin-custom-field";
			case IMAGE_URL -> "";
			default -> "vaadin-text-field";
		};
		return scenario.widget() == AttributeWidgetCase.Widget.IMAGE_URL
				? testId("AttributeEditDialog", "PublicLinkableImageValueComponent.url")
				: By.cssSelector(DIALOG + tag + "[data-testid='InternalAttributeValueEditor.value']");
	}

	private void chooseExternalImage()
	{
		page.visible(testId("AttributeEditDialog", "PublicLinkableImageValueComponent.source"))
				.findElements(By.tagName("vaadin-radio-button")).stream()
				.filter(option -> option.getText().equals("External image URL")).findFirst().orElseThrow().click();
	}

	private void fillDateTime(AttributeWidgetCase scenario, String value)
	{
		String date = "";
		String time = "";
		if (!value.isEmpty())
		{
			LocalDateTime dateTime = scenario.widget() == AttributeWidgetCase.Widget.ZONED_DATETIME
					? ZonedDateTime.parse(value).toLocalDateTime() : LocalDateTime.parse(value);
			date = dateTime.toLocalDate().toString();
			time = dateTime.toLocalTime().toString();
		}
		if (scenario.widget() == AttributeWidgetCase.Widget.ZONED_DATETIME && !value.isEmpty())
		{
			date += " 00:00:00";
			page.select(ZONE, ZonedDateTime.parse(value).getZone().toString());
		}
		fillPicker(By.cssSelector(VALUE + " vaadin-date-picker"), date);
		fillPicker(By.cssSelector(VALUE + " vaadin-time-picker"), time);
	}

	private void fillPicker(By locator, String value)
	{
		page.visible(locator).findElement(By.tagName("input"))
				.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE, value, Keys.ENTER, Keys.TAB);
		wait.until(ignored -> !Boolean.parseBoolean(page.visible(locator).getDomProperty("opened")));
	}
}
