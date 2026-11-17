package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.fido.credential.FidoCredential;
import io.imunity.otp.HashFunction;
import io.imunity.otp.OTPCredentialDefinition;
import io.imunity.otp.OTPGenerationParams;
import io.imunity.otp.OTPResetSettings;
import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredential;

@Tag("admin-authentication")
class LocalAuthenticatorTest extends SeleniumTestBase
{
	private final String name = "ui-authenticator-" + UUID.randomUUID();
	private final String credentialName = "ui-local-" + UUID.randomUUID();
	private boolean credentialCreated;

	@Autowired
	@Qualifier("insecure")
	private AuthenticatorManagement authenticators;
	@Autowired
	@Qualifier("insecure")
	private CredentialManagement credentials;

	@AfterEach
	void removeFixtures() throws Exception
	{
		try
		{
			if (authenticators.getAuthenticators(null).stream().anyMatch(authenticator -> authenticator.getId().equals(name)))
				authenticators.removeAuthenticator(name);
		} finally
		{
			if (credentialCreated)
				credentials.removeCredentialDefinition(credentialName);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("authenticatorCases")
	void shouldCreateAndReloadLocalAuthenticatorSettings(AuthenticatorCase scenario) throws Exception
	{
		CredentialDefinition definition = new CredentialDefinition(scenario.type(), credentialName,
				new I18nString(credentialName), new I18nString("Disposable credential"));
		definition.setConfiguration(configuration(scenario.type()));
		credentials.addCredentialDefinition(definition);
		credentialCreated = true;

		VaadinPage page = new VaadinPage(driver);
		AdminPage editor = new AdminPage(driver);
		page.login(baseUrl, "/admin", "ui-admin", "Ui-admin!42");
		driver.get(baseUrl + "/admin/facilities/authenticator/new");
		String label = scenario.label();
		if (label == null)
			label = authenticators.getAvailableAuthenticatorsTypes().stream()
					.filter(type -> type.getVerificationMethod().equals(scenario.type()))
					.map(type -> scenario.type() + " (" + type.getVerificationMethodDescription() + ")")
					.findFirst().orElseThrow();
		page.select(testId("AuthenticatorEditView.type"), label);
		page.fill(testId("BaseAuthenticatorEditor.name"), name);
		page.select(testId("BaseLocalAuthenticatorEditor.credential"), credentialName);
		editor.openInteractiveSettings();
		page.fill(testId(scenario.editor() + ".retrievalName"), "Initial login label");
		save();
		assertStored(scenario, "Initial login label");

		driver.get(baseUrl + "/admin/facilities/authenticator/" + name);
		assertThat(page.value(testId("BaseAuthenticatorEditor.name"))).isEqualTo(name);
		assertThat(page.value(testId("BaseLocalAuthenticatorEditor.credential"))).isEqualTo(credentialName);
		editor.openInteractiveSettings();
		assertThat(page.value(testId(scenario.editor() + ".retrievalName"))).isEqualTo("Initial login label");
		page.fill(testId(scenario.editor() + ".retrievalName"), "Updated login label");
		save();
		driver.get(baseUrl + "/admin/facilities/authenticator/" + name);
		editor.openInteractiveSettings();
		assertThat(page.value(testId(scenario.editor() + ".retrievalName"))).isEqualTo("Updated login label");
		assertStored(scenario, "Updated login label");
		screenshot("Reloaded local authenticator settings");
	}

	private String configuration(String type) throws Exception
	{
		return switch (type)
		{
			case "password" -> credentials.getCredentialDefinition("sys:password").getConfiguration();
			case "certificate" -> "";
			case "sms" -> new SMSCredential().getSerializedConfiguration().toString();
			case "fido" -> new FidoCredential().serialize();
			case "otp" -> JsonUtil.toJsonString(new OTPCredentialDefinition(
					new OTPGenerationParams(6, HashFunction.SHA1, 30), "Selenium", 3,
					new OTPResetSettings(false, 6, null, null, OTPResetSettings.ConfirmationMode.EMAIL),
					Optional.empty()));
			default -> throw new IllegalArgumentException(type);
		};
	}

	private void save()
	{
		new VaadinPage(driver).click(testId("EditViewActionLayoutFactory.save"));
		waitForPageLoadByURL("/facilities");
	}

	private void assertStored(AuthenticatorCase scenario, String label) throws Exception
	{
		AuthenticatorInfo stored = authenticators.getAuthenticator(name);
		assertThat(stored.getTypeDescription().getVerificationMethod()).isEqualTo(scenario.type());
		assertThat(stored.getLocalCredentialName()).contains(credentialName);
		Properties properties = new Properties();
		properties.load(new StringReader(stored.getConfiguration()));
		assertThat(properties.getProperty(scenario.prefix() + "name.en")).isEqualTo(label);
	}

	private static Stream<AuthenticatorCase> authenticatorCases()
	{
		return Stream.of(
				AuthenticatorCase.builder("password", "PasswordAuthenticatorEditor", "retrieval.password.")
						.label("Password").build(),
				AuthenticatorCase.builder("certificate", "CertificateAuthenticatorEditor", "retrieval.tls.")
						.label("X.509 Certificate").build(),
				AuthenticatorCase.builder("sms", "SMSAuthenticatorEditor", "retrieval.sms.").label("SMS Code").build(),
				AuthenticatorCase.builder("otp", "OTPAuthenticatorEditor", "otp.").build(),
				AuthenticatorCase.builder("fido", "FidoAuthenticatorEditor", "fido.").build());
	}

	record AuthenticatorCase(String type, String editor, String prefix, String label)
	{
		static Builder builder(String type, String editor, String prefix)
		{
			return new Builder(type, editor, prefix);
		}

		@Override
		public String toString()
		{
			return type;
		}

		static final class Builder
		{
			private final String type;
			private final String editor;
			private final String prefix;
			private String label;

			private Builder(String type, String editor, String prefix)
			{
				this.type = type;
				this.editor = editor;
				this.prefix = prefix;
			}

			Builder label(String label)
			{
				this.label = label;
				return this;
			}

			AuthenticatorCase build()
			{
				return new AuthenticatorCase(type, editor, prefix, label);
			}
		}
	}
}
