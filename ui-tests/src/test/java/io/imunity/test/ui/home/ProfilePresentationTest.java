package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static io.imunity.vaadin.endpoint.common.LanguageCookie.LANGUAGE_COOKIE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.EndUserTestBase;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

@Tag("home")
class ProfilePresentationTest extends EndUserTestBase
{
	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;

	@Test
	void shouldSavePolishProfileWithKeyboardInNarrowViewport() throws Exception
	{
		AttributeType type = new AttributeType("ui-localized-profile", "string");
		type.setSelfModificable(true);
		type.setMaxElements(1);
		types.addAttributeType(type);
		TestUser user = createUser("Polish profile owner");
		attributes.setAttribute(user.entity(), StringAttribute.of(type.getName(), "/", "Original profile"));
		deployHomeAt("/localized-home", """
				unity.userhome.attributes.1.attribute=ui-localized-profile
				unity.userhome.attributes.1.group=/
				unity.userhome.attributes.1.editable=true
				""");
		driver.manage().window().setSize(new Dimension(420, 900));
		driver.get(baseUrl + "/localized-home");
		driver.manage().addCookie(new Cookie(LANGUAGE_COOKIE, "pl"));
		driver.navigate().refresh();
		VaadinPage page = new VaadinPage(driver);
		page.signIn(user.username(), user.password());
		page.visible(By.id("MainHeader.loggedAs"));
		driver.get(baseUrl + "/localized-home/profile");
		By field = By.cssSelector("[data-testid='ProfileView.attribute']"
				+ "[data-attribute-name='ui-localized-profile'] vaadin-text-field");
		page.fill(field, "Zażółć gęślą jaźń");
		page.visible(testId("ProfileView.save")).sendKeys(Keys.ENTER);
		waitForElementNotPresent(testId("ProfileView.save"));
		assertThat(attributes.getAttributes(user.entity(), "/", type.getName())).singleElement()
				.satisfies(attribute -> assertThat(attribute.getValues()).containsExactly("Zażółć gęślą jaźń"));
		driver.navigate().refresh();
		assertThat(page.value(field)).isEqualTo("Zażółć gęślą jaźń");
		assertThat(driver.manage().getCookieNamed(LANGUAGE_COOKIE).getValue()).isEqualTo("pl");
	}
}
