package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import io.imunity.test.ui.VaadinPage;

@Tag("admin-directory")
class AttributeWidgetValidationTest extends AttributeWidgetTestBase
{
	@ParameterizedTest(name = "invalid {0}")
	@MethodSource("invalidCases")
	void shouldPreserveStoredValueOnInvalidEditAndAllowCorrection(AttributeWidgetCase scenario) throws Exception
	{
		prepare(scenario);
		DirectoryWidgetPage directory = new DirectoryWidgetPage(driver);
		AttributeWidgetPage widget = new AttributeWidgetPage(driver);
		directory.addAttribute(name);
		fill(widget, scenario, scenario.initial(), false);
		directory.saveAttribute();
		String before = storedValue();

		directory.editAttribute(name);
		if (scenario.uploadsImage())
			widget.clearImage();
		else
			widget.fill(scenario, scenario.invalid());
		new VaadinPage(driver).click(testId("AttributeEditDialog", "DialogWithActionFooter.confirm"));
		if (scenario.uploadsImage())
			assertThat(new VaadinPage(driver).visible(
					testId("AttributeEditDialog", "UnityImageValueComponent.error")).getText()).isNotBlank();
		else
			waitFor(() -> driver.findElements(By.cssSelector("[data-testid='AttributeEditDialog'] [invalid]"))
					.stream().anyMatch(WebElement::isDisplayed));
		assertThat(new VaadinPage(driver).visible(testId("AttributeEditDialog")).isDisplayed()).isTrue();
		assertThat(storedValue()).isEqualTo(before);
		screenshot("Invalid attribute value");
		fill(widget, scenario, scenario.updated(), false);
		directory.saveAttribute();
		assertStoredAndRendered(scenario, scenario.updated());
		reload(directory);
		directory.editAttribute(name);
		widget.assertValue(scenario, input(scenario, scenario.updated()));
	}

	private static Stream<AttributeWidgetCase> invalidCases()
	{
		return AttributeWidgetCase.cases().filter(scenario -> scenario.invalid() != null);
	}
}
