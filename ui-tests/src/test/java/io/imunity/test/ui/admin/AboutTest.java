package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.engine.api.version.VersionInformation;
import pl.edu.icm.unity.engine.api.version.VersionInformationProvider;

@Tag("admin-settings")
class AboutTest extends SeleniumTestBase
{
	@Autowired
	private VersionInformationProvider versions;

	@Test
	void shouldDisplayRunningBuildInformationAfterReload() throws Exception
	{
		VersionInformation running = versions.getVersionInformation();
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/about");
		assertBuild(page, running);
		driver.navigate().refresh();
		assertBuild(page, running);
		screenshot("Running build information");
	}

	private void assertBuild(VaadinPage page, VersionInformation running)
	{
		assertThat(page.visible(testId("AboutView.version")).getText()).isEqualTo(running.version);
		assertThat(page.visible(testId("AboutView.buildTime")).getText())
				.isEqualTo(TimeUtil.formatStandardInstant(running.buildTime));
	}
}
