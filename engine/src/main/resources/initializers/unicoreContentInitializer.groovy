import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.Set

import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax
import pl.edu.icm.unity.stdext.utils.InitializerCommon
import pl.edu.icm.unity.types.basic.AttributeType
import pl.edu.icm.unity.types.basic.AttributesClass
import pl.edu.icm.unity.types.basic.Group

try
{
	commonInitializer.initializeCommonAttributeTypes();
	commonInitializer.assignCnToAdmin();
	commonInitializer.initializeCommonAttributeStatements();
	commonInitializer.initializeMainAttributeClass();
	AttributesClass unicoreAC = new AttributesClass("UNICORE portal attributes",
			"Attributes useful for the UNICORE portal",
			new HashSet<>(Arrays.asList(InitializerCommon.JPEG_ATTR,
					InitializerCommon.ORG_ATTR)),
			new HashSet<>(Arrays.asList(InitializerCommon.CN_ATTR,
					InitializerCommon.EMAIL_ATTR)), false,
			new HashSet<>(Arrays.asList(InitializerCommon.MAIN_AC)));
	if (attributeClassManagement.getAttributeClasses().containsKey(unicoreAC.getName()))
	{
		log.info("Seems that UNICORE contents is installed, skipping.");
		return;
	}
	attributeClassManagement.addAttributeClass(unicoreAC);
	
	Set<AttributeType> existingATs = new HashSet<>(attributeTypeManagement.getAttributeTypes());
	
	Set<String> allowedRoles = new HashSet<>();
	allowedRoles.add("user");
	allowedRoles.add("admin");
	allowedRoles.add("server");
	allowedRoles.add("banned");
	AttributeType roleAT = new AttributeType("urn:unicore:attrType:role",
			EnumAttributeSyntax.ID, unityMessageSource);
	EnumAttributeSyntax roleSyntax = new EnumAttributeSyntax(allowedRoles);
	roleAT.setMinElements(1);
	roleAT.setValueSyntaxConfiguration(roleSyntax.getSerializedConfiguration());
	if (!existingATs.contains(roleAT))
		attributeTypeManagement.addAttributeType(roleAT);

	AttributeType xloginAT = new AttributeType("urn:unicore:attrType:xlogin",
			StringAttributeSyntax.ID, unityMessageSource);
	xloginAT.setMinElements(1);
	xloginAT.setMaxElements(16);
	StringAttributeSyntax xloginSyntax = new StringAttributeSyntax();
	xloginSyntax.setMaxLength(100);
	xloginSyntax.setMinLength(1);
	xloginAT.setValueSyntaxConfiguration(xloginSyntax.getSerializedConfiguration());
	if (!existingATs.contains(xloginAT))
		attributeTypeManagement.addAttributeType(xloginAT);
	
	//TODO add the rest of UNICORE attribute types.
	
	Group portal = new Group("/portal");
	portal.setAttributesClasses(Collections.singleton(unicoreAC.getName()));
	groupsManagement.addGroup(portal);
	
} catch (Exception e)
{
	log.warn("Error loading default UNICORE contents. This is not critical and usaully " +
			"means that your existing data is in conflict with the loaded contents.", e);
}
