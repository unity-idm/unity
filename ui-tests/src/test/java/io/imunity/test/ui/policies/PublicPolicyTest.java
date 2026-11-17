package io.imunity.test.ui.policies;

import static io.imunity.test.ui.VaadinPage.testId;
import static io.imunity.vaadin.endpoint.common.LanguageCookie.LANGUAGE_COOKIE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;

@Tag("public-policies")
class PublicPolicyTest extends SeleniumTestBase
{
	@Autowired
	@Qualifier("insecure")
	private PolicyDocumentManagement policies;
	private Long documentId;

	@AfterEach
	void removeDocument() throws Exception
	{
		if (documentId != null)
			policies.removePolicyDocument(documentId);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("policyCases")
	void shouldShowPublicPolicyOrEmptyStateWithoutAuthentication(PolicyCase policyCase) throws Exception
	{
		I18nString title = new I18nString("Selenium privacy policy");
		title.addValue("pl", "Polityka prywatności Selenium");
		I18nString content = new I18nString("<p>Only the documented profile values are collected.</p>");
		content.addValue("pl", "<p>Zbieramy wyłącznie opisane dane profilu.</p>");
		documentId = policies.addPolicyDocument(new PolicyDocumentCreateRequest("ui-policy-" + UUID.randomUUID(),
				title, false, PolicyDocumentContentType.EMBEDDED, content));
		long requested = policyCase.exists() ? documentId : Long.MAX_VALUE;
		VaadinPage page = new VaadinPage(driver);
		driver.get(baseUrl + "/unitygw/pub/policyDocuments/" + requested);
		driver.manage().addCookie(new Cookie(LANGUAGE_COOKIE, policyCase.language()));
		driver.navigate().refresh();
		assertPolicy(page, policyCase);
		driver.navigate().refresh();
		assertPolicy(page, policyCase);
		assertThat(driver.findElements(By.className("u-passwordUsernameField"))).isEmpty();
		screenshot("Public policy document");
	}

	private void assertPolicy(VaadinPage page, PolicyCase policyCase)
	{
		if (policyCase.exists())
		{
			assertThat(page.visible(testId("PublicPolicyDocumentView.title")).getText())
					.isEqualTo(policyCase.language().equals("pl")
							? "Polityka prywatności Selenium" : "Selenium privacy policy");
			assertThat(page.visible(testId("PublicPolicyDocumentView.content")).getText())
					.isEqualTo(policyCase.language().equals("pl") ? "Zbieramy wyłącznie opisane dane profilu."
							: "Only the documented profile values are collected.");
			assertThat(driver.findElements(testId("PublicPolicyDocumentView.empty"))).isEmpty();
		} else
		{
			assertThat(page.visible(testId("PublicPolicyDocumentView.empty")).getText()).isNotBlank();
			assertThat(driver.findElements(testId("PublicPolicyDocumentView.title"))).isEmpty();
			assertThat(driver.findElements(testId("PublicPolicyDocumentView.content"))).isEmpty();
		}
	}

	private static Stream<PolicyCase> policyCases()
	{
		return Stream.of(PolicyCase.builder().name("English public document").exists(true).language("en").build(),
				PolicyCase.builder().name("Polish public document").exists(true).language("pl").build(),
				PolicyCase.builder().name("unknown public document").exists(false).language("en").build());
	}

	record PolicyCase(String name, boolean exists, String language)
	{
		static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return name;
		}

		static final class Builder
		{
			private String name;
			private boolean exists;
			private String language;

			Builder name(String name)
			{
				this.name = name;
				return this;
			}

			Builder exists(boolean exists)
			{
				this.exists = exists;
				return this;
			}

			Builder language(String language)
			{
				this.language = language;
				return this;
			}

			PolicyCase build()
			{
				return new PolicyCase(name, exists, language);
			}
		}
	}
}
