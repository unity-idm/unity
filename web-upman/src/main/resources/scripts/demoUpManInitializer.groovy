import java.util.Arrays;
import groovy.transform.Field
import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.stdext.identity.EmailIdentity
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.types.I18nString
import pl.edu.icm.unity.types.basic.Attribute
import pl.edu.icm.unity.types.basic.EntityParam
import pl.edu.icm.unity.types.basic.EntityState
import pl.edu.icm.unity.types.basic.Group
import pl.edu.icm.unity.types.basic.GroupContents
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration
import pl.edu.icm.unity.types.basic.Identity
import pl.edu.icm.unity.types.basic.IdentityParam
import pl.edu.icm.unity.types.basic.IdentityTaV
import pl.edu.icm.unity.engine.InitializerCommon
import pl.edu.icm.unity.engine.server.EngineInitialization
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings
import pl.edu.icm.unity.types.registration.RegistrationForm
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder
import pl.edu.icm.unity.types.registration.EnquiryForm
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam
import pl.edu.icm.unity.types.registration.layout.FormLayoutSettings
import pl.edu.icm.unity.types.translation.ProfileType
import pl.edu.icm.unity.types.translation.TranslationAction
import pl.edu.icm.unity.types.translation.TranslationProfile
import pl.edu.icm.unity.types.translation.TranslationRule
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import com.google.common.collect.Lists
import org.springframework.util.StringUtils

@Field final String NAME_ATTR = "name"
@Field final String FIRSTNAME_ATTR = "firstname"
@Field final String SURNAME_ATTR = "surname"
@Field final String TEAMNAME_ATTR = "teamname"

@Field final String ROLE_ATTR = "sys:ProjectManagementRole"

@Field final String FBI_GROUP = "/projects/FBI"
@Field final String UNIV_GROUP = "/projects/univ"

@Field final String UNIV_LOGO_SMALL = "https://upload.wikimedia.org/wikipedia/en/thumb/3/3d/University_of_London.svg/150px-University_of_London.svg.png"
@Field final String FBI_LOGO_SMALL = "https://upload.wikimedia.org/wikipedia/commons/9/9f/FBISeal.png"

//TODO REMOVE
@Field final String FBI_UPDATE_ENQ_FORM = "FBIUpdateEnquiry"
@Field final String UNIV_UPDATE_ENQ_FORM = "UnivUpdateEnquiry"

if (!isColdStart)
{
	log.info("Database already initialized with demo UpMan groups, skipping...");
	return;
}

createGroupsStructure();

for (int i=0; i<30; i++)
	createExampleUser(i);

addDemoUserAsManager();


List<AttributeRegistrationParam> FBIAttrs = Arrays.asList(getAttributeParam(TEAMNAME_ATTR, FBI_GROUP, true));
List<AttributeRegistrationParam> UnivAttrs = Arrays.asList(getAttributeParam(FIRSTNAME_ATTR, UNIV_GROUP, true), 
	getAttributeParam(SURNAME_ATTR, UNIV_GROUP, true));

RegistrationForm fbiRegistrationForm = groupDelegationConfigGenerator.generateRegistrationForm(FBI_GROUP, FBI_LOGO_SMALL, Arrays.asList(TEAMNAME_ATTR))
RegistrationForm univRegistrationForm = groupDelegationConfigGenerator.generateRegistrationForm(UNIV_GROUP, UNIV_LOGO_SMALL, Arrays.asList(FIRSTNAME_ATTR, SURNAME_ATTR))
registrationsManagement.addForm(fbiRegistrationForm);  
registrationsManagement.addForm(univRegistrationForm);  

EnquiryForm fbiJoinEnquiryForm = groupDelegationConfigGenerator.generateJoinEnquiryForm(FBI_GROUP, FBI_LOGO_SMALL)
EnquiryForm univJoinEnquiryForm = groupDelegationConfigGenerator.generateJoinEnquiryForm(UNIV_GROUP, UNIV_LOGO_SMALL)
enquiryManagement.addEnquiry(fbiJoinEnquiryForm);  		 
enquiryManagement.addEnquiry(univJoinEnquiryForm);  		 

//TODO REMOVE, REPLACE BY GENERATOR
addEnquiryForm(EnquiryType.STICKY, FBI_UPDATE_ENQ_FORM, "",  FBI_GROUP, FBI_GROUP, FBIAttrs);
addEnquiryForm(EnquiryType.STICKY, UNIV_UPDATE_ENQ_FORM, "", UNIV_GROUP, UNIV_GROUP, UnivAttrs);


