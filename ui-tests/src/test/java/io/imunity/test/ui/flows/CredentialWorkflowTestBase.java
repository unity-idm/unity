package io.imunity.test.ui.flows;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;

public abstract class CredentialWorkflowTestBase extends EndUserTestBase
{
	protected static final String CREDENTIAL = "ui-secondary";

	@Autowired
	@Qualifier("insecure")
	protected AuthenticatorManagement authenticators;
	@Autowired
	@Qualifier("insecure")
	protected AuthenticationFlowManagement flows;
	@Autowired
	@Qualifier("insecure")
	protected EntityCredentialManagement userCredentials;
	@Autowired
	@Qualifier("insecure")
	private CredentialManagement credentials;
	@Autowired
	@Qualifier("insecure")
	private CredentialRequirementManagement requirements;

	protected final TestUser credentialOwner(String type, String configuration) throws Exception
	{
		CredentialDefinition credential = new CredentialDefinition(type, CREDENTIAL,
				new I18nString("Selenium secondary credential"), new I18nString("Enrollment workflow"));
		credential.setConfiguration(configuration);
		credentials.addCredentialDefinition(credential);
		requirements.addCredentialRequirement(new CredentialRequirements("ui-multiple", "Enrollment fixture",
				Set.of("sys:password", CREDENTIAL)));
		TestUser user = createUser("Credential owner");
		userCredentials.setEntityCredentialRequirements(user.entity(), "ui-multiple");
		return user;
	}
}
