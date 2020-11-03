package scripts

import pl.edu.icm.unity.types.basic.AttributeStatement
import pl.edu.icm.unity.types.basic.Group

initAttributesAndGroups()

void initAttributesAndGroups()
{
    String newGroup = "bihuniak"
    if(groupsManagement.isPresent(newGroup)) {
        AttributeStatement attributeStatement = new AttributeStatement("true", "/",
                AttributeStatement.ConflictResolution.skip, "email", "eattr['firstname']")
        groupsManagement.addGroup(new Group(newGroup))
    }
}
