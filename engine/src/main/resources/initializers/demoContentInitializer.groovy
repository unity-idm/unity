import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute
import pl.edu.icm.unity.stdext.credential.PasswordToken
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.stdext.identity.X500Identity
import pl.edu.icm.unity.stdext.utils.InitializerCommon
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

try
{
	commonInitializer.initializeCommonAttributeTypes();
	commonInitializer.assignCnToAdmin();
	
	GroupContents rootContents = groupsManagement.getContents("/", GroupContents.GROUPS);
	if (rootContents.getSubGroups().contains("/A"))
	{
		log.error("Seems that demo contents is installed, skipping");
		return;
	}
	groupsManagement.addGroup(new Group("/A"));
	groupsManagement.addGroup(new Group("/A/B"));
	groupsManagement.addGroup(new Group("/A/B/C"));
	groupsManagement.addGroup(new Group("/D"));
	groupsManagement.addGroup(new Group("/D/E"));
	groupsManagement.addGroup(new Group("/D/G"));
	groupsManagement.addGroup(new Group("/D/F"));
	
	AttributeType userPicture = new AttributeType("picture", JpegImageAttributeSyntax.ID, unityMessageSource);
	JpegImageAttributeSyntax picSyntax = new JpegImageAttributeSyntax();
	picSyntax.setMaxSize(1400000);
	picSyntax.setMaxWidth(900);
	picSyntax.setMaxHeight(900);
	userPicture.setValueSyntaxConfiguration(picSyntax.getSerializedConfiguration());
	userPicture.setMaxElements(10);
	attributeTypeManagement.addAttributeType(userPicture);

	AttributeType name = new AttributeType("name", StringAttributeSyntax.ID, unityMessageSource);
	name.setMinElements(1);
	StringAttributeSyntax namesyntax = new StringAttributeSyntax();
	namesyntax.setMaxLength(100);
	namesyntax.setMinLength(2);
	name.setValueSyntaxConfiguration(namesyntax.getSerializedConfiguration());
	attributeTypeManagement.addAttributeType(name);

	
	AttributeType postalcode = new AttributeType("postalcode", StringAttributeSyntax.ID, unityMessageSource);
	postalcode.setMinElements(0);
	postalcode.setMaxElements(Integer.MAX_VALUE);
	StringAttributeSyntax pcsyntax = new StringAttributeSyntax();
	pcsyntax.setRegexp("[0-9][0-9]-[0-9][0-9][0-9]");
	pcsyntax.setMaxLength(6);
	postalcode.setValueSyntaxConfiguration(pcsyntax.getSerializedConfiguration());
	attributeTypeManagement.addAttributeType(postalcode);
	
	
	
	AttributeType height = new AttributeType("height", FloatingPointAttributeSyntax.ID, unityMessageSource);
	height.setMinElements(1);
	attributeTypeManagement.addAttributeType(height);

	IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, "demo-user");
	Identity base = entityManagement.addEntity(toAdd, "Password requirement", EntityState.valid, false);

	IdentityParam toAddDn = new IdentityParam(X500Identity.ID, "CN=Demo user");
	entityManagement.addIdentity(toAddDn, new EntityParam(base.getEntityId()), true);

	groupsManagement.addMemberFromParent("/A", new EntityParam(base.getEntityId()));

	Attribute a = EnumAttribute.of("sys:AuthorizationRole", "/", "Regular User");
	attributesManagement.setAttribute(new EntityParam(base.getEntityId()), a, false);

	Attribute orgA = StringAttribute.of("o", "/", "Example organization", "org2", "org3");
	attributesManagement.setAttribute(new EntityParam(base.getEntityId()), orgA, false);

	VerifiableEmail emailVal = new VerifiableEmail("some@email.com", new ConfirmationInfo(true));
	emailVal.getConfirmationInfo().setConfirmationDate(System.currentTimeMillis());
	emailVal.getConfirmationInfo().setConfirmed(true);
	Attribute emailA = VerifiableEmailAttribute.of(
			InitializerCommon.EMAIL_ATTR, "/", emailVal);
	attributesManagement.setAttribute(new EntityParam(base.getEntityId()), emailA, false);

	Attribute cnA = StringAttribute.of("cn", "/", "Hiper user");
	attributesManagement.setAttribute(new EntityParam(base.getEntityId()), cnA, false);

	PasswordToken pToken = new PasswordToken("the!test1");
	entityCredentialManagement.setEntityCredential(new EntityParam(base.getEntityId()), "Password credential",
			pToken.toJson());
} catch (Exception e)
{
	log.warn("Error loading demo contents. This can happen and by far is not critical. " +
			"It means that demonstration contents was not loaded to your database, " +
			"usaully due to conflict with its existing data", e);
}