package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.EndUserTestBase;
import io.imunity.upman.rest.RESTUpmanEndpoint;
import pl.edu.icm.unity.rest.RESTEndpointProperties;
import pl.edu.icm.unity.rest.RESTEndpointProperties.OpenApiPublication;
import pl.edu.icm.unity.restadm.RESTAdminEndpoint;

@Tag("admin-services")
class RestDocumentationServiceEditorTest extends EndUserTestBase
{
	private static final By PUBLICATION = testId("OpenApiPublicationField.publication");

	@Test
	void shouldSaveAndReloadPublicationModesForAdminAndUpman() throws Exception
	{
		String admin = deployAt(RESTAdminEndpoint.NAME, "/rest-admin-docs-test", "");
		String upman = deployAt(RESTUpmanEndpoint.NAME, "/rest-upman-docs-test", """
				unity.upman.rest.rootGroup=/
				unity.upman.rest.authorizationGroup=/
				unity.upman.rest.rootGroupAttributes.1=name
				""");
		var page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");

		changeAndReload(page, admin, "Disabled", "OpenAPI specification", OpenApiPublication.specification);
		changeAndReload(page, upman, "Disabled", "OpenAPI specification and Scalar UI",
				OpenApiPublication.specificationAndUI);
		screenshot("UpMan REST documentation publication");
		changeAndReload(page, admin, "OpenAPI specification", "Disabled", OpenApiPublication.disabled);
	}

	private void changeAndReload(VaadinPage page, String service, String before, String after,
			OpenApiPublication expected) throws Exception
	{
		String url = baseUrl + "/admin/services/edit/" + URLEncoder.encode(service, StandardCharsets.UTF_8)
				.replace("+", "%20");
		driver.get(url);
		assertThat(page.value(PUBLICATION)).isEqualTo(before);

		page.select(PUBLICATION, after);
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/services");

		var saved = new Properties();
		saved.load(new StringReader(endpoints.getEndpoint(service).getConfiguration().getConfiguration()));
		assertThat(new RESTEndpointProperties(saved).getOpenApiPublication()).isEqualTo(expected);
		driver.get(url);
		assertThat(page.value(PUBLICATION)).isEqualTo(after);
	}
}
