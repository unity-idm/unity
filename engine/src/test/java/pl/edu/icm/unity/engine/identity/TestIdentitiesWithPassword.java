/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;

/*
 * Note - those tests most often (always?) do not require password cred, and could equally well use mock cred,
 * and be merged with other file with tests in this package.
 */
public class TestIdentitiesWithPassword extends DBIntegrationTestBase
{
	@Autowired
	private EntitiesScheduledUpdater entitiesUpdater;
	private EntityParam entityParam;

	@Before
	public void prepare() throws Exception
	{
		setupPasswordAuthn();
	}
	
	@Test
	public void scheduledRemovalWorksForUser() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam ep1 = new EntityParam(id.getEntityId());
		
		setupUserContext(DEF_USER, null);
		
		idsMan.scheduleRemovalByUser(ep1, new Date(System.currentTimeMillis()+200));
		idsMan.getEntity(ep1);
		Thread.sleep(200);
		entitiesUpdater.updateEntities();
		
		try
		{
			idsMan.getEntity(ep1);
			fail("Entity not removed");
		} catch (IllegalArgumentException e)
		{
			//ok
		}
	}

	@Test
	public void scheduledRemovalWorksForUserImmediately() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam ep1 = new EntityParam(id.getEntityId());
		
		setupUserContext(DEF_USER, null);
		
		idsMan.scheduleRemovalByUser(ep1, new Date(System.currentTimeMillis()));
		try
		{
			idsMan.getEntity(ep1);
			fail("Entity not removed");
		} catch (IllegalArgumentException e)
		{
			//ok
		}
	}

	@Test
	public void scheduledRemovalGraceTimeWorksForUser() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam ep1 = new EntityParam(id.getEntityId());

		setupUserContext(DEF_USER, null);
		idsMan.scheduleRemovalByUser(ep1, new Date(System.currentTimeMillis()+500));
		setupUserContext(DEF_USER, null);
		entitiesUpdater.updateEntities();
		
		Entity entity = idsMan.getEntity(ep1);
		assertEquals(EntityState.valid, entity.getState());
	}

	@Test
	public void setIdentitiesFailsOnIdentitiesOfWrongType() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		try
		{
			idsMan.setIdentities(new EntityParam(id.getEntityId()), 
					new HashSet<String>(), Sets.newHashSet(id));
			fail("Managed to set identities of not enumerated type");
		} catch (IllegalArgumentException e)
		{
			//ok
		}
	}

	@Test
	public void setIdentitiesUpdatesIdentities() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		IdentityParam dnId = new IdentityParam(X500Identity.ID,  "CN=someCN");
		idsMan.addIdentity(dnId, new EntityParam(id));
		IdentityParam emailId = new IdentityParam(EmailIdentity.ID,  "email@example.org");
		idsMan.addIdentity(emailId, new EntityParam(id));
		
		
		IdentityParam newUser = new IdentityParam(UsernameIdentity.ID, "user-new");
		IdentityParam newUser2 = new IdentityParam(UsernameIdentity.ID, "user-new2");
		IdentityParam newId = new IdentityParam(IdentifierIdentity.ID, "added");
		idsMan.setIdentities(new EntityParam(id.getEntityId()),
				Sets.newHashSet(UsernameIdentity.ID, X500Identity.ID, IdentifierIdentity.ID), 
				Sets.newHashSet(newUser, newUser2, newId));
		
		Entity updated = idsMan.getEntity(new EntityParam(id.getEntityId()));
		Set<String> identities = new HashSet<>();
		for (Identity i: updated.getIdentities())
			identities.add(i.getValue());
		//added
		assertTrue(identities.contains(newUser.getValue()));
		assertTrue(identities.contains(newUser2.getValue()));
		assertTrue(identities.contains(newId.getValue()));
		//left
		assertTrue(identities.contains(emailId.getValue()));
		//removed
		assertFalse(identities.contains(dnId.getValue()));
		assertFalse(identities.contains(id.getValue()));
	}

	@Test
	public void setIdentitiesRespectTypeLimits() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		
		IdentityType idType = new IdentityType(EmailIdentity.ID, EmailIdentity.ID);
		idType.setSelfModificable(true);
		idType.setMinInstances(2);
		idType.setMaxInstances(2);
		idType.setMinVerifiedInstances(1);
		idTypeMan.updateIdentityType(idType);
		entityParam = new EntityParam(id);
		IdentityParam emailId = new IdentityParam(EmailIdentity.ID,  "email@example.org");
		emailId.setConfirmationInfo(new ConfirmationInfo(true));
		IdentityParam emailId2 = new IdentityParam(EmailIdentity.ID,  "email2@example.org");
		emailId2.setConfirmationInfo(new ConfirmationInfo(true));
		IdentityParam emailId3 = new IdentityParam(EmailIdentity.ID,  "email3@example.org");
		emailId3.setConfirmationInfo(new ConfirmationInfo(true));

		idsMan.addIdentity(emailId, entityParam);

		setupUserContext(DEF_USER, null);
		
		idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId, emailId2));

		idsMan.getEntity(entityParam).getIdentities();
		for (Identity i: idsMan.getEntity(entityParam).getIdentities())
			if (i.getValue().equals("email2@example.org"))
				assertFalse(i.getConfirmationInfo().isConfirmed());
		
		try
		{
			idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId3, emailId2));
			fail("Managed to break confirmed limit");
		} catch (SchemaConsistencyException e)
		{
			//expected
		}
		
		try
		{
			idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId));
			fail("Managed to break min limit");
		} catch (SchemaConsistencyException e)
		{
			//expected
		}

		try
		{
			idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId, emailId2, emailId3));
			fail("Managed to break max limit");
		} catch (SchemaConsistencyException e)
		{
			//expected
		}
	}

	/**
	 * When identity limits are not met (due to admin's settings) user should be able to add identities
	 * if there are less then required by limit and to remove if there is more then the upper limit,
	 * i.e. to improve the situation.
	 * @throws Exception
	 */
	@Test
	public void userCanImproveLimitsSituation() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		
		IdentityType idType = new IdentityType(EmailIdentity.ID, EmailIdentity.ID);
		idType.setSelfModificable(true);
		idType.setMinInstances(2);
		idType.setMaxInstances(3);
		idType.setMinVerifiedInstances(1);
		idTypeMan.updateIdentityType(idType);
		entityParam = new EntityParam(id);
		IdentityParam emailId = new IdentityParam(EmailIdentity.ID,  "email@example.org");
		emailId.setConfirmationInfo(new ConfirmationInfo(true));
		IdentityParam emailId2 = new IdentityParam(EmailIdentity.ID,  "email2@example.org");
		emailId2.setConfirmationInfo(new ConfirmationInfo(true));
		IdentityParam emailId3 = new IdentityParam(EmailIdentity.ID,  "email3@example.org");
		emailId3.setConfirmationInfo(new ConfirmationInfo(false));
		IdentityParam emailId4 = new IdentityParam(EmailIdentity.ID,  "email4@example.org");
		emailId4.setConfirmationInfo(new ConfirmationInfo(false));
		IdentityParam emailId5 = new IdentityParam(EmailIdentity.ID,  "email5@example.org");
		emailId5.setConfirmationInfo(new ConfirmationInfo(false));

		idsMan.addIdentity(emailId, entityParam);
		idsMan.addIdentity(emailId2, entityParam);
		idsMan.addIdentity(emailId3, entityParam);
		idsMan.addIdentity(emailId4, entityParam);
		idsMan.addIdentity(emailId5, entityParam);

		setupUserContext(DEF_USER, null);

		//still above limit, but removing works
		idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId, emailId2, emailId3, emailId4));

		setupAdmin();
		
		idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				new HashSet<IdentityParam>());

		setupUserContext(DEF_USER, null);

		//still under limit, but adding and changing works
		idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId));
		idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId, emailId2));
		idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId3, emailId4));
	}

	@Test
	public void typeLimitsAreIgnoredForAdmin() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		
		IdentityType idType = new IdentityType(EmailIdentity.ID, EmailIdentity.ID);
		idType.setSelfModificable(true);
		idType.setMinInstances(2);
		idType.setMaxInstances(2);
		idType.setMinVerifiedInstances(1);
		idTypeMan.updateIdentityType(idType);
		entityParam = new EntityParam(id);
		IdentityParam emailId = new IdentityParam(EmailIdentity.ID,  "email@example.org");
		emailId.setConfirmationInfo(new ConfirmationInfo(true));
		IdentityParam emailId2 = new IdentityParam(EmailIdentity.ID,  "email2@example.org");
		emailId2.setConfirmationInfo(new ConfirmationInfo(true));
		IdentityParam emailId3 = new IdentityParam(EmailIdentity.ID,  "email3@example.org");
		emailId3.setConfirmationInfo(new ConfirmationInfo(true));

		idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId));
		idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				new HashSet<IdentityParam>());
		idsMan.setIdentities(entityParam, Sets.newHashSet(EmailIdentity.ID), 
				Sets.newHashSet(emailId, emailId2, emailId3));
	}
	
	@Test
	public void selfModifiableIdentityCanBeControlledByUser() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam ep1 = new EntityParam(id.getEntityId());
		IdentityType idType = new IdentityType(EmailIdentity.ID, EmailIdentity.ID);
		idType.setSelfModificable(true);
		idTypeMan.updateIdentityType(idType);
		Collection<IdentityType> identityTypes = idTypeMan.getIdentityTypes();
		for (IdentityType idTypeI: identityTypes)
			if (idTypeI.getIdentityTypeProvider().equals(EmailIdentity.ID))
				assertTrue(idTypeI.isSelfModificable());
			else
				assertFalse(idTypeI.isSelfModificable());
		
		setupUserContext(DEF_USER, null);
		
		idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email1@custom.net"), ep1);
		idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email2@custom.net"), ep1);
		try
		{
			idsMan.addIdentity(new IdentityParam(UsernameIdentity.ID, "dummy"), ep1);
			fail("Managed to add non self modifiable identity");
		} catch (AuthorizationException e)
		{
			//expected
		}
		
		idsMan.removeIdentity(new IdentityTaV(EmailIdentity.ID, "email1@custom.net"));

		Entity entity = idsMan.getEntity(ep1);
		List<Identity> ids = entity.getIdentities();
		for (Identity idd: ids)
			if (idd.getTypeId().equals(EmailIdentity.ID))
				assertEquals(idd.getValue(), "email2@custom.net");
	}

	@Test
	public void minMaxIsEnforced() throws Exception
	{
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam ep1 = new EntityParam(id.getEntityId());
		IdentityType idType = new IdentityType(EmailIdentity.ID, EmailIdentity.ID);
		idType.setSelfModificable(true);
		idType.setMinInstances(2);
		idType.setMaxInstances(3);
		idType.setMinVerifiedInstances(1);
		idTypeMan.updateIdentityType(idType);
		Collection<IdentityType> identityTypes = idTypeMan.getIdentityTypes();
		for (IdentityType idTypeI: identityTypes)
			if (idTypeI.getIdentityTypeProvider().equals(EmailIdentity.ID))
				assertTrue(idTypeI.isSelfModificable());
			else
				assertFalse(idTypeI.isSelfModificable());
		IdentityParam identityParam = new IdentityParam(EmailIdentity.ID, "emailV@custom.net");
		identityParam.setConfirmationInfo(new ConfirmationInfo(true));
		idsMan.addIdentity(identityParam, ep1);
		
		setupUserContext(DEF_USER, null);
		
		idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email1@custom.net"), ep1);
		idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email2@custom.net"), ep1);
		try
		{
			idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email3@custom.com"), ep1);
			fail("Managed to add too many emails");
		} catch (SchemaConsistencyException e)
		{
			//expected
		}
		try
		{
			idsMan.removeIdentity(new IdentityTaV(EmailIdentity.ID, "emailV@custom.net"));
			fail("Managed to remove confirmed");
		} catch (SchemaConsistencyException e)
		{
			//expected
		}
		
		idsMan.removeIdentity(new IdentityTaV(EmailIdentity.ID, "email1@custom.net"));
		try
		{
			idsMan.removeIdentity(new IdentityTaV(EmailIdentity.ID, "email2@custom.net"));
			fail("Managed to remove too many");
		} catch (SchemaConsistencyException e)
		{
			//expected
		}
	}

	@Test
	public void longIdentityIsSupported() throws Exception
	{
		StringBuilder id = new StringBuilder();
		for (int i=0; i<300; i++)
			id.append(i%10);
		
		Identity added = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, id.toString()), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid);
		
		Entity full = idsMan.getEntity(new EntityParam(added), null, true, null);
		assertEquals(2, full.getIdentities().size());
	}
}
