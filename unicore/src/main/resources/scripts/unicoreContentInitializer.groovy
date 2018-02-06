/*
 * Script with default schema useful when Unity server is used with UNICORE middleware.
 * Otherwise should be ignored.
 *
 * Depends on defaultContentInitializer.groovy
 */

import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.Map
import java.util.Set

import pl.edu.icm.unity.exceptions.EngineException
import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax
import pl.edu.icm.unity.types.basic.AttributeStatement
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution
import pl.edu.icm.unity.types.basic.AttributeType
import pl.edu.icm.unity.types.basic.AttributesClass
import pl.edu.icm.unity.types.basic.Group
import pl.edu.icm.unity.types.basic.GroupContents
import groovy.transform.Field


@Field final String CN_ATTR = "name"
@Field final String JPEG_ATTR = "picture";
@Field final String ORG_ATTR = "organization";
@Field final String EMAIL_ATTR = "email";

@Field final String MAIN_AC = "Common attributes";
@Field final String NAMING_AC = "Common identification attributes";
@Field final String PORTAL_AC = "UNICORE portal attributes"




if (!isColdStart)
{
	log.debug("Database already initialized with content, skipping...");
	return;
}

try
{
	Map<String, AttributeType> existingATs = attributeTypeManagement.getAttributeTypesAsMap();
	if (!existingATs.containsKey(CN_ATTR) || !existingATs.containsKey(EMAIL_ATTR) ||
		!existingATs.containsKey(JPEG_ATTR) || !existingATs.containsKey(ORG_ATTR))
	{
		log.error("UNICORE contents can be only installed if standard types were installed " +
			"prior to it. Attribute types name, organization, picture and email are required.");
		return;
	}
	if (attributeClassManagement.getAttributeClasses().containsKey(PORTAL_AC))
	{
		log.info("Seems that UNICORE contents is installed, skipping.");
		return;
	}
	
	initializeAttributeClasses();
	initializeAttributeTypes();
	initializeGroups();
} catch (Exception e)
{
	log.warn("Error loading default UNICORE contents. This is not critical and usaully " +
			"means that your existing data is in conflict with the loaded contents.", e);
}



void initializeAttributeClasses()
{
	AttributesClass mainAC = new AttributesClass(MAIN_AC,
			"General purpose attributes, should be enabled for everybody",
			new HashSet<>(Arrays.asList("sys:AuthorizationRole")),
			new HashSet<String>(), false,
			new HashSet<String>());
	Map<String, AttributesClass> allAcs = attributeClassManagement.getAttributeClasses();
	if (!allAcs.containsKey(MAIN_AC))
		attributeClassManagement.addAttributeClass(mainAC);

	AttributesClass namingAC = new AttributesClass(NAMING_AC,
			"Identification attributes, should be set for everybody to enable common system features",
			new HashSet<String>(Arrays.asList(ORG_ATTR, JPEG_ATTR)),
			new HashSet<String>(Arrays.asList(CN_ATTR, EMAIL_ATTR)), false,
			new HashSet<String>());
	if (!allAcs.containsKey(NAMING_AC))
		attributeClassManagement.addAttributeClass(namingAC);
		
		
	AttributesClass unicoreAC = new AttributesClass(PORTAL_AC,
		"Attributes useful for the UNICORE portal",
		new HashSet<>(Arrays.asList(JPEG_ATTR, ORG_ATTR)),
		new HashSet<>(Arrays.asList(CN_ATTR, EMAIL_ATTR)), false,
		new HashSet<>(Arrays.asList(MAIN_AC)));
	attributeClassManagement.addAttributeClass(unicoreAC);
}

void initializeAttributeTypes()
{
	Set<AttributeType> existingATs = new HashSet<>(attributeTypeManagement.getAttributeTypes());
	
	Set<String> allowedRoles = new HashSet<>();
	allowedRoles.add("user");
	allowedRoles.add("admin");
	allowedRoles.add("server");
	allowedRoles.add("banned");
	AttributeType roleAT = new AttributeType("urn:unicore:attrType:role",
			EnumAttributeSyntax.ID, msgSrc);
	EnumAttributeSyntax roleSyntax = new EnumAttributeSyntax(allowedRoles);
	roleAT.setMinElements(1);
	roleAT.setValueSyntaxConfiguration(roleSyntax.getSerializedConfiguration());
	if (!existingATs.contains(roleAT))
		attributeTypeManagement.addAttributeType(roleAT);

	AttributeType xloginAT = new AttributeType("urn:unicore:attrType:xlogin",
			StringAttributeSyntax.ID, msgSrc);
	xloginAT.setMinElements(1);
	xloginAT.setMaxElements(16);
	StringAttributeSyntax xloginSyntax = new StringAttributeSyntax();
	xloginSyntax.setMaxLength(100);
	xloginSyntax.setMinLength(1);
	xloginAT.setValueSyntaxConfiguration(xloginSyntax.getSerializedConfiguration());
	if (!existingATs.contains(xloginAT))
		attributeTypeManagement.addAttributeType(xloginAT);
}

void initializeGroups()
{
	Group unicore = new Group("/unicore");
	groupsManagement.addGroup(unicore);
	Group servers = new Group("/unicore/servers");
	groupsManagement.addGroup(servers);
	Group users = new Group("/unicore/users");
	groupsManagement.addGroup(users);
	
	//create attribute statement for the /unicore group, which assigns proper UNICORE role basing on the subgroups membership
	AttributeStatement usersStmt = AttributeStatement.getFixedStatement(
		EnumAttribute.of("urn:unicore:attrType:role", "/unicore", "user"), null, "groups contains '/unicore/users'");
	AttributeStatement serverRoleStmt = AttributeStatement.getFixedStatement(
		EnumAttribute.of("urn:unicore:attrType:role", "/unicore", "server"), null, "groups contains '/unicore/servers'");

	unicore = groupsManagement.getContents("/unicore", GroupContents.METADATA).getGroup();
	AttributeStatement[] statements4Unicore = [usersStmt, serverRoleStmt];
	unicore.setAttributeStatements(statements4Unicore);
	groupsManagement.updateGroup("/unicore", unicore);

	//create attribute statement for the / group, which assigns Inspector authZ role all members of the /unicore/servers group
	AttributeStatement serversStmt = AttributeStatement.getFixedStatement(
		EnumAttribute.of("sys:AuthorizationRole", "/", "Inspector"), null, "groups contains '/unicore/servers'");
	serversStmt.setConflictResolution(ConflictResolution.overwrite);
	Group root = groupsManagement.getContents("/", GroupContents.METADATA).getGroup();
	AttributeStatement[] statements4Root = root.getAttributeStatements() + [serversStmt];
	root.setAttributeStatements(statements4Root);
	groupsManagement.updateGroup("/", root);

}


