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
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import groovy.transform.Field


@Field final String CN_ATTR = "cn"
@Field final String JPEG_ATTR = "jpegPhoto";
@Field final String ORG_ATTR = "o";
@Field final String EMAIL_ATTR = "email";


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
	assignCnToAdmin();
		
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
	
	AttributeType userPicture = new AttributeType(JPEG_ATTR, JpegImageAttributeSyntax.ID, msgSrc);
	JpegImageAttributeSyntax jpegSyntax = new JpegImageAttributeSyntax();
	jpegSyntax.setMaxSize(2000000);
	jpegSyntax.setMaxWidth(120);
	jpegSyntax.setMaxHeight(120);
	userPicture.setMinElements(1);
	userPicture.setValueSyntaxConfiguration(jpegSyntax.getSerializedConfiguration());
	if (!existingATs.containsKey(JPEG_ATTR))
		attributeTypeManagement.addAttributeType(userPicture);

	//The cn attribute will be marked as special attribute providing owner's displayed name.
	AttributeType cn = new AttributeType(CN_ATTR, StringAttributeSyntax.ID, msgSrc);
	cn.setMinElements(1);
	StringAttributeSyntax cnSyntax = new StringAttributeSyntax();
	cnSyntax.setMaxLength(100);
	cnSyntax.setMinLength(2);
	cn.setValueSyntaxConfiguration(cnSyntax.getSerializedConfiguration());
	cn.getMetadata().put(EntityNameMetadataProvider.NAME, "");
	if (!existingATs.containsKey(CN_ATTR))
		attributeTypeManagement.addAttributeType(cn);

	AttributeType org = new AttributeType(ORG_ATTR, StringAttributeSyntax.ID, msgSrc);
	StringAttributeSyntax orgSyntax = new StringAttributeSyntax();
	org.setMinElements(1);
	org.setMaxElements(10);
	orgSyntax.setMaxLength(33);
	orgSyntax.setMinLength(2);
	org.setValueSyntaxConfiguration(orgSyntax.getSerializedConfiguration());
	if (!existingATs.containsKey(ORG_ATTR))
		attributeTypeManagement.addAttributeType(org);

	//The email attribute will be marked as special attribute providing owner's contact e-mail.
	AttributeType verifiableEmail = new AttributeType(EMAIL_ATTR, 
		VerifiableEmailAttributeSyntax.ID, msgSrc);
	verifiableEmail.setMinElements(1);
	verifiableEmail.setMaxElements(5);
	verifiableEmail.getMetadata().put(ContactEmailMetadataProvider.NAME, "");
	if (!existingATs.containsKey(EMAIL_ATTR))
		attributeTypeManagement.addAttributeType(verifiableEmail);
}

void assignCnToAdmin() throws EngineException
{
	//admin user has no "name" - let's assign one.
	String adminU = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_USER);
	Attribute cnA = StringAttribute.of(CN_ATTR, "/", "Default Administrator");
	EntityParam entity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, adminU));
	try
	{
		if (attributesManagement.getAttributes(entity, "/", CN_ATTR).isEmpty())
			attributesManagement.setAttribute(entity, cnA, false);
	} catch (IllegalIdentityValueException e)
	{
		//ok - no default admin, no default CN.
	}
}

