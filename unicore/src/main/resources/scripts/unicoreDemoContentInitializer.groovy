/*
 * Script adding demo UNICORE server DN to /unicore/servers group. Useful for integration testing with unicore 
 * quickstart distribution. Should not be used for production!  
 *
 * Depends on unicoreContentInitializer.groovy
 */

import java.util.Map;

import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

import groovy.transform.Field


@Field final String CN_ATTR = "name"


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

	addDemoServer("CN=Demo UNICORE/X,O=UNICORE,C=EU", "Demo UNICORE/X");
	addDemoServer("CN=Demo Registry,O=UNICORE,C=EU", "Demo UNICORE Registry");
	addDemoServer("CN=Demo Servorch,O=UNICORE,C=EU", "Demo UNICORE Servorch");
	addDemoServer("CN=Demo Workflow,O=UNICORE,C=EU", "Demo UNICORE Workflow");

} catch (Exception e)
{
	log.warn("Error loading demo UNICORE contents. This is not critical and usaully " +
			"means that your existing data is in conflict with the loaded contents.", e);
}


void addDemoServer(String dn, String cn)
{
	IdentityParam unicoreClient = new IdentityParam(X500Identity.ID, dn);
	Identity unicoreClientA = entityManagement.addEntity(unicoreClient, "sys:all",
			EntityState.valid, false);
	log.warn("DEMO UNICORE client with DN {} was created using the demo (insecure, publicly known) certificate identity", dn);
	EntityParam demoServer = new EntityParam(unicoreClientA.getEntityId());
	groupsManagement.addMemberFromParent("/unicore", demoServer);
	groupsManagement.addMemberFromParent("/unicore/servers", demoServer);
	
	Attribute cnA = StringAttribute.of(CN_ATTR, "/", cn);
	attributesManagement.createAttribute(demoServer, cnA);
}
