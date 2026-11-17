package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementAcceptanceStatus;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementDecision;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;

@Tag("authentication")
class RenewedPolicyTest extends SamlWorkflowTestBase
{
	@Autowired
	@Qualifier("insecure")
	private PolicyDocumentManagement policies;
	@Autowired
	private PolicyAgreementManagement agreements;

	@Test
	void shouldRequireNewPolicyRevisionBeforeCompletingProtectedLogin() throws Exception
	{
		TestUser user = createUser("Renewed policy owner");
		I18nString title = new I18nString("Selenium terms");
		long id = policies.addPolicyDocument(new PolicyDocumentCreateRequest("ui-terms", title, true,
				PolicyDocumentContentType.EMBEDDED, new I18nString("<p>Original terms.</p>")));
		agreements.submitDecisions(user.entity(), List.of(new PolicyAgreementDecision(
				PolicyAgreementAcceptanceStatus.ACCEPTED, List.of(id))));
		int originalRevision = policies.getPolicyDocument(id).revision;
		policies.updatePolicyDocumentWithRevision(new PolicyDocumentUpdateRequest(id, "ui-terms", title, true,
				PolicyDocumentContentType.EMBEDDED, new I18nString("<p>Revised terms require renewed consent.</p>")));
		int revision = policies.getPolicyDocument(id).revision;
		assertThat(revision).isGreaterThan(originalRevision);
		deploySaml("""
				unity.saml.policyAgreementsTitle=Selenium revised terms
				unity.saml.policyAgreements.1.policyDocuments=%s
				unity.saml.policyAgreements.1.policyAgreementPresentationType=CHECKBOX_NOTSELECTED
				unity.saml.policyAgreements.1.text=I accept {%s:Selenium terms}
				""".formatted(id, id), List.of());
		driver.get(baseUrl + "/saml-home");
		VaadinPage page = new VaadinPage(driver);
		page.click(PROVIDER);
		page.signIn(user.username(), user.password());
		By submit = By.xpath("//vaadin-button[normalize-space(.)='Submit']");
		waitFor(() -> page.visible(submit).getDomAttribute("disabled") != null);
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
		screenshot("Renewed policy agreement");
		page.click(By.linkText("Selenium terms"));
		String source = driver.getWindowHandle();
		waitFor(() -> driver.getWindowHandles().size() == 2);
		driver.switchTo().window(driver.getWindowHandles().stream().filter(handle -> !handle.equals(source))
				.findFirst().orElseThrow());
		assertThat(page.visible(By.cssSelector("[data-testid='PublicPolicyDocumentView.content']")).getText())
				.isEqualTo("Revised terms require renewed consent.");
		driver.close();
		driver.switchTo().window(source);
		page.check(By.cssSelector("vaadin-checkbox"), true);
		page.click(submit);
		page.click(By.className("u-consentConfirmButton"));
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(agreements.getPolicyAgreementsStatus(user.entity())).anySatisfy(state ->
		{
			assertThat(state.policyDocumentId).isEqualTo(id);
			assertThat(state.policyDocumentRevision).isEqualTo(revision);
			assertThat(state.acceptanceStatus).isEqualTo(PolicyAgreementAcceptanceStatus.ACCEPTED);
		});
		var fresh = newBrowser();
		VaadinPage login = new VaadinPage(fresh);
		fresh.get(baseUrl + "/saml-home");
		login.click(PROVIDER);
		login.signIn(user.username(), user.password());
		login.click(By.className("u-consentConfirmButton"));
		assertThat(login.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}
}
