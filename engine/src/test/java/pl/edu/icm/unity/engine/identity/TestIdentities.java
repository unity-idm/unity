/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TransientIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

public class TestIdentities extends DBIntegrationTestBase
{
	
	@Autowired
	private EntitiesScheduledUpdater entitiesUpdater;
	private EntityParam entityParam;

	
	@Test
	public void shouldUpdateIdentityConfirmation() throws Exception
	{
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(EmailIdentity.ID, "test@example.com");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);

		//verify
		Entity retrieved = idsMan.getEntity(new EntityParam(idParam));
		assertThat(getByType(retrieved, EmailIdentity.ID).getConfirmationInfo().isConfirmed(), is(false));
		
		Identity updated = id.clone();
		updated.setConfirmationInfo(new ConfirmationInfo(true));
		idsMan.updateIdentity(idParam, updated);
		
		retrieved = idsMan.getEntity(new EntityParam(idParam));
		assertThat(getByType(retrieved, EmailIdentity.ID).getConfirmationInfo().isConfirmed(), is(true));
	}

	@Test
	public void shouldDisallowUpdatingIdentityComparableValue() throws Exception
	{
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(EmailIdentity.ID, "test@example.com");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		
		Identity updated = id.clone();
		updated.setValue("other@example.com");
		
		Throwable error = catchThrowable(() -> idsMan.updateIdentity(idParam, updated));
		
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void scheduledDisableWork() throws Exception
	{
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		
		EntityParam ep1 = new EntityParam(id.getEntityId());
		Date scheduledTime = new Date(System.currentTimeMillis()+100);
		idsMan.scheduleEntityChange(ep1, scheduledTime, EntityScheduledOperation.DISABLE);
		
		Entity retrieved = idsMan.getEntity(new EntityParam(idParam));
		assertEquals(EntityScheduledOperation.DISABLE,
				retrieved.getEntityInformation().getScheduledOperation());
		assertEquals(scheduledTime,
				retrieved.getEntityInformation().getScheduledOperationTime());
		
		Thread.sleep(150);
		entitiesUpdater.updateEntities();
		
		Entity updated = idsMan.getEntity(ep1);
		assertEquals(EntityState.disabled, updated.getState());
	}

	@Test
	public void scheduledRemovalWork() throws Exception
	{
		setupMockAuthn();
		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2");
		Identity id2 = idsMan.addEntity(idParam2, "crMock", EntityState.valid, false);
		
		EntityParam ep2 = new EntityParam(id2.getEntityId());
		Date scheduledTime = new Date(System.currentTimeMillis()+100);
		idsMan.scheduleEntityChange(ep2, scheduledTime, EntityScheduledOperation.REMOVE);
		
		Entity retrieved = idsMan.getEntity(ep2);
		assertEquals(EntityScheduledOperation.REMOVE,
				retrieved.getEntityInformation().getScheduledOperation());
		assertEquals(scheduledTime,
				retrieved.getEntityInformation().getScheduledOperationTime());

		Thread.sleep(150);
		entitiesUpdater.updateEntities();
		
		try
		{
			idsMan.getEntity(ep2);
			fail("Entity not removed");
		} catch (IllegalArgumentException e)
		{
			//ok
		}
	}

	@Test
	public void scheduledRemovalWorksForUser() throws Exception
	{
		setupPasswordAuthn();
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
		setupPasswordAuthn();
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
		setupPasswordAuthn();
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
		setupPasswordAuthn();
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
		setupPasswordAuthn();
		Identity id = createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		IdentityParam dnId = new IdentityParam(X500Identity.ID,  "CN=someCN");
		idsMan.addIdentity(dnId, new EntityParam(id), false);
		IdentityParam emailId = new IdentityParam(EmailIdentity.ID,  "email@example.org");
		idsMan.addIdentity(emailId, new EntityParam(id), false);
		
		
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
		setupPasswordAuthn();
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

		idsMan.addIdentity(emailId, entityParam, false);

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
		setupPasswordAuthn();
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

		idsMan.addIdentity(emailId, entityParam, false);
		idsMan.addIdentity(emailId2, entityParam, false);
		idsMan.addIdentity(emailId3, entityParam, false);
		idsMan.addIdentity(emailId4, entityParam, false);
		idsMan.addIdentity(emailId5, entityParam, false);

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
		setupPasswordAuthn();
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
		setupPasswordAuthn();
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
		
		idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email1@custom.net"), ep1, false);
		idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email2@custom.net"), ep1, false);
		try
		{
			idsMan.addIdentity(new IdentityParam(UsernameIdentity.ID, "dummy"), ep1, false);
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
		setupPasswordAuthn();
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
		idsMan.addIdentity(identityParam, ep1, false);
		
		setupUserContext(DEF_USER, null);
		
		idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email1@custom.net"), ep1, false);
		idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email2@custom.net"), ep1, false);
		try
		{
			idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, "email3@custom.com"), ep1, false);
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
		setupPasswordAuthn();
		
		StringBuilder id = new StringBuilder();
		for (int i=0; i<300; i++)
			id.append(i%10);
		
		Identity added = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, id.toString()), 
				DBIntegrationTestBase.CRED_REQ_PASS, EntityState.valid, false);
		
		Entity full = idsMan.getEntity(new EntityParam(added), null, true, null);
		assertEquals(2, full.getIdentities().size());
	}

	@Test
	public void typesForAllSyntaxesAreReturned() throws Exception
	{
		Collection<IdentityType> idTypes = idTypeMan.getIdentityTypes();
		assertEquals(7, idTypes.size());
		assertNotNull(getIdentityTypeByName(idTypes, PersistentIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, TargetedPersistentIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, X500Identity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, UsernameIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, TransientIdentity.ID));
		assertNotNull(getIdentityTypeByName(idTypes, IdentifierIdentity.ID));
	}

	@Test
	public void addingUnknownAttributeExtractionIsNotAllowed() throws Exception
	{
		Collection<IdentityType> idTypes = idTypeMan.getIdentityTypes();
		IdentityType toUpdate = getIdentityTypeByName(idTypes, X500Identity.ID);
		Map<String, String> extracted = new HashMap<String, String>();
		extracted.put("cn", "cn");
		toUpdate.setExtractedAttributes(extracted);
		try
		{
			idTypeMan.updateIdentityType(toUpdate);
			fail("managed to set attributes extraction with undefined attribute t");
		} catch(IllegalAttributeTypeException e) {}
	}
	
	@Test
	public void addingOneUnknownAttributeExtractionIsNotAllowed() throws Exception
	{
		Collection<IdentityType> idTypes = idTypeMan.getIdentityTypes();
		aTypeMan.addAttributeType(new AttributeType("cn", StringAttributeSyntax.ID));

		IdentityType toUpdate = getIdentityTypeByName(idTypes, X500Identity.ID);
		Map<String, String> extracted = new HashMap<String, String>();
		extracted.put("cn", "cn");
		extracted.put("unknown", "cn");
		toUpdate.setExtractedAttributes(extracted);
		try
		{
			idTypeMan.updateIdentityType(toUpdate);
			fail("managed to set attributes extraction with unsupported attribute t");
		} catch(IllegalAttributeTypeException e) {}
	}
	
	@Test
	public void updatedTypeIsReturned() throws Exception
	{
		Collection<IdentityType> idTypes = idTypeMan.getIdentityTypes();
		aTypeMan.addAttributeType(new AttributeType("cn", StringAttributeSyntax.ID));
		AttributeType at = new AttributeType("country", StringAttributeSyntax.ID);
		at.setMaxElements(0);
		aTypeMan.addAttributeType(at);

		IdentityType toUpdate = getIdentityTypeByName(idTypes, X500Identity.ID);
		toUpdate.setDescription("fiu fiu");
		Map<String, String> extracted = new HashMap<String, String>();
		extracted.put("cn", "cn");
		extracted.put("c", "country");
		toUpdate.setExtractedAttributes(extracted);
		idTypeMan.updateIdentityType(toUpdate);
		
		idTypes = idTypeMan.getIdentityTypes();
		IdentityType updated = getIdentityTypeByName(idTypes, X500Identity.ID);
		assertThat(updated, is(toUpdate));
	}
	
	@Test
	public void configuredAttribtuesAreExtractedFromAddedIdentity() throws Exception
	{
		Collection<IdentityType> idTypes = idTypeMan.getIdentityTypes();

		aTypeMan.addAttributeType(new AttributeType("cn", StringAttributeSyntax.ID));
		AttributeType at = new AttributeType("country", StringAttributeSyntax.ID);
		at.setMaxElements(0);
		aTypeMan.addAttributeType(at);

		IdentityType toUpdate = getIdentityTypeByName(idTypes, X500Identity.ID);
		Map<String, String> extracted = new HashMap<String, String>();
		extracted.put("cn", "cn");
		extracted.put("c", "country");
		toUpdate.setExtractedAttributes(extracted);
		idTypeMan.updateIdentityType(toUpdate);
		
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi, dc=ddd, ou=org unit,C=pl");
		Identity added = idsMan.addEntity(idParam, "crMock", EntityState.valid, true);
		
		Collection<AttributeExt> attributes = attrsMan.getAttributes(new EntityParam(added), "/", null);
		assertEquals(1 + DEF_ATTRS, attributes.size());
		Attribute cnAttr = getAttributeByName(attributes, "cn");
		assertEquals(cnAttr.getValues().get(0), "golbi");
	}
	
	@Test
	public void removedAttributeIsRemovedFromExtractedAttributes() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("cn", StringAttributeSyntax.ID));
		aTypeMan.addAttributeType(new AttributeType("country", StringAttributeSyntax.ID));

		Collection<IdentityType> idTypes = idTypeMan.getIdentityTypes();
		IdentityType toUpdate = getIdentityTypeByName(idTypes, X500Identity.ID);
		Map<String, String> extracted = new HashMap<String, String>();
		extracted.put("cn", "cn");
		extracted.put("c", "country");
		toUpdate.setExtractedAttributes(extracted);
		idTypeMan.updateIdentityType(toUpdate);
		
		aTypeMan.removeAttributeType("cn", true);
		idTypes = idTypeMan.getIdentityTypes();
		IdentityType updated = getIdentityTypeByName(idTypes, X500Identity.ID);
		assertEquals(1, updated.getExtractedAttributes().size());
		assertEquals("c", updated.getExtractedAttributes().keySet().iterator().next());
		assertEquals("country", updated.getExtractedAttributes().values().iterator().next());
		
		aTypeMan.updateAttributeType(new AttributeType("country", IntegerAttributeSyntax.ID));
		idTypes = idTypeMan.getIdentityTypes();
		updated = getIdentityTypeByName(idTypes, X500Identity.ID);
		assertEquals(0, updated.getExtractedAttributes().size());
	}

	@Test
	public void onlyPersistentAddedWhenAllowedWithoutTarget() throws Exception
	{
		setupMockAuthn();
		setupAdmin();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		EntityParam entityParam = new EntityParam(id.getEntityId());
		idsMan.resetIdentity(entityParam, PersistentIdentity.ID, null, null);
		
		Entity e2 = idsMan.getEntity(entityParam, null, true, "/");
		
		assertEquals(2, e2.getIdentities().size());
		assertNotNull(getByType(e2, X500Identity.ID));
		assertTrue(getByType(e2, PersistentIdentity.ID).getValue().length() > 0);
	}

	@Test
	public void shouldCreatePersistentIdentityWithEntity() throws Exception
	{
		setupMockAuthn();
		setupAdmin();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		EntityParam entityParam = new EntityParam(id.getEntityId());
		
		Entity e2 = idsMan.getEntity(entityParam, null, false, "/");
		
		assertEquals(2, e2.getIdentities().size());
		assertNotNull(getByType(e2, X500Identity.ID));
		assertTrue(getByType(e2, PersistentIdentity.ID).getValue().length() > 0);
	}

	
	@Test
	public void allAddedWhenAllowedWithTarget() throws Exception
	{
		setupMockAuthn();
		setupAdmin();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		EntityParam entityParam = new EntityParam(id.getEntityId());
		
		Entity e3 = idsMan.getEntity(entityParam, "target1", true, "/");

		assertEquals(4, e3.getIdentities().size());
		assertNotNull(getByType(e3, X500Identity.ID));
		assertTrue(getByType(e3, PersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TargetedPersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TransientIdentity.ID).getValue().length() > 0);
	}
	
	@Test
	public void differentTargetedIdentitiesAreCreatedForDifferentTargets() throws Exception
	{
		setupMockAuthn();
		setupAdmin();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		EntityParam entityParam = new EntityParam(id.getEntityId());
		
		Entity e3 = idsMan.getEntity(entityParam, "target1", true, "/");
		assertEquals(4, e3.getIdentities().size());
		assertNotNull(getByType(e3, X500Identity.ID));
		assertTrue(getByType(e3, PersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TargetedPersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e3, TransientIdentity.ID).getValue().length() > 0);
		
		Entity e4 = idsMan.getEntity(entityParam, "target2", true, "/");
		assertEquals(4, e4.getIdentities().size());
		assertNotNull(getByType(e4, X500Identity.ID));
		assertTrue(getByType(e4, PersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e4, TargetedPersistentIdentity.ID).getValue().length() > 0);
		assertTrue(getByType(e4, TransientIdentity.ID).getValue().length() > 0);

		assertNotEquals(getByType(e3, TransientIdentity.ID).getValue(), 
				getByType(e4, TransientIdentity.ID).getValue());
		assertNotEquals(getByType(e3, TargetedPersistentIdentity.ID).getValue(), 
				getByType(e4, TargetedPersistentIdentity.ID).getValue());
		assertEquals(getByType(e3, PersistentIdentity.ID).getValue(), 
				getByType(e4, PersistentIdentity.ID).getValue());
		Entity e2 = idsMan.getEntity(entityParam, null, true, "/");
		assertEquals(getByType(e2, PersistentIdentity.ID).getValue(), 
				getByType(e4, PersistentIdentity.ID).getValue());
	}	
	
	@Test
	public void getEntityTriggersCreationOfPersistentIdentity() throws Exception
	{
		setupMockAuthn();
		setupAdmin();

		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2");
		Identity id2 = idsMan.addEntity(idParam2, "crMock", EntityState.valid, false);
		EntityParam entityParam2 = new EntityParam(id2.getEntityId());

		Entity e5 = idsMan.getEntity(entityParam2);
		assertEquals(2, e5.getIdentities().size());
		assertNotNull(getByType(e5, X500Identity.ID));
		assertTrue(getByType(e5, PersistentIdentity.ID).getValue().length() > 0);
	}
	
	private Identity getByType(Entity e, String type)
	{
		for (Identity id: e.getIdentities())
			if (id.getTypeId().equals(type))
				return id;
		fail("No such type");
		return null;
	}

	private Identity getByName(Entity e, String type, String name)
	{
		for (Identity id: e.getIdentities())
			if (id.getTypeId().equals(type) && id.getValue().equals(name))
				return id;
		fail("No such type");
		return null;
	}

	@Test
	public void identitiesWithSameTypeAndDifferentTypeAreDistinguished() throws Exception
	{
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(UsernameIdentity.ID, "id");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		IdentityParam idParam2 = new IdentityParam(IdentifierIdentity.ID, "id");
		idsMan.addIdentity(idParam2, new EntityParam(id.getEntityId()), false);
		
		Entity ret = idsMan.getEntity(new EntityParam(id.getEntityId()), null, false, "/");
		
		assertThat(ret.getIdentities().size(), is(3));
		assertThat(getIdentityByType(ret.getIdentities(), UsernameIdentity.ID).getValue(), is("id"));
		assertThat(getIdentityByType(ret.getIdentities(), IdentifierIdentity.ID).getValue(), is("id"));
	}
	
	@Test
	public void testCreate() throws Exception
	{
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		assertNotNull(id.getEntityId());
		assertEquals("CN=golbi", id.getValue());
		assertEquals(true, id.isLocal());
		assertNotNull(id.getCreationTs());
		assertNotNull(id.getUpdateTs());
		assertNull(id.getTranslationProfile());
		assertNull(id.getRemoteIdp());
		
		IdentityParam idParam2 = new IdentityParam(X500Identity.ID, "CN=golbi2", "remoteIdp", "prof1");
		Identity id2 = idsMan.addIdentity(idParam2, new EntityParam(id.getEntityId()), false);
		assertEquals("CN=golbi2", id2.getValue());
		assertEquals(id.getEntityId(), id2.getEntityId());
		assertEquals(false, id2.isLocal());
		assertNotNull(id2.getCreationTs());
		assertNotNull(id2.getUpdateTs());
		assertEquals("prof1", id2.getTranslationProfile());
		assertEquals("remoteIdp", id2.getRemoteIdp());
		
		Entity entity = idsMan.getEntity(new EntityParam(id2));
		assertEquals(3, entity.getIdentities().size());
		assertEquals(id, getByName(entity, X500Identity.ID, "CN=golbi"));
		getByType(entity, PersistentIdentity.ID);
		Identity retDn = getByName(entity, X500Identity.ID, "CN=golbi2");
		assertEquals(id2, retDn);
		assertEquals(id.getEntityId(), entity.getId().longValue());
		
		idsMan.setEntityStatus(new EntityParam(entity.getId()), EntityState.disabled);
		entity = idsMan.getEntity(new EntityParam(id2));
		assertEquals(EntityState.disabled, entity.getState());
		
		GroupContents contents = groupsMan.getContents("/", GroupContents.MEMBERS);
		assertEquals(2, contents.getMembers().size());
		assertTrue(contents.getMembers().stream().anyMatch(t -> id.getEntityId() == t.getEntityId()));

		groupsMan.addGroup(new Group("/test"));
		groupsMan.addMemberFromParent("/test", new EntityParam(id.getEntityId()));
		contents = groupsMan.getContents("/test", GroupContents.MEMBERS);
		assertEquals(1, contents.getMembers().size());
		assertEquals(id.getEntityId(), contents.getMembers().get(0).getEntityId());
		
		
		groupsMan.addGroup(new Group("/test2"));
		groupsMan.addGroup(new Group("/test2/test"));
		try
		{
			groupsMan.addMemberFromParent("/test2/test", new EntityParam(id.getEntityId()));
			fail("Added to a group while is not in parent");
		} catch(IllegalGroupValueException e) {}
		

		try
		{
			groupsMan.removeMember("/", new EntityParam(id.getEntityId()));
			fail("removed member from /");
		} catch(IllegalArgumentException e) {}

		try
		{
			groupsMan.removeMember("/test2", new EntityParam(id.getEntityId()));
			fail("removed non member");
		} catch(IllegalArgumentException e) {}

		groupsMan.addMemberFromParent("/test2", new EntityParam(id.getEntityId()));
		groupsMan.addMemberFromParent("/test2/test", new EntityParam(id.getEntityId()));
		groupsMan.removeMember("/test2", new EntityParam(id.getEntityId()));
		GroupContents t2Contents = groupsMan.getContents("/test2/test", GroupContents.MEMBERS);
		assertEquals(0, t2Contents.getMembers().size());
		
		groupsMan.removeMember("/test", new EntityParam(id.getEntityId()));
		contents = groupsMan.getContents("/test", GroupContents.MEMBERS);
		assertEquals(0, contents.getMembers().size());
		
		idsMan.removeIdentity(id);
		entity = idsMan.getEntity(new EntityParam(id2));
		assertEquals(2, entity.getIdentities().size());
		Identity retdnp = getByName(entity, X500Identity.ID, "CN=golbi2");
		assertEquals(id2, retdnp);
		assertEquals(id2.getEntityId(), entity.getId().longValue());
		
		idsMan.removeEntity(new EntityParam(id2));
		
		try
		{
			idsMan.getEntity(new EntityParam(id2));
			fail("Removed entity is still available");
		} catch (IllegalArgumentException e) {}
		
		contents = groupsMan.getContents("/", GroupContents.MEMBERS);
		assertEquals(1, contents.getMembers().size());
	}
	
	@Test
	public void removingLastIdentityIsProhibited() throws Exception
	{
		setupMockAuthn();
		IdentityParam idParam = new IdentityParam(X500Identity.ID, "CN=golbi");
		Identity id = idsMan.addEntity(idParam, "crMock", EntityState.valid, false);
		idsMan.resetIdentity(new EntityParam(id.getEntityId()), PersistentIdentity.ID, null, null);
		
		catchException(idsMan).removeIdentity(id);

		assertThat(caughtException(), isA(SchemaConsistencyException.class));
	}
}
