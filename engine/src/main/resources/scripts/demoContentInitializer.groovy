/*
 * Script with demonstration data of Unity server.
 * Should be used in test instalations only to have example contents just 
 * after initial startup.
 *
 * Depends on defaultContentInitializer.groovy
 */

import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute
import pl.edu.icm.unity.stdext.credential.PasswordToken
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


@Field final String CN_ATTR = "cn"
@Field final String ORG_ATTR = "o";
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
	if (!existingATs.containsKey(CN_ATTR) || !existingATs.containsKey(EMAIL_ATTR) || 
		!existingATs.containsKey(ORG_ATTR))
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
	AttributeType name = new AttributeType("name", StringAttributeSyntax.ID, msgSrc);
	name.setMinElements(1);
	StringAttributeSyntax namesyntax = new StringAttributeSyntax();
	namesyntax.setMaxLength(100);
	namesyntax.setMinLength(2);
	name.setValueSyntaxConfiguration(namesyntax.getSerializedConfiguration());
	attributeTypeManagement.addAttributeType(name);

	
	AttributeType postalcode = new AttributeType("postalcode", StringAttributeSyntax.ID, msgSrc);
	postalcode.setMinElements(0);
	postalcode.setMaxElements(Integer.MAX_VALUE);
	StringAttributeSyntax pcsyntax = new StringAttributeSyntax();
	pcsyntax.setRegexp("[0-9][0-9]-[0-9][0-9][0-9]");
	pcsyntax.setMaxLength(6);
	postalcode.setValueSyntaxConfiguration(pcsyntax.getSerializedConfiguration());
	attributeTypeManagement.addAttributeType(postalcode);
	
	AttributeType height = new AttributeType("height", FloatingPointAttributeSyntax.ID, msgSrc);
	height.setMinElements(1);
	attributeTypeManagement.addAttributeType(height);
}


void createExampleUser()
{
	IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "demo-user");
	Identity base = entityManagement.addEntity(toAdd, "Password requirement", EntityState.valid, false);

	IdentityParam toAddDn = new IdentityParam(X500Identity.ID, "CN=Demo user");
	EntityParam entityP = new EntityParam(base.getEntityId());
	
	entityManagement.addIdentity(toAddDn, entityP, true);

	groupsManagement.addMemberFromParent("/A", entityP);

	Attribute a = EnumAttribute.of("sys:AuthorizationRole", "/", "Regular User");
	attributesManagement.setAttribute(entityP, a, false);

	Attribute orgA = StringAttribute.of("o", "/", "Example organization", "org2", "org3");
	attributesManagement.setAttribute(entityP, orgA, false);

	VerifiableEmail emailVal = new VerifiableEmail("some@email.com", new ConfirmationInfo(true));
	emailVal.getConfirmationInfo().setConfirmationDate(System.currentTimeMillis());
	emailVal.getConfirmationInfo().setConfirmed(true);
	Attribute emailA = VerifiableEmailAttribute.of("email", "/", emailVal);
	attributesManagement.setAttribute(entityP, emailA, false);

	Attribute cnA = StringAttribute.of("cn", "/", "Hiper user");
	attributesManagement.setAttribute(entityP, cnA, false);

	PasswordToken pToken = new PasswordToken("the!test1");
	entityCredentialManagement.setEntityCredential(entityP, "Password credential",
			pToken.toJson());
	log.warn("Demo user 'demo-user' was created with default password. Please change it!");
}

