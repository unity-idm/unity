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
import pl.edu.icm.unity.types.basic.Identity
import pl.edu.icm.unity.types.basic.IdentityParam
import pl.edu.icm.unity.types.basic.IdentityTaV

@Field final String NAME_ATTR = "name"
@Field final String ROLE_ATTR = "sys:ProjectManagementRole"

if (!isColdStart)
{
	log.info("Database already initialized with demo UpMan groups, skipping...");
	return;
}

createGroupsStructure();

for (int i=0; i<30; i++)
	createExampleUser(i);

addDemoUserAsManager();



private void createGroupsStructure()
{
	log.info("Creating demo UpMan groups...");
	addGroup("/projects", "Projects");

	addGroup("/projects/FBI", "FBI");
	addGroup("/projects/FBI/AJA8O", "Cyber division");
	addGroup("/projects/FBI/HSK3F", "HR division");
	addGroup("/projects/FBI/KA328", "Security division");
	addGroup("/projects/FBI/NWKUE", "X Files");
	addGroup("/projects/FBI/RJG68", "Training division");

	addGroup("/projects/univ", "University");
	addGroup("/projects/univ/XHWFO", "Students");
	addGroup("/projects/univ/XHWFO/MWC3X", "First year");
	addGroup("/projects/univ/XHWFO/RG2DK", "Second year");
	addGroup("/projects/univ/XHWFO/ZG37E", "Third year");
	addGroup("/projects/univ/YFTLU", "Staff");
	addGroup("/projects/univ/YFTLU/DW5NI", "HR division");
	addGroup("/projects/univ/YFTLU/DASNK", "Teachers division");
	addGroup("/projects/univ/YFTLU/XSADA", "Network admins");
}

private void addGroup(String path, String name)
{
	Group g = new Group(path);
	g.setDisplayedName(new I18nString(msgSrc.getLocaleCode(), name));
	groupsManagement.addGroup(g);
}

void createExampleUser(int suffix)
{
	IdentityParam toAdd = new IdentityParam(EmailIdentity.ID, "demo-user-" + suffix + "@example.com");
	Identity base = entityManagement.addEntity(toAdd, EntityState.valid, false);
	EntityParam entityP = new EntityParam(base.getEntityId());
	
	groupsManagement.addMemberFromParent("/projects", entityP);
	groupsManagement.addMemberFromParent("/projects/FBI", entityP);
	groupsManagement.addMemberFromParent("/projects/univ", entityP);

	Attribute cnA = StringAttribute.of(NAME_ATTR, "/", "Demo user " + suffix);
	attributesManagement.createAttribute(entityP, cnA);

	log.info("Demo user 'demo-user-" + suffix + "' was created");
}

void addDemoUserAsManager()
{
	EntityParam entityP = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "demo-user"));

	groupsManagement.addMemberFromParent("/projects", entityP);
	groupsManagement.addMemberFromParent("/projects/FBI", entityP);
	groupsManagement.addMemberFromParent("/projects/univ", entityP);

	Attribute managerA = EnumAttribute.of(ROLE_ATTR, "/projects/FBI", "manager");
	attributesManagement.createAttribute(entityP, managerA);

	managerA = EnumAttribute.of(ROLE_ATTR, "/projects/univ", "manager");
	attributesManagement.createAttribute(entityP, managerA);

	log.info("Demo user was added as projects manager");
}