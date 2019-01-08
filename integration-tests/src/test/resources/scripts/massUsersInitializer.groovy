/*
 * Creates many users
 *
 * Depends on defaultContentInitializer.groovy
 */
import pl.edu.icm.unity.engine.server.EngineInitialization
import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.stdext.identity.X500Identity
import pl.edu.icm.unity.types.basic.Attribute
import pl.edu.icm.unity.types.basic.AttributeType
import pl.edu.icm.unity.types.basic.EntityParam
import pl.edu.icm.unity.types.basic.EntityState
import pl.edu.icm.unity.types.basic.Group
import pl.edu.icm.unity.types.basic.GroupContents
import pl.edu.icm.unity.types.basic.Identity
import pl.edu.icm.unity.types.basic.IdentityParam
import pl.edu.icm.unity.types.basic.VerifiableEmail
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo
import groovy.transform.Field


@Field final String NAME_ATTR = "name"
@Field final String EMAIL_ATTR = "email";
@Field final int ENTITIES = 10000;
@Field final int GROUPS = 3000;

//if (!isColdStart)
//{
//	log.info("Database already initialized with content, skipping...");
//	return;
//}

log.info("Creating demo content...");

try
{
	GroupContents rootContents = groupsManagement.getContents("/", GroupContents.GROUPS);
	
	Map<String, AttributeType> existingATs = attributeTypeManagement.getAttributeTypesAsMap();
	if (!existingATs.containsKey(NAME_ATTR) || !existingATs.containsKey(EMAIL_ATTR))
	{
		log.error("Demo contents can be only installed if standard types were installed " +  
			"prior to it. Attribute types cn, o and email are required.");
		return;
	}
	
	//createExampleGroups();
	//for (int i=0; i<ENTITIES; i++)
	//	createExampleUser(i);
	//setCredentialForFirst();
	addUsersToAllGroups();
	
} catch (Exception e)
{
	log.warn("Error loading demo contents. This can happen and by far is not critical. " +
			"It means that demonstration contents was not loaded to your database, " +
			"usaully due to conflict with its existing data", e);
}

void createExampleGroups()
{
	groupsManagement.addGroup(new Group("/root"));
	for (int i=0; i<GROUPS; i++)
	{
		String grp = "/root/grp" + i;
		groupsManagement.addGroup(new Group(grp));
		log.warn("Group " + grp + " was created");
	}
}

void addUsersToAllGroups()
{
	for (int e=4; e<104; e++)
	{
		EntityParam entityP = new EntityParam(e);
		groupsManagement.addMemberFromParent("/root", entityP);
		for (int i=0; i<GROUPS; i++)
		{
			String grp = "/root/grp" + i;
			groupsManagement.addMemberFromParent(grp, entityP);
			log.warn("Added user to group " + grp);
		}
	}
}

void setCredentialForFirst()
{
	EntityParam entityP = new EntityParam(3);
	Attribute a = EnumAttribute.of("sys:AuthorizationRole", "/", "System Manager");
	attributesManagement.createAttribute(entityP, a);
	PasswordToken pToken = new PasswordToken("the!test12");
	entityCredentialManagement.setEntityCredential(entityP, EngineInitialization.DEFAULT_CREDENTIAL, pToken.toJson());
}


void createExampleUser(int suffix)
{
	IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "demo-user-" + suffix);
	Identity base = entityManagement.addEntity(toAdd, EntityState.valid, false);

	IdentityParam toAddDn = new IdentityParam(X500Identity.ID, "CN=Demo user " + suffix);
	EntityParam entityP = new EntityParam(base.getEntityId());
	
	entityManagement.addIdentity(toAddDn, entityP, true);

	//Attribute a = EnumAttribute.of("sys:AuthorizationRole", "/", "Regular User");
	//attributesManagement.createAttribute(entityP, a);

	groupsManagement.addMemberFromParent("/A", entityP);

	VerifiableEmail emailVal = new VerifiableEmail("some" + suffix + "@example.com", new ConfirmationInfo(true));
	emailVal.getConfirmationInfo().setConfirmationDate(System.currentTimeMillis());
	emailVal.getConfirmationInfo().setConfirmed(true);
	Attribute emailA = VerifiableEmailAttribute.of(EMAIL_ATTR, "/", emailVal);
	attributesManagement.createAttribute(entityP, emailA);

	Attribute cnA = StringAttribute.of(NAME_ATTR, "/", "Demo user " + suffix);
	attributesManagement.createAttribute(entityP, cnA);

//	PasswordToken pToken = new PasswordToken("the!test12");
	//entityCredentialManagement.setEntityCredential(entityP, EngineInitialization.DEFAULT_CREDENTIAL,
		//	pToken.toJson());
	log.warn("Demo user 'demo-user-" + suffix + "' was created");
}

