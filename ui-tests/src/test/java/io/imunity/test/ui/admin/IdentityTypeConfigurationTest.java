package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.engine.api.IdentityTypesManagement;

@Tag("admin-settings")
class IdentityTypeConfigurationTest extends SeleniumTestBase
{
	@Autowired
	@Qualifier("insecure")
	private IdentityTypesManagement identityTypes;
	private IdentityType original;

	@AfterEach
	void restoreType() throws Exception
	{
		if (original != null)
			identityTypes.updateIdentityType(original);
	}

	@Test
	void shouldPersistSelfServiceIdentityLimits() throws Exception
	{
		original = identityTypes.getIdentityType("userName");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/identity-type/edit/userName");
		page.fill(testId("IdentityTypeEditor.description"), "Selenium username policy");
		page.check(testId("IdentityTypeEditor.selfModifiable"), false);
		waitFor(() -> Boolean.parseBoolean(page.visible(testId("IdentityTypeEditor.minimum")).getDomProperty("disabled")));
		page.check(testId("IdentityTypeEditor.selfModifiable"), true);
		page.fill(testId("IdentityTypeEditor.minimum"), "1");
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/identity-types");

		driver.get(baseUrl + "/admin/identity-type/edit/userName");
		assertThat(page.value(testId("IdentityTypeEditor.description"))).isEqualTo("Selenium username policy");
		assertThat(page.visible(testId("IdentityTypeEditor.selfModifiable")).getDomProperty("checked"))
				.isEqualTo("true");
		assertThat(page.visible(testId("IdentityTypeEditor.minimum")).getDomProperty("disabled")).isEqualTo("false");
		assertThat(page.value(testId("IdentityTypeEditor.minimum"))).isEqualTo("1");
		IdentityType stored = identityTypes.getIdentityType("userName");
		assertThat(stored.getDescription()).isEqualTo("Selenium username policy");
		assertThat(stored.isSelfModificable()).isTrue();
		assertThat(stored.getMinInstances()).isEqualTo(1);
	}
}
