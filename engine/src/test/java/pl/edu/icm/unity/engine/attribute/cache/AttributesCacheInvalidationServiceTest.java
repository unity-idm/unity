/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.attributes.AttributesCacheRefreshTrigger;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TxManager;

@ExtendWith(MockitoExtension.class)
public class AttributesCacheInvalidationServiceTest
{
	@Mock
	private AttributesCachePendingDAO pendingDAO;
	@Mock
	private MembershipDAO membershipDAO;
	@Mock
	private TxManager txMan;
	@Mock
	private AttributesCacheRefreshTrigger refreshTrigger;

	private AttributesCacheInvalidationService create()
	{
		return new AttributesCacheInvalidationService(pendingDAO, membershipDAO, txMan, refreshTrigger);
	}

	@Test
	public void entityInvalidationMarksAllItsGroups()
	{
		when(membershipDAO.getEntityMembershipSimple(1L)).thenReturn(Set.of("/", "/A"));

		create().invalidateEntity(1L);

		verify(pendingDAO).markPending(1L, "/");
		verify(pendingDAO).markPending(1L, "/A");
		verifyNoMoreInteractions(pendingDAO);
	}

	@Test
	public void entityInGroupInvalidationMarksOnlyThatGroup()
	{
		create().invalidateEntityInGroup(1L, "/A");

		verify(pendingDAO).markPending(1L, "/A");
		verifyNoMoreInteractions(pendingDAO);
		verifyNoInteractions(membershipDAO);
	}

	@Test
	public void groupInvalidationMarksMembersInAllTheirGroups()
	{
		create().invalidateGroup("/A");

		verify(pendingDAO).markPendingForGroupMembers("/A");
		verifyNoMoreInteractions(pendingDAO);
	}

	@Test
	public void globalInvalidationMarksAllMemberships()
	{
		create().invalidateEverything();

		verify(pendingDAO).markPendingForAll();
		verifyNoMoreInteractions(pendingDAO);
	}

	@Test
	public void wakeupHappensOnlyAfterCommit()
	{
		create().invalidateEntityInGroup(1L, "/A");

		ArgumentCaptor<Runnable> postCommit = ArgumentCaptor.forClass(Runnable.class);
		verify(txMan).addPostCommitAction(postCommit.capture());
		verify(refreshTrigger, never()).wakeUp();

		postCommit.getValue().run();

		verify(refreshTrigger).wakeUp();
	}
}
