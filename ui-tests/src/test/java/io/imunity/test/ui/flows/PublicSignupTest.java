package io.imunity.test.ui.flows;

import static io.imunity.test.ui.VaadinPage.testId;
import static io.imunity.vaadin.endpoint.common.LanguageCookie.LANGUAGE_COOKIE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;

@Tag("registration")
class PublicSignupTest extends EndUserTestBase
{
	private static final String FORM = "ui-public-signup";
	private static final By USERNAME = By.id("UsernameIdentityEditor.username");
	private static final By SUBMIT = testId("StandaloneRegistration.submit");

	@Autowired
	@Qualifier("insecure")
	private RegistrationsManagement registrations;

	@ParameterizedTest(name = "{0}")
	@MethodSource("presentations")
	void shouldValidateRequiredDataAgreementAndDuplicateBeforeAcceptingSignup(Presentation scenario) throws Exception
	{
		TestUser existing = createUser("Existing signup identity");
		I18nString title = new I18nString("Selenium public signup");
		title.addValue("pl", "Rejestracja Selenium");
		I18nString terms = new I18nString("I accept the signup terms");
		terms.addValue("pl", "Akceptuję warunki rejestracji");
		registrations.addForm(new RegistrationFormBuilder().withName(FORM).withPageTitle(title)
				.withDefaultCredentialRequirement("ui-password").withPubliclyAvailable(true).withCaptchaLength(0)
				.withCheckIdentityOnSubmit(true).withAddedAgreement(new AgreementRegistrationParam(terms, true))
				.withAddedCredentialParam(new CredentialRegistrationParam("sys:password", null, null))
				.withAddedIdentityParam().withIdentityType("userName")
						.withRetrievalSettings(ParameterRetrievalSettings.interactive).endIdentityParam()
				.withTranslationProfile(new TranslationProfile(FORM, "", ProfileType.REGISTRATION, List.of())).build());
		driver.manage().window().setSize(new Dimension(scenario.width(), 1000));
		driver.get(baseUrl + "/unitygw/pub/registration/" + FORM);
		driver.manage().addCookie(new Cookie(LANGUAGE_COOKIE, scenario.language()));
		driver.navigate().refresh();
		VaadinPage page = new VaadinPage(driver);
		page.visible(USERNAME);
		assertThat(driver.getTitle()).isEqualTo(title.getValue(scenario.language()));
		assertThat(driver.findElement(By.tagName("body")).getText()).contains(terms.getValue(scenario.language()));
		page.click(SUBMIT);
		waitForUi();
		assertThat(requests()).isEmpty();
		page.fill(USERNAME, existing.username());
		page.fill(By.className("u-password-setup"), "Signup!75-Harbor");
		page.fill(By.className("u-password-repeat"), "Signup!75-Harbor");
		page.click(SUBMIT);
		waitForUi();
		assertThat(requests()).isEmpty();
		page.visible(By.cssSelector("vaadin-checkbox")).sendKeys(Keys.SPACE);
		page.click(SUBMIT);
		page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(requests()).isEmpty();
		assertThat(entities.getEntity(existing.entity()).getIdentities()).anySatisfy(identity ->
				assertThat(identity.getValue()).isEqualTo(existing.username()));

		String username = "ui-signup-" + UUID.randomUUID();
		page.fill(USERNAME, username);
		page.click(SUBMIT);
		waitFor(() -> requests().size() == 1);
		RegistrationRequestState submitted = requests().getFirst();
		assertThat(submitted.getStatus()).isEqualTo(RegistrationRequestStatus.pending);
		assertThat(submitted.getCreatedEntityId()).isNull();
		assertThat(submitted.getRequest().getIdentities()).singleElement()
				.satisfies(identity -> assertThat(identity.getValue()).isEqualTo(username));
		waitForElementNotPresent(SUBMIT);
		driver.navigate().refresh();
		assertThat(requests()).hasSize(1);
	}

	private List<RegistrationRequestState> requests()
	{
		try
		{
			return registrations.getRegistrationRequests().stream()
					.filter(request -> FORM.equals(request.getRequest().getFormId())).toList();
		} catch (Exception e)
		{
			throw new IllegalStateException("Cannot read signup requests", e);
		}
	}

	private static Stream<Presentation> presentations()
	{
		return Stream.of(Presentation.builder().language("en").width(1280).build(),
				Presentation.builder().language("pl").width(420).build());
	}

	private record Presentation(String language, int width)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private String language;
			private int width;

			Builder language(String value)
			{
				language = value;
				return this;
			}

			Builder width(int value)
			{
				width = value;
				return this;
			}

			Presentation build()
			{
				return new Presentation(language, width);
			}
		}
	}
}
