/*
 * Script adding demo UNICORE server DN to /unicore/servers group. Useful for integration testing with unicore 
 * quickstart distribution. Should not be used for production!  
 *
 * Depends on unicoreContentInitializer.groovy
 */

import java.util.Map;

import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

import groovy.transform.Field


@Field final String CN_ATTR = "cn"


if (!isColdStart)
{
	log.debug("Database already initialized with content, skipping...");
	return;
}

try
{
	Map<String, AttributeType> existingATs = attributeTypeManagement.getAttributeTypesAsMap();
	if (!existingATs.containsKey("urn:unicore:attrType:role"))
	{
		log.error("UNICORE demo server can be only installed if the main UNICORE initialization was performed.");
		return;
	}

	addDemoServer();

} catch (Exception e)
{
	log.warn("Error loading demo UNICORE contents. This is not critical and usaully " +
			"means that your existing data is in conflict with the loaded contents.", e);
}


void addDemoServer()
{
	IdentityParam unicoreClient = new IdentityParam(X500Identity.ID, "CN=Demo UNICORE/X,O=UNICORE,C=EU");
	Identity unicoreClientA = entityManagement.addEntity(unicoreClient, "Certificate",
			EntityState.valid, false);
	log.warn("DEMO UNICORE client user was created using the demo (insecure, publicly known) certificate identity");
	EntityParam demoServer = new EntityParam(unicoreClientA.getEntityId());
	groupsManagement.addMemberFromParent("/unicore", demoServer);
	groupsManagement.addMemberFromParent("/unicore/servers", demoServer);
	
	Attribute cnA = StringAttribute.of(CN_ATTR, "/", "Demo UNICORE/X");
	attributesManagement.setAttribute(demoServer, cnA, false);
}