setGroupDelegationConfig(UNIV_GROUP,
				UNIV_LOGO_SMALL,
				univRegistrationForm.getName(),
				univJoinEnquiryForm.getName(),
				UNIV_UPDATE_ENQ_FORM,
				Arrays.asList(FIRSTNAME_ATTR, SURNAME_ATTR));

setGroupDelegationConfig(FBI_GROUP,
				FBI_LOGO_SMALL,
				fbiRegistrationForm.getName(),
				fbiJoinEnquiryForm.getName(),
				FBI_UPDATE_ENQ_FORM,
				Arrays.asList(TEAMNAME_ATTR));

AttributeRegistrationParam getAttributeParam(String type, String group, boolean optional)
{
    AttributeRegistrationParam param = new AttributeRegistrationParam();
	param.setAttributeType(type);
	param.setGroup(group);
	param.setRetrievalSettings(ParameterRetrievalSettings.interactive);
	param.setOptional(optional);
	param.setLabel(StringUtils.capitalize(type) + ":");
	return param;    
}


void addRegistrationForm(String name, String logo, String groupPath, List<AttributeRegistrationParam> extraAttrs)
{
	log.info("Creating registration form " + name);
	
	RegistrationFormNotifications not = new RegistrationFormNotifications();
	not.setSubmittedTemplate("registrationRequestSubmitted");
	not.setInvitationTemplate("invitationWithCode");
	
	String[] action = [AutomaticRequestAction.accept.toString()]
	TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				action);
	String[] action2 = ["\"" + groupPath + "\""]
	TranslationAction a2 = new TranslationAction(AddToGroupActionFactory.NAME,
				action2);			
			
	List<TranslationRule> rules = Lists.newArrayList(new TranslationRule("validCode == true", a1), new TranslationRule("true", a2))
	
	TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);
	
	FormLayoutSettings lsettings = new FormLayoutSettings();
	lsettings.setLogoURL(logo);
	lsettings.setColumnWidth(21);
	lsettings.setColumnWidthUnit("em");
	
	RegistrationForm form = new RegistrationFormBuilder()
				.withName(name)
				.withNotificationsConfiguration(not)
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withAddedCredentialParam(
						new CredentialRegistrationParam(EngineInitialization.DEFAULT_CREDENTIAL, null, null))	
				.withAddedIdentityParam()
					.withIdentityType(EmailIdentity.ID)
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endIdentityParam()
				.withAddedAttributeParam()
			    	.withLabel(StringUtils.capitalize(NAME_ATTR) + ":")
			    	.withAttributeType(NAME_ATTR).withGroup("/")
					.withOptional(false)
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withShowGroups(false)
				.endAttributeParam()
				.withAddedGroupParam()
					.withLabel("Select group:")
					.withGroupPath(groupPath + "/*?/**")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withMultiselect(true)
				.endGroupParam()
				.withFormLayoutSettings(lsettings)
				.withDisplayedName(new I18nString(msgSrc.getLocaleCode(), "Create new account"))
				.withTitle2ndStage(new I18nString(msgSrc.getLocaleCode(), "Provide your details"))
				.withTranslationProfile(tp)
				.build();
		
	if (extraAttrs != null)
		form.getAttributeParams().addAll(extraAttrs)
			
	registrationsManagement.addForm(form);  
}

void addEnquiryForm(EnquiryType type, String name, String logo, String groupPath, String targetGroup, List<AttributeRegistrationParam> extraAttrs)
{
	log.info("Creating enquiry form " + name);
	
	EnquiryFormNotifications not = new EnquiryFormNotifications();
	not.setSubmittedTemplate("enquiryFilled");
	not.setInvitationTemplate("invitationWithCode");
	
	String[] target = [targetGroup]
	
	String[] action = [AutomaticRequestAction.accept.toString()]
	TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				action);
	String[] action2 = ["\"" + groupPath + "\""]
	TranslationAction a2 = new TranslationAction(AddToGroupActionFactory.NAME,
				action2);			
			
	List<TranslationRule> rules = Lists.newArrayList(new TranslationRule("validCode == true", a1), new TranslationRule("true", a2))
	
	TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, rules);
	
	FormLayoutSettings lsettings = new FormLayoutSettings();
	lsettings.setLogoURL(logo);
	lsettings.setColumnWidth(21);
	lsettings.setColumnWidthUnit("em");
	
	EnquiryForm form = new EnquiryFormBuilder()
				.withName(name)
				.withNotificationsConfiguration(not)
				.withTargetGroups(target)
				.withType(type)
				.withAddedAttributeParam()
					.withLabel(StringUtils.capitalize(NAME_ATTR) + ":")
	    			.withAttributeType(NAME_ATTR).withGroup("/")
					.withOptional(false)
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withShowGroups(false)
				.endAttributeParam()
				.withAddedGroupParam()
					.withLabel("Select group:")
					.withMultiselect(true)
					.withGroupPath(groupPath + "/*?/**")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.withDisplayedName(new I18nString(msgSrc.getLocaleCode(), "Update your account"))
				.withFormLayoutSettings(lsettings)
				.withTranslationProfile(tp)
				.build()
				
	if (extraAttrs != null)
			form.getAttributeParams().addAll(extraAttrs)
	
	enquiryManagement.addEnquiry(form);  		 
}			

