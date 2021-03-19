package pl.edu.icm.unity.engine.group;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.capacityLimit.CapacityLimit;
import pl.edu.icm.unity.base.capacityLimit.CapacityLimitName;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.exceptions.CapacityLimitReachedException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.generic.CapacityLimitDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.IdentityTaV;

public class GroupsManagementImplTest extends DBIntegrationTestBase
{
	@Autowired
	private MembershipDAO membershipDAO;
	@Autowired
	protected TransactionalRunner tx;
	@Autowired
	private CapacityLimitDB limitDB;

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
		tx.runInTransaction(() -> {
			Set<String> membership = membershipDAO.getEntityMembership(entityId).stream()
					.map(GroupMembership::getGroup).collect(Collectors.toSet());
			assertThat(membership, equalTo(Sets.newHashSet("/", "/test")));
		});

		// when
		groupsMan.removeGroup("/test", true);

		// then
		tx.runInTransaction(() -> {
			Set<String> membership = membershipDAO.getEntityMembership(entityId).stream()
					.map(GroupMembership::getGroup).collect(Collectors.toSet());
			assertThat(membership, equalTo(Sets.newHashSet("/")));
		});
	}

	@Test
	public void shouldCreateFullPath() throws EngineException
	{
		groupsMan.addGroup(new Group("/parent1/parent2/parent3"), true);

		Set<String> groups = groupsMan.getChildGroups("/");

		assertEquals(4, groups.size());
		assertTrue(groups.contains("/"));
		assertTrue(groups.contains("/parent1"));
		assertTrue(groups.contains("/parent1/parent2"));
		assertTrue(groups.contains("/parent1/parent2/parent3"));
	}

	@Test
	public void shouldCreateMultipleGroups() throws EngineException
	{
		Group g1 = new Group("/g1");
		g1.setDisplayedName(new I18nString("G1"));
		Group g2 = new Group("/g1/g2");
		Group g21 = new Group("/g1/g2/g21");
		Group g3 = new Group("/g1/g2/g3");

		Group g4 = new Group("/g4");
		g4.setDelegationConfiguration(new GroupDelegationConfiguration(true));
		
		Group g5 = new Group("/g4/g5");
		Group g6 = new Group("/g6");

		Set<Group> toAdd = Sets.newHashSet(g6, g5, g1, g2, g3, g4, g21);
		groupsMan.addGroups(Sets.newHashSet(toAdd));
		
		Set<String> groups = groupsMan.getChildGroups("/");
		assertThat(groups, hasItems(toAdd.stream().map(Group::toString).toArray(String[]::new)));
		assertThat(groupsMan.getContents(g1.toString(), GroupContents.EVERYTHING).getGroup(), is(g1));
		assertThat(groupsMan.getContents(g4.toString(), GroupContents.EVERYTHING).getGroup(), is(g4));
	}

	@Test
	public void shouldFailAndRollbackWhenOneGroupIsIncorrect() throws EngineException
	{
		Group g1 = new Group("/g1");
		Group g2 = new Group("/g1/g2");
		Group g21 = new Group("/g1/g2/g21");
		Group g3 = new Group("/g1/g2/g3");
		Group g4 = new Group("/g4");
		Group g5 = new Group("/g4/g5");
		Group g6 = new Group("/g6/g7");
		Set<Group> toAdd = Sets.newHashSet(g6, g5, g1, g2, g3, g4, g21);
		try
		{
			groupsMan.addGroups(Sets.newHashSet(g6, g5, g1, g2, g3, g4, g21));
			fail();
		} catch (Exception e)
		{
			//ok
		}

		Set<String> groups = groupsMan.getChildGroups("/");
		assertThat(groups, not(hasItems(toAdd.stream().map(Group::toString).toArray(String[]::new))));
	}
	
	@Test(expected = CapacityLimitReachedException.class)
	public void shouldFailWhenCapacityLimitExceeded() throws EngineException
	{
		tx.runInTransactionThrowing(() -> {
			limitDB.create(new CapacityLimit(CapacityLimitName.GroupsCount, groupsMan.getChildGroups("/").size() + 2));
		});
	
		Group g1 = new Group("/g1");
		Group g2 = new Group("/g1/g2");
		Group g3 = new Group("/g1/g2/g3");
		
		groupsMan.addGroups(Sets.newHashSet(g1,g2,g3));
	}
	
	@Test
	public void shouldCreateNextPathElements() throws EngineException
	{
		groupsMan.addGroup(new Group("/parent1"), false);
		groupsMan.addGroup(new Group("/parent1/parent2/parent3"), true);

		Set<String> groups = groupsMan.getChildGroups("/");

		assertEquals(4, groups.size());
		assertTrue(groups.contains("/"));
		assertTrue(groups.contains("/parent1"));
		assertTrue(groups.contains("/parent1/parent2"));
		assertTrue(groups.contains("/parent1/parent2/parent3"));
	}
}
