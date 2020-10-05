/*
 * Creates many users
 *
 * Depends on defaultContentInitializer.groovy
 */
import pl.edu.icm.unity.engine.server.EngineInitialization
import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.stdext.identity.X500Identity
import pl.edu.icm.unity.types.basic.Attribute
import pl.edu.icm.unity.types.basic.AttributeStatement
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution
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


@Field final String FNAME_ATTR = "firstname"
@Field final String LNAME_ATTR = "surname"
@Field final String EMAIL_ATTR = "email";
@Field final int ENTITIES = 4990;
@Field final int GROUPS = 5000;

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
	if (!existingATs.containsKey(FNAME_ATTR) || !existingATs.containsKey(EMAIL_ATTR))
	{
		log.error("Demo contents can be only installed if standard types were installed " +  
			"prior to it. Attribute types cn, o and email are required.");
		return;
	}
	
	//createExampleGroups();
	for (int i=10; i<ENTITIES; i++)
		createExampleUser(i);
	//setCredentialForFirst();
	//addUsersToAllGroups();
	
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
		Group grp = new Group("/root/grp" + i);
		AttributeStatement fnameStmt = new AttributeStatement("true", "/", ConflictResolution.skip, 
			FNAME_ATTR, "eattr['firstname']");
		AttributeStatement lnameStmt = new AttributeStatement("true", "/", ConflictResolution.skip, 
			LNAME_ATTR, "eattr['surname']");
		AttributeStatement emailStmt = new AttributeStatement("true", "/", ConflictResolution.skip, 
			EMAIL_ATTR, "eattr['email']");
		AttributeStatement[] statements = [fnameStmt, lnameStmt, emailStmt];
		grp.setAttributeStatements(statements);
		groupsManagement.addGroup(grp);
		log.info("Group " + grp + " was created");
	}
}

void addUsersToAllGroups()
{
	for (int e=0; e<ENTITIES; e++)
	{
		EntityParam entityP = new EntityParam(new IdentityParam(UsernameIdentity.ID, "demo-user-" + e));
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
	Identity base = entityManagement.addEntity(toAdd, EntityState.valid);

//	IdentityParam toAddDn = new IdentityParam(X500Identity.ID, "CN=Demo user " + suffix);
	EntityParam entityP = new EntityParam(base.getEntityId());
//	
//	entityManagement.addIdentity(toAddDn, entityP);

	//Attribute a = EnumAttribute.of("sys:AuthorizationRole", "/", "Regular User");
	//attributesManagement.createAttribute(entityP, a);

//	groupsManagement.addMemberFromParent("/A", entityP);

	VerifiableEmail emailVal = new VerifiableEmail("some" + suffix + "@example.com", new ConfirmationInfo(true));
	emailVal.getConfirmationInfo().setConfirmationDate(System.currentTimeMillis());
	emailVal.getConfirmationInfo().setConfirmed(true);
	Attribute emailA = VerifiableEmailAttribute.of(EMAIL_ATTR, "/", emailVal);
	attributesManagement.createAttribute(entityP, emailA);

	Attribute fnameA = StringAttribute.of(FNAME_ATTR, "/", "Demo " + suffix);
	attributesManagement.createAttribute(entityP, fnameA);

	Attribute lnameA = StringAttribute.of(LNAME_ATTR, "/", "User " + suffix);
	attributesManagement.createAttribute(entityP, lnameA);

//	PasswordToken pToken = new PasswordToken("the!test12");
	//entityCredentialManagement.setEntityCredential(entityP, EngineInitialization.DEFAULT_CREDENTIAL,
		//	pToken.toJson());
	log.warn("Demo user 'demo-user-" + suffix + "' was created");
}

