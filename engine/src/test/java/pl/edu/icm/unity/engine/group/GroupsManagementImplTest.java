package pl.edu.icm.unity.engine.group;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;


import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.capacity_limit.CapacityLimit;
import pl.edu.icm.unity.base.capacity_limit.CapacityLimitName;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.exceptions.CapacityLimitReachedException;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.generic.CapacityLimitDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

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
			assertThat(membership).isEqualTo(Sets.newHashSet("/", "/test"));
		});

		// when
		groupsMan.removeGroup("/test", true);

		// then
		tx.runInTransaction(() -> {
			Set<String> membership = membershipDAO.getEntityMembership(entityId).stream()
					.map(GroupMembership::getGroup).collect(Collectors.toSet());
			assertThat(membership).isEqualTo(Sets.newHashSet("/"));
		});
	}

	@Test
	public void shouldCreateFullPath() throws EngineException
	{
		groupsMan.addGroup(new Group("/parent1/parent2/parent3"), true);

		Set<String> groups = groupsMan.getChildGroups("/");

		assertThat(groups).hasSize(4);
		assertThat(groups.contains("/")).isTrue();
		assertThat(groups.contains("/parent1")).isTrue();
		assertThat(groups.contains("/parent1/parent2")).isTrue();
		assertThat(groups.contains("/parent1/parent2/parent3")).isTrue();
	}

	@Test
	public void shouldCreateMultipleGroups() throws EngineException
	{
		Group g1 = new Group("/g1");
		Group g2 = new Group("/g1/g2");
		Group g3 = new Group("/g3");
		
		Set<Group> toAdd = Sets.newHashSet(g1, g2, g3);
		groupsMan.addGroups(Sets.newHashSet(toAdd));
		
		Set<String> groups = groupsMan.getChildGroups("/");
		assertThat(groups).contains(toAdd.stream().map(Group::toString).toArray(String[]::new));
		assertThat(groupsMan.getContents(g1.toString(), GroupContents.EVERYTHING).getGroup()).isEqualTo(g1);
	}
	
	@Test
	public void shouldCreateMultipleGroupsWithConfigs() throws EngineException
	{
		Group g1 = new Group("/g1");
		g1.setDisplayedName(new I18nString("G1"));
		g1.setDelegationConfiguration(new GroupDelegationConfiguration(true));
		Group g2 = new Group("/g1/g2");
		g2.setDisplayedName(new I18nString("G2"));
		Group g3 = new Group("/g3");
		g3.setDisplayedName(new I18nString("G3"));
		g3.setDelegationConfiguration(new GroupDelegationConfiguration(false));
		
		Set<Group> toAdd = Sets.newHashSet(g1, g2, g3);
		groupsMan.addGroups(Sets.newHashSet(toAdd));
		
		Set<String> groups = groupsMan.getChildGroups("/");
		assertThat(groups).contains(toAdd.stream().map(Group::toString).toArray(String[]::new));
		assertThat(groupsMan.getContents(g1.toString(), GroupContents.EVERYTHING).getGroup()).isEqualTo(g1);
		assertThat(groupsMan.getContents(g2.toString(), GroupContents.EVERYTHING).getGroup()).isEqualTo(g2);
		assertThat(groupsMan.getContents(g3.toString(), GroupContents.EVERYTHING).getGroup()).isEqualTo(g3);
	}


	@Test
	public void shouldFailAndRollbackWhenOneGroupIsIncorrect() throws EngineException
	{
		Group g1 = new Group("/g1");
		Group g2 = new Group("/g1/g2");
		Group g3Broken = new Group("/g6/g7");
		
		Set<Group> toAdd = Sets.newHashSet(g1, g2, g3Broken);
		
		Throwable exception = catchThrowable(() -> groupsMan.addGroups(toAdd));
		assertExceptionType(exception, IllegalArgumentException.class);

		Set<String> groups = groupsMan.getChildGroups("/");
		assertThat(groups).doesNotContain(toAdd.stream().map(Group::toString).toArray(String[]::new));
	}
	
	@Test
	public void shouldFailWhenCapacityLimitExceeded() throws EngineException
	{
		tx.runInTransactionThrowing(() ->
		{
			limitDB.create(new CapacityLimit(CapacityLimitName.GroupsCount, groupsMan.getChildGroups("/")
					.size() + 2));
		});

		Group g1 = new Group("/g1");
		Group g2 = new Group("/g1/g2");
		Group g3 = new Group("/g1/g2/g3");
		assertThrows(CapacityLimitReachedException.class, () -> groupsMan.addGroups(Sets.newHashSet(g1, g2, g3)));
	}
	
	@Test
	public void shouldCreateNextPathElements() throws EngineException
	{
		groupsMan.addGroup(new Group("/parent1"), false);
		groupsMan.addGroup(new Group("/parent1/parent2/parent3"), true);

		Set<String> groups = groupsMan.getChildGroups("/");

		assertThat(groups).hasSize(4);
		assertThat(groups.contains("/")).isTrue();
		assertThat(groups.contains("/parent1")).isTrue();
		assertThat(groups.contains("/parent1/parent2")).isTrue();
		assertThat(groups.contains("/parent1/parent2/parent3")).isTrue();
	}
}
