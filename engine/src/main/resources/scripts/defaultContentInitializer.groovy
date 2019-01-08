/*
 * Script with default initialization logic of Unity server. 
 * Basically should be always used. 
 * 
 * Sets up default authorization policy, couple of commonly useful attribute types
 * and assigns common name to the default admin user.
 * 
 * Note that other default/example scripts sometimes depend on contents 
 * (typically attribute types) created by this script. 
 */

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider
import pl.edu.icm.unity.stdext.utils.ContactMobileMetadataProvider
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import groovy.transform.Field


@Field final String NAME_ATTR = "name"
@Field final String EMAIL_ATTR = "email";
@Field final String MOBILE_ATTR = "mobile";


//run only if it is the first start of the server on clean DB.
if (!isColdStart)
{
	log.info("Database already initialized with content, skipping...");
	return;
}


log.info("Adding the default server contents...");

try
{
	initializeDefaultAuthzPolicy();
	initializeCommonAttributeTypes();
	assignNameToAdmin();
		
} catch (Exception e)
{
	log.warn("Error loading default contents", e);
}


void initializeDefaultAuthzPolicy() throws EngineException
{
	//create attribute statement for the root group, which assigns regular user role
	//to all its members
	AttributeStatement everybodyStmt = AttributeStatement.getFixedEverybodyStatement(
		EnumAttribute.of("sys:AuthorizationRole", "/", "Regular User"));
	Group rootGroup = groupsManagement.getContents("/", GroupContents.METADATA).getGroup();
	AttributeStatement[] statements = [everybodyStmt];
	rootGroup.setAttributeStatements(statements);
	groupsManagement.updateGroup("/", rootGroup);
}


void initializeCommonAttributeTypes() throws EngineException
{
	//here we create couple of useful attribute types, paying attention not to 
	// create those which are already defined. This check shouldn't be necessary 
	// when coldStart check is done, it is relevant only if this check is turned off. 
	
	Map<String, AttributeType> existingATs = attributeTypeManagement.getAttributeTypesAsMap();
	
	//The name attribute will be marked as special attribute providing owner's displayed name.
	AttributeType name = new AttributeType(NAME_ATTR, StringAttributeSyntax.ID, msgSrc);
	name.setMinElements(1);
	StringAttributeSyntax nameSyntax = new StringAttributeSyntax();
	nameSyntax.setMaxLength(100);
	nameSyntax.setMinLength(2);
	name.setValueSyntaxConfiguration(nameSyntax.getSerializedConfiguration());
	name.getMetadata().put(EntityNameMetadataProvider.NAME, "");
	if (!existingATs.containsKey(NAME_ATTR))
		attributeTypeManagement.addAttributeType(name);


	//The email attribute will be marked as special attribute providing owner's contact e-mail.
	AttributeType verifiableEmail = new AttributeType(EMAIL_ATTR, 
		VerifiableEmailAttributeSyntax.ID, msgSrc);
	verifiableEmail.setMinElements(1);
	verifiableEmail.setMaxElements(5);
	verifiableEmail.getMetadata().put(ContactEmailMetadataProvider.NAME, "");
	if (!existingATs.containsKey(EMAIL_ATTR))
		attributeTypeManagement.addAttributeType(verifiableEmail);
		
	//The mobile attribute will be marked as special attribute providing owner's contact mobile.
	AttributeType verifiableMobile = new AttributeType(MOBILE_ATTR, 
		VerifiableMobileNumberAttributeSyntax.ID, msgSrc);
	verifiableMobile.setMinElements(1);
	verifiableMobile.setMaxElements(5);
	verifiableMobile.getMetadata().put(ContactMobileMetadataProvider.NAME, "");
	if (!existingATs.containsKey(MOBILE_ATTR))
		attributeTypeManagement.addAttributeType(verifiableMobile);	
	
}

void assignNameToAdmin() throws EngineException
{
	//admin user has no "name" - let's assign one.
	String adminU = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_USER);
	if (adminU == null)
		return;
	Attribute nameA = StringAttribute.of(NAME_ATTR, "/", "Default Administrator");
	EntityParam entity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, adminU));
	try
	{
		if (attributesManagement.getAttributes(entity, "/", NAME_ATTR).isEmpty())
			attributesManagement.createAttribute(entity, nameA);
	} catch (IllegalIdentityValueException e)
	{
		//ok - no default admin, no default Name.
	}
}

