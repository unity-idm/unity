import java.io.File

import org.apache.commons.io.FileUtils

import pl.edu.icm.unity.base.i18n.I18nString
import pl.edu.icm.unity.base.authn.CredentialDefinition

log.info("Adding credentials test");

String name = "secured password100";
String typeId = "password";
String description = "addCredentialsTest";
File configFile = new File("src/test/resources/passwordDef.json");
String jsonConfiguration = FileUtils.readFileToString(configFile);

CredentialDefinition credentialDefinition = new CredentialDefinition(typeId, name,
		new I18nString(name),
		new I18nString(description));
credentialDefinition.setConfiguration(jsonConfiguration);

credentialManagement.addCredentialDefinition(credentialDefinition);

log.info(name + " added");
