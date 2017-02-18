import java.io.File
import java.util.Collection
import java.util.HashMap
import java.util.Map
import java.util.Set

import org.apache.commons.io.FileUtils

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration
import pl.edu.icm.unity.types.I18nString
import pl.edu.icm.unity.types.authn.CredentialDefinition

log.info("Loading all configured credentials");
Collection<CredentialDefinition> definitions = credentialManagement.getCredentialDefinitions();
Map<String, CredentialDefinition> existing = new HashMap<>();
for (CredentialDefinition cd: definitions)
	existing.put(cd.getName(), cd);

Set<String> credentialsList = config.getStructuredListKeys(UnityServerConfiguration.CREDENTIALS);
for (String credentialKey: credentialsList)
{
	String name = config.getValue(credentialKey+UnityServerConfiguration.CREDENTIAL_NAME);
	String typeId = config.getValue(credentialKey+UnityServerConfiguration.CREDENTIAL_TYPE);
	String description = config.getValue(credentialKey+UnityServerConfiguration.CREDENTIAL_DESCRIPTION);
	File configFile = config.getFileValue(credentialKey+UnityServerConfiguration.CREDENTIAL_CONFIGURATION, false);

	String jsonConfiguration = FileUtils.readFileToString(configFile);
	CredentialDefinition credentialDefinition = new CredentialDefinition(typeId, name,
			new I18nString(name),
			new I18nString(description));
	credentialDefinition.setConfiguration(jsonConfiguration);
	
	if (!existing.containsKey(name))
	{
		credentialManagement.addCredentialDefinition(credentialDefinition);
		log.info(" - " + name + " [" + typeId + "]");
	}
}
