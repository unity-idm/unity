package io.imunity.test.ui.flows;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

@Tag("enquiry")
class EnquiryWorkflowTest extends EndUserTestBase
{
	private static final String ANSWER = "ui-enquiry-answer";
	private static final By DIALOG = testId("EnquiryFormFillDialog");
	private static final By CONFIRM = By.cssSelector("[data-testid='EnquiryFormFillDialog']"
			+ " vaadin-button[slot='confirm-button']");

	@Autowired
	@Qualifier("insecure")
	private EnquiryManagement enquiries;
	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groups;
	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;

	@Test
	void shouldGateLoginUntilMandatoryEnquiryIsSubmitted() throws Exception
	{
		TestUser user = createUser("Mandatory enquiry owner");
		String form = prepareEnquiry(user, EnquiryType.REQUESTED_MANDATORY);
		VaadinPage page = new VaadinPage(driver);
		driver.get(baseUrl + "/home");
		page.signIn(user.username(), user.password());
		page.visible(DIALOG);
		driver.get(baseUrl + "/home/profile");
		assertThat(page.visible(DIALOG).getDomProperty("opened")).isEqualTo("true");
		page.click(CONFIRM);
		page.visible(By.cssSelector("[data-testid='EnquiryFormFillDialog'] vaadin-text-field[invalid]"));
		assertThat(attributes.getAttributes(user.entity(), "/", ANSWER)).isEmpty();
		page.fill(By.cssSelector("[data-testid='EnquiryFormFillDialog'] vaadin-text-field"), "Accepted answer");
		page.click(CONFIRM);
		waitFor(() -> driver.findElements(DIALOG).stream().noneMatch(element -> element.isDisplayed()));
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertAnswer(user, "Accepted answer");
		assertThat(enquiries.getEnquiryResponses())
				.anyMatch(response -> form.equals(response.getRequest().getFormId()));
		assertFreshLoginWithoutEnquiry(user);
	}

	@Test
	void shouldPersistIgnoringOptionalEnquiryWithoutChangingAttributes() throws Exception
	{
		TestUser user = createUser("Optional enquiry owner");
		prepareEnquiry(user, EnquiryType.REQUESTED_OPTIONAL);
		VaadinPage page = new VaadinPage(driver);
		driver.get(baseUrl + "/home");
		page.signIn(user.username(), user.password());
		page.visible(DIALOG);
		page.click(By.cssSelector("[data-testid='EnquiryFormFillDialog'] vaadin-button[slot='reject-button']"));
		page.visible(By.id("MainHeader.loggedAs"));
		assertThat(enquiries.getEnquiryResponses()).isEmpty();
		assertThat(attributes.getAttributes(user.entity(), "/", ANSWER)).isEmpty();
		assertFreshLoginWithoutEnquiry(user);
	}

	@Test
	void shouldSaveStickyAccountUpdateAndShowSavedValueAfterReload() throws Exception
	{
		TestUser user = createUser("Account update owner");
		String form = prepareEnquiry(user, EnquiryType.STICKY);
		deployHomeAt("/enquiry-home", "unity.userhome.accountUpdateEnquiries.1=" + form + "\n");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/enquiry-home", user.username(), user.password());
		driver.get(baseUrl + "/enquiry-home/account-update");
		By field = By.cssSelector(".u-sticky-enquiry vaadin-text-field");
		page.fill(field, "Updated from Home");
		page.click(testId("StickyEnquiryUpdatableComponent.submit"));
		waitFor(() -> !page.value(field).isBlank());
		assertAnswer(user, "Updated from Home");
		driver.navigate().refresh();
		assertThat(page.value(field)).isEqualTo("Updated from Home");
		page.fill(field, "Second account update");
		page.click(testId("StickyEnquiryUpdatableComponent.submit"));
		page.visible(By.cssSelector("vaadin-notification-card"));
		assertAnswer(user, "Second account update");
	}

	@Test
	void shouldSubmitStandaloneStickyEnquiryThroughAuthenticatedAlias() throws Exception
	{
		TestUser user = createUser("Standalone enquiry owner");
		String form = prepareEnquiry(user, EnquiryType.STICKY);
		deployAt("WellKnownLinksHandler", "/well-known", "");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/home", user.username(), user.password());
		driver.get(baseUrl + "/well-known/sec/enquiry/" + form);
		page.fill(By.cssSelector(".u-standalone-public-form vaadin-text-field"), "Standalone answer");
		page.click(testId("StandaloneEnquiryView.submit"));
		page.visible(By.className("u-final-info"));
		assertAnswer(user, "Standalone answer");
	}

	private String prepareEnquiry(TestUser user, EnquiryType type) throws Exception
	{
		String suffix = UUID.randomUUID().toString();
		String target = "/ui-enquiry-" + suffix;
		groups.addGroup(new Group(target));
		groups.addMemberFromParent(target, user.entity());
		AttributeType attribute = new AttributeType(ANSWER, "string");
		StringAttributeSyntax syntax = new StringAttributeSyntax();
		syntax.setMinLength(1);
		attribute.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		attribute.setMinElements(1);
		attribute.setMaxElements(1);
		types.addAttributeType(attribute);
		EnquiryForm form = new EnquiryFormBuilder().withName("ui-enquiry-" + suffix)
				.withDisplayedName(new I18nString("Selenium account update"))
				.withTargetGroups(new String[] {target}).withType(type)
				.withAddedAttributeParam().withAttributeType(ANSWER).withGroup("/")
						.withOptional(false)
						.withRetrievalSettings(ParameterRetrievalSettings.interactive).endAttributeParam()
				.withTranslationProfile(new TranslationProfile("ui-autoaccept", "", ProfileType.REGISTRATION, List.of(
						new TranslationRule("true", new TranslationAction(AutoProcessActionFactory.NAME, "accept")))))
				.build();
		enquiries.addEnquiry(form);
		return form.getName();
	}

	private void assertAnswer(TestUser user, String expected) throws Exception
	{
		waitFor(() ->
		{
			try
			{
				return attributes.getAttributes(user.entity(), "/", ANSWER).stream()
						.anyMatch(attribute -> attribute.getValues().equals(List.of(expected)));
			} catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		});
		assertThat(attributes.getAttributes(user.entity(), "/", ANSWER)).singleElement()
				.satisfies(attribute -> assertThat(attribute.getValues()).containsExactly(expected));
	}

	private void assertFreshLoginWithoutEnquiry(TestUser user)
	{
		WebDriver fresh = newBrowser();
		VaadinPage page = new VaadinPage(fresh);
		page.login(baseUrl, "/home", user.username(), user.password());
		assertThat(fresh.findElements(DIALOG)).isEmpty();
	}
}
