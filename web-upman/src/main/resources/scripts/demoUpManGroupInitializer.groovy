import pl.edu.icm.unity.types.basic.Group
import pl.edu.icm.unity.types.I18nString

if (!isColdStart)
{
	log.info("Database already initialized with demo UpMan groups, skipping...");
	return;
}

log.info("Creating demo UpMan groups...");


private void addGroup(String path, String name)
{
    Group g = new Group(path);
	g.setDisplayedName(new I18nString(msgSrc.getLocaleCode(), name));
	groupsManagement.addGroup(g);
}


addGroup("/MYF9W", "Projects");

addGroup("/MYF9W/CXM9C", "FBI");
addGroup("/MYF9W/CXM9C/AJA8O", "Cyber division");
addGroup("/MYF9W/CXM9C/HSK3F", "HR division");
addGroup("/MYF9W/CXM9C/KA328", "Security division");
addGroup("/MYF9W/CXM9C/NWKUE", "X Files");
addGroup("/MYF9W/CXM9C/RJG68", "Training division");

addGroup("/MYF9W/LQRA0", "University");
addGroup("/MYF9W/LQRA0/XHWFO", "Students");
addGroup("/MYF9W/LQRA0/XHWFO/MWC3X", "First year");
addGroup("/MYF9W/LQRA0/XHWFO/RG2DK", "Second year");
addGroup("/MYF9W/LQRA0/XHWFO/ZG37E", "Third year");
addGroup("/MYF9W/LQRA0/YFTLU", "Staff");
addGroup("/MYF9W/LQRA0/YFTLU/DW5NI", "HR division");
addGroup("/MYF9W/LQRA0/YFTLU/DASNK", "Teachers division");
addGroup("/MYF9W/LQRA0/YFTLU/XSADA", "Network admins");