void createGroupsStructure()
{
	log.info("Creating demo UpMan groups...");
	addGroup("/projects", "Projects");

	addGroup(FBI_GROUP, "FBI");
	
	addGroup(FBI_GROUP + "/AJA8O", "Cyber division");
	addGroup(FBI_GROUP + "/HSK3F", "HR division");
	addGroup(FBI_GROUP + "/KA328", "Security division");
	addGroup(FBI_GROUP + "/NWKUE", "X Files");
	addGroup(FBI_GROUP + "/RJG68", "Training division");

	addGroup(UNIV_GROUP, "University");

	addGroup(UNIV_GROUP + "/XHWFO", "Students");
	addGroup(UNIV_GROUP + "/XHWFO/MWC3X", "First year");
	addGroup(UNIV_GROUP + "/XHWFO/RG2DK", "Second year");
	addGroup(UNIV_GROUP + "/XHWFO/ZG37E", "Third year");
	addGroup(UNIV_GROUP + "/YFTLU", "Staff");
	addGroup(UNIV_GROUP + "/YFTLU/DW5NI", "HR division");
	addGroup(UNIV_GROUP + "/YFTLU/DASNK", "Teachers division");
	addGroup(UNIV_GROUP + "/YFTLU/XSADA", "Network admins");
}

void addGroup(String path, String name)
{
	Group g = new Group(path);
	g.setDisplayedName(new I18nString(msgSrc.getLocaleCode(), name));
	groupsManagement.addGroup(g);
}

void setGroupDelegationConfig(String path, String logo, String registrationForm, String enquiryForm, String stickyEnquiryForm, List<String> attributes)
{  		
	log.info("Setting delegation configuration for group " + path);
	GroupDelegationConfiguration config = new GroupDelegationConfiguration(true, logo, registrationForm, enquiryForm, stickyEnquiryForm, attributes);
	GroupContents content = groupsManagement.getContents(path, GroupContents.METADATA);
	Group g = content.getGroup();
	g.setDelegationConfiguration(config);
	groupsManagement.updateGroup(path, g); 
} 


void createExampleUser(int suffix)
{
	IdentityParam toAdd = new IdentityParam(EmailIdentity.ID, "demo-user-" + suffix + "@example.com");
	Identity base = entityManagement.addEntity(toAdd, EntityState.valid, false);
	EntityParam entityP = new EntityParam(base.getEntityId());
	
	groupsManagement.addMemberFromParent("/projects", entityP);
	groupsManagement.addMemberFromParent(FBI_GROUP, entityP);
	groupsManagement.addMemberFromParent(UNIV_GROUP , entityP);

	Attribute cnA = StringAttribute.of(NAME_ATTR, "/", "Demo user " + suffix);
	attributesManagement.createAttribute(entityP, cnA);

	Attribute teamNameA = StringAttribute.of(TEAMNAME_ATTR, FBI_GROUP, "FBI team " + suffix%3);
	attributesManagement.createAttribute(entityP, teamNameA);
	
	Attribute firstnameA = StringAttribute.of(FIRSTNAME_ATTR, UNIV_GROUP, "UN name " + suffix);
	attributesManagement.createAttribute(entityP, firstnameA);
	
	Attribute surnameA = StringAttribute.of(SURNAME_ATTR, UNIV_GROUP, "UN surname " + suffix);
	attributesManagement.createAttribute(entityP, surnameA);
	
	log.info("Demo user 'demo-user-" + suffix + "' was created");
}

void addDemoUserAsManager()
{
	EntityParam entityP = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "demo-user"));

	groupsManagement.addMemberFromParent("/projects", entityP);
	groupsManagement.addMemberFromParent("/projects/FBI", entityP);
	groupsManagement.addMemberFromParent("/projects/univ", entityP);

	Attribute managerA = EnumAttribute.of(ROLE_ATTR, FBI_GROUP, "manager");
	attributesManagement.createAttribute(entityP, managerA);

	managerA = EnumAttribute.of(ROLE_ATTR, UNIV_GROUP, "manager");
	attributesManagement.createAttribute(entityP, managerA);

	log.info("Demo user was added as projects manager");
}