package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.i18n.I18nMessage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.MessageType;
import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;

@Tag("admin-authentication")
class CredentialDefinitionTest extends SeleniumTestBase
{
	private final String name = "ui-credential-" + UUID.randomUUID();
	private final String templateName = "ui-sms-" + UUID.randomUUID();
	private final List<String> createdTemplates = new ArrayList<>();
	private boolean channelCreated;

	@Autowired
	@Qualifier("insecure")
	private CredentialManagement credentials;
	@Autowired
	@Qualifier("insecure")
	private MessageTemplateManagement templates;
	@Autowired
	@Qualifier("insecure")
	private NotificationsManagement notifications;

	@AfterEach
	void removeFixtures() throws Exception
	{
		try
		{
			if (credentials.getCredentialDefinitions().stream().anyMatch(definition -> definition.getName().equals(name)))
				credentials.removeCredentialDefinition(name);
		} finally
		{
			try
			{
				for (String template : createdTemplates.reversed())
					templates.removeTemplate(template);
			} finally
			{
				if (channelCreated)
					notifications.removeNotificationChannel(templateName);
			}
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("credentialCases")
	void shouldCreateEditReloadAndInspectCredentialDefinition(CredentialCase scenario) throws Exception
	{
		VaadinPage page = openEditor(scenario);
		page.fill(testId("CredentialsEditView", "CredentialsEditView.name"), name);
		page.fill(testId("CredentialsEditView", "CredentialsEditView.description"), "Initial credential description");
		if (scenario.field() != null)
			page.fill(editorField(scenario), scenario.initial());
		save();
		assertStored(scenario, scenario.initial(), "Initial credential description");

		driver.get(baseUrl + "/admin/credentials/edit/" + name);
		assertThat(page.value(testId("CredentialsEditView", "CredentialsEditView.name"))).isEqualTo(name);
		assertThat(page.value(testId("CredentialsEditView", "CredentialsEditView.type"))).isEqualTo(scenario.type());
		assertThat(page.visible(testId("CredentialsEditView", "CredentialsEditView.name"))
				.getDomProperty("readonly")).isEqualTo("true");
		if (scenario.field() != null)
		{
			assertThat(page.value(editorField(scenario))).isEqualTo(scenario.initial());
			page.fill(editorField(scenario), scenario.updated());
		}
		page.fill(testId("CredentialsEditView", "CredentialsEditView.description"), "Updated credential description");
		save();
		driver.get(baseUrl + "/admin/credentials/edit/" + name);
		assertThat(page.value(testId("CredentialsEditView", "CredentialsEditView.description")))
				.isEqualTo("Updated credential description");
		if (scenario.field() != null)
			assertThat(page.value(editorField(scenario))).isEqualTo(scenario.updated());
		assertStored(scenario, scenario.updated(), "Updated credential description");

		driver.get(baseUrl + "/admin/credentials/info/" + name);
		assertThat(page.visible(testId("CredentialsInfoView", "CredentialsInfoView.name")).getText()).isEqualTo(name);
		assertThat(page.visible(testId("CredentialsInfoView", "CredentialsInfoView.type")).getText())
				.isEqualTo(scenario.type());
		assertThat(page.visible(testId("CredentialsInfoView", "CredentialsInfoView.description")).getText())
				.contains("Updated credential description");
		if (scenario.type().equals("fido"))
		{
			By host = testId("CredentialsInfoView.configuration", scenario.field());
			assertThat(page.value(host)).isEqualTo(scenario.updated());
			page.fill(host, "unsaved.example.org");
			driver.navigate().refresh();
			assertThat(page.value(host)).isEqualTo(scenario.updated());
			assertStored(scenario, scenario.updated(), "Updated credential description");
		}
		else if (scenario.viewer() != null)
			assertThat(page.visible(testId("CredentialsInfoView", scenario.viewer())).getText()).contains(scenario.updated());
		else
			assertThat(page.visible(testId("CredentialsInfoView.configuration")).getText()).isNotBlank();
	}

	@ParameterizedTest(name = "required configuration: {0}")
	@MethodSource("requiredFieldCases")
	void shouldRejectMissingRequiredConfigurationWithoutCreatingDefinition(CredentialCase scenario) throws Exception
	{
		VaadinPage page = openEditor(scenario);
		page.fill(testId("CredentialsEditView", "CredentialsEditView.name"), name);
		page.fill(editorField(scenario), "");
		page.click(testId("EditViewActionLayoutFactory.save"));
		waitFor(() -> Boolean.parseBoolean(page.visible(editorField(scenario)).getDomProperty("invalid")));
		assertThat(driver.getCurrentUrl()).endsWith("/credentials/edit");
		assertThat(credentials.getCredentialDefinitions()).extracting(CredentialDefinition::getName).doesNotContain(name);
	}

	private VaadinPage openEditor(CredentialCase scenario) throws Exception
	{
		if (scenario.type().equals("sms"))
		{
			notifications.addNotificationChannel(new NotificationChannel(templateName, "Disposable SMS channel",
					"unity.sms.provider=clickatell\nunity.sms.clickatell.apiKey=unused-test-key", "SMS"));
			channelCreated = true;
			templates.addTemplate(new MessageTemplate(templateName, "Disposable SMS template",
					new I18nMessage(new I18nString("Code"), new I18nString("Code: ${code}")),
					"SMSAuthn", MessageType.PLAIN, templateName));
			createdTemplates.add(templateName);
			templates.addTemplate(new MessageTemplate(templateName + "-confirmation", "Disposable confirmation template",
					new I18nMessage(new I18nString("Confirmation"), new I18nString("Code: ${confirmationCode}")),
					"MobileNumberConfirmation", MessageType.PLAIN, templateName));
			createdTemplates.add(templateName + "-confirmation");
		}
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/credentials/edit");
		page.select(testId("CredentialsEditView", "CredentialsEditView.type"), scenario.type());
		if (scenario.type().equals("sms"))
			new AdminPage(driver).selectOption(testId("SMSCredentialDefinitionEditor.template"),
					templateName);
		return page;
	}

	private static By editorField(CredentialCase scenario)
	{
		return testId("CredentialsEditView", "CredentialsEditView.configuration", scenario.field());
	}

	private void save()
	{
		new VaadinPage(driver).click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/credentials");
	}

	private void assertStored(CredentialCase scenario, String expected, String description) throws Exception
	{
		CredentialDefinition stored = credentials.getCredentialDefinition(name);
		assertThat(stored.getTypeId()).isEqualTo(scenario.type());
		assertThat(stored.getDescription().getValue("en")).isEqualTo(description);
		if (scenario.jsonKey() != null)
			assertThat(JsonUtil.parse(stored.getConfiguration()).get(scenario.jsonKey()).asText()).isEqualTo(expected);
		else
			assertThat(stored.getConfiguration()).isEqualTo(scenario.type().equals("certificate") ? "" : "{}");
	}

	private static Stream<CredentialCase> requiredFieldCases()
	{
		return credentialCases().filter(scenario -> scenario.field() != null && !scenario.type().equals("fido"));
	}

	private static Stream<CredentialCase> credentialCases()
	{
		return Stream.of(
				CredentialCase.builder("password").setting("PasswordCredentialDefinitionEditor.minimumLength",
						"minLength", "12", "14").viewer("PasswordCredentialDefinitionViewer.minimumLength").build(),
				CredentialCase.builder("certificate").build(),
				CredentialCase.builder("sms").setting("SMSCredentialDefinitionEditor.codeLength", "codeLength", "7", "8")
						.viewer("SMSCredentialDefinitionViewer.codeLength").build(),
				CredentialCase.builder("otp").setting("OTPCredentialDefinitionEditor.issuer", "issuerName",
						"Selenium issuer", "Updated issuer").viewer("OTPCredentialDefinitionViewer.issuer").build(),
				CredentialCase.builder("fido").setting("FidoCredentialDefinitionEditor.hostName", "hostName",
						"initial.example.org", "updated.example.org").build(),
				CredentialCase.builder("private-key-jwt").build());
	}

	record CredentialCase(String type, String field, String jsonKey, String initial, String updated, String viewer)
	{
		static Builder builder(String type)
		{
			return new Builder(type);
		}

		@Override
		public String toString()
		{
			return type;
		}

		static final class Builder
		{
			private final String type;
			private String field;
			private String jsonKey;
			private String initial;
			private String updated;
			private String viewer;

			private Builder(String type)
			{
				this.type = type;
			}

			Builder setting(String field, String jsonKey, String initial, String updated)
			{
				this.field = field;
				this.jsonKey = jsonKey;
				this.initial = initial;
				this.updated = updated;
				return this;
			}

			Builder viewer(String viewer)
			{
				this.viewer = viewer;
				return this;
			}

			CredentialCase build()
			{
				return new CredentialCase(type, field, jsonKey, initial, updated, viewer);
			}
		}
	}
}
