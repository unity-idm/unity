/*
 * Script with demonstration data of Unity server.
 * Should be used in test instalations only to have example contents just 
 * after initial startup.
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


if (!isColdStart)
{
	log.info("Database already initialized with content, skipping...");
	return;
}

log.info("Creating demo content...");

try
{
	GroupContents rootContents = groupsManagement.getContents("/", GroupContents.GROUPS);
	if (rootContents.getSubGroups().contains("/A"))
	{
		log.error("Seems that demo contents is installed, skipping");
		return;
	}
	
	Map<String, AttributeType> existingATs = attributeTypeManagement.getAttributeTypesAsMap();
	if (!existingATs.containsKey(NAME_ATTR) || !existingATs.containsKey(EMAIL_ATTR))
	{
		log.error("Demo contents can be only installed if standard types were installed " +  
			"prior to it. Attribute types cn, o and email are required.");
		return;
	}
	
	createExampleGroups();
	
	createExampleAttributeTypes();
	
	createExampleUser();
	
} catch (Exception e)
{
	log.warn("Error loading demo contents. This can happen and by far is not critical. " +
			"It means that demonstration contents was not loaded to your database, " +
			"usaully due to conflict with its existing data", e);
}


void createExampleGroups()
{
	groupsManagement.addGroup(new Group("/A"));
	groupsManagement.addGroup(new Group("/A/B"));
	groupsManagement.addGroup(new Group("/A/B/C"));
	groupsManagement.addGroup(new Group("/D"));
	groupsManagement.addGroup(new Group("/D/E"));
	groupsManagement.addGroup(new Group("/D/G"));
	groupsManagement.addGroup(new Group("/D/F"));
}


void createExampleAttributeTypes()
{
	
	AttributeType height = new AttributeType("height", FloatingPointAttributeSyntax.ID, msgSrc);
	height.setMinElements(1);
	attributeTypeManagement.addAttributeType(height);
	
	AttributeType weight = new AttributeType("weight", FloatingPointAttributeSyntax.ID, msgSrc);
	weight.setMinElements(1);
	attributeTypeManagement.addAttributeType(weight);
}


void createExampleUser()
{
	IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "demo-user");
	Identity base = entityManagement.addEntity(toAdd, EntityState.valid, false);

	IdentityParam toAddDn = new IdentityParam(X500Identity.ID, "CN=Demo user");
	EntityParam entityP = new EntityParam(base.getEntityId());
	
	entityManagement.addIdentity(toAddDn, entityP, true);

	groupsManagement.addMemberFromParent("/A", entityP);

	Attribute a = EnumAttribute.of("sys:AuthorizationRole", "/", "Regular User");
	attributesManagement.createAttribute(entityP, a);

	VerifiableEmail emailVal = new VerifiableEmail("some@example.com", new ConfirmationInfo(true));
	emailVal.getConfirmationInfo().setConfirmationDate(System.currentTimeMillis());
	emailVal.getConfirmationInfo().setConfirmed(true);
	Attribute emailA = VerifiableEmailAttribute.of(EMAIL_ATTR, "/", emailVal);
	attributesManagement.createAttribute(entityP, emailA);

	Attribute cnA = StringAttribute.of(NAME_ATTR, "/", "Demo user");
	attributesManagement.createAttribute(entityP, cnA);

	PasswordToken pToken = new PasswordToken("the!test12");
	entityCredentialManagement.setEntityCredential(entityP, EngineInitialization.DEFAULT_CREDENTIAL,
			pToken.toJson());
	log.warn("Demo user 'demo-user' was created with default password. Please change it!");
}

