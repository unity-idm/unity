import java.util.Arrays;
import groovy.transform.Field
import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.base.i18n.I18nString
import pl.edu.icm.unity.stdext.identity.EmailIdentity
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.base.attribute.Attribute
import pl.edu.icm.unity.base.entity.EntityParam
import pl.edu.icm.unity.base.entity.EntityState
import pl.edu.icm.unity.base.group.Group
import pl.edu.icm.unity.base.group.GroupContents
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration
import pl.edu.icm.unity.base.identity.Identity
import pl.edu.icm.unity.base.identity.IdentityParam
import pl.edu.icm.unity.base.identity.IdentityTaV
import pl.edu.icm.unity.base.registration.RegistrationForm
import pl.edu.icm.unity.base.registration.EnquiryForm


@Field final String NAME_ATTR = "name"
@Field final String FIRSTNAME_ATTR = "firstname"
@Field final String SURNAME_ATTR = "surname"
@Field final String TEAMNAME_ATTR = "teamname"

@Field final String ROLE_ATTR = "sys:ProjectManagementRole"

@Field final String FBI_GROUP = "/projects/FBI"
@Field final String UNIV_GROUP = "/projects/univ"

@Field final String UNIV_LOGO_SMALL = "https://previews.123rf.com/images/captainvector/captainvector1703/captainvector170309945/74377645-university-logo-element.jpg"
@Field final String FBI_LOGO_SMALL = "https://upload.wikimedia.org/wikipedia/commons/9/9f/FBISeal.png"

if (!isColdStart)
{
	log.info("Database already initialized with demo UpMan groups, skipping...");
	return;
}

createGroupsStructure();

for (int i=0; i<30; i++)
	createExampleUser(i);

addDemoUserAsManager();

RegistrationForm fbiRegistrationForm = groupDelegationConfigGenerator.generateProjectRegistrationForm(FBI_GROUP, FBI_LOGO_SMALL, Arrays.asList(TEAMNAME_ATTR), List.of())
RegistrationForm univRegistrationForm = groupDelegationConfigGenerator.generateProjectRegistrationForm(UNIV_GROUP, UNIV_LOGO_SMALL, Arrays.asList(FIRSTNAME_ATTR, SURNAME_ATTR), List.of())
registrationsManagement.addForm(fbiRegistrationForm);  
registrationsManagement.addForm(univRegistrationForm);  

EnquiryForm fbiJoinEnquiryForm = groupDelegationConfigGenerator.generateProjectJoinEnquiryForm(FBI_GROUP, FBI_LOGO_SMALL, List.of())
EnquiryForm univJoinEnquiryForm = groupDelegationConfigGenerator.generateProjectJoinEnquiryForm(UNIV_GROUP, UNIV_LOGO_SMALL, List.of())
enquiryManagement.addEnquiry(fbiJoinEnquiryForm);  		 
enquiryManagement.addEnquiry(univJoinEnquiryForm);  	

EnquiryForm fbiUpdateEnquiryForm = groupDelegationConfigGenerator.generateProjectUpdateEnquiryForm(FBI_GROUP, FBI_LOGO_SMALL)
EnquiryForm univUpdateEnquiryForm = groupDelegationConfigGenerator.generateProjectUpdateEnquiryForm(UNIV_GROUP, UNIV_LOGO_SMALL)
enquiryManagement.addEnquiry(fbiUpdateEnquiryForm);  		 
enquiryManagement.addEnquiry(univUpdateEnquiryForm);  		 


setGroupDelegationConfig(UNIV_GROUP,
				UNIV_LOGO_SMALL,
				univRegistrationForm.getName(),
				univJoinEnquiryForm.getName(),
				univUpdateEnquiryForm.getName(),
				Arrays.asList(FIRSTNAME_ATTR, SURNAME_ATTR));

setGroupDelegationConfig(FBI_GROUP,
				FBI_LOGO_SMALL,
				fbiRegistrationForm.getName(),
				fbiJoinEnquiryForm.getName(),
				fbiUpdateEnquiryForm.getName(),
				Arrays.asList(TEAMNAME_ATTR));

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
	GroupDelegationConfiguration config = new GroupDelegationConfiguration(true, true, logo, registrationForm, enquiryForm, stickyEnquiryForm, attributes, List.of());
	GroupContents content = groupsManagement.getContents(path, GroupContents.METADATA);
	Group g = content.getGroup();
	g.setDelegationConfiguration(config);
	groupsManagement.updateGroup(path, g); 
} 


void createExampleUser(int suffix)
{
	IdentityParam toAdd = new IdentityParam(EmailIdentity.ID, "demo-user-" + suffix + "@example.com");
	Identity base = entityManagement.addEntity(toAdd, EntityState.valid);
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

	Attribute managerA = EnumAttribute.of(ROLE_ATTR, FBI_GROUP, "projectsAdmin");
	attributesManagement.createAttribute(entityP, managerA);

	managerA = EnumAttribute.of(ROLE_ATTR, UNIV_GROUP, "projectsAdmin");
	attributesManagement.createAttribute(entityP, managerA);

	log.info("Demo user was added as projects manager");
}
