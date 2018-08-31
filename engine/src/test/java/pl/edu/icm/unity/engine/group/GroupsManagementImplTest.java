package pl.edu.icm.unity.engine.group;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.IdentityTaV;

public class GroupsManagementImplTest extends DBIntegrationTestBase
{
	@Autowired
	private MembershipDAO membershipDAO;
	@Autowired
	protected TransactionalRunner tx;
	
	@Test
	public void membershipsShouldBeUpdatedAfterDeletingGroup() throws Exception 
	{
		// given
		setupPasswordAuthn();
		createUsernameUserWithRole("System Manager");
		long entityId = setupUserContext(DEF_USER, null);
		EntityParam entityParam = new EntityParam(new IdentityTaV(UsernameIdentity.ID, DEF_USER));
		groupsMan.addGroup(new Group("/test"));
		groupsMan.addMemberFromParent("/test", entityParam);
		
		// expect
		tx.runInTransaction(() -> 
		{
			Set<String> membership = membershipDAO.getEntityMembership(entityId).stream()
					.map(GroupMembership::getGroup)
					.collect(Collectors.toSet());
			assertThat(membership, equalTo(Sets.newHashSet("/", "/test")));
		});
		
		// when
		groupsMan.removeGroup("/test", true);
		
		// then
		tx.runInTransaction(() -> 
		{
			Set<String> membership = membershipDAO.getEntityMembership(entityId).stream()
					.map(GroupMembership::getGroup)
					.collect(Collectors.toSet());
			assertThat(membership, equalTo(Sets.newHashSet("/")));
		});
	}

}
