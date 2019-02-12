/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationParam.InvitationType;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class TestMigrationFrom2_7
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;

	@Autowired
	protected TransactionalRunner tx;
	
	@Autowired
	protected ImportExport ie;
	
	@Autowired
	private RegistrationRequestDB regRequestDB;
	
	@Autowired
	private EnquiryResponseDB enquiryResponseDB;
	
	@Autowired
	private InvitationDB invitationDB;
	
	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}
	
	@Test
	public void testImportFrom2_7_3()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from2.7.x/"
						+ "testbed-from2.7.3-withTriggeringMode.afterRemoteLogin.json")));
				ie.store(new FileOutputStream("target/afterImport.json"));
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}

			checkRequests();
			checkEnquiries();
		});
	}
	
	@Test
	public void testImportFrom2_7_5()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from2.7.x/"
						+ "testbed-from-2.7.5-withInvitation.json")));
				ie.store(new FileOutputStream("target/afterImport.json"));
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}

			checkInvitations();
		});
	}
	
	
	private void checkRequests()
	{
		List<RegistrationRequestState> all = regRequestDB.getAll();
		assertThat(all.size(), is(1));
		
		RegistrationRequestState req1 = all.get(0);
		assertThat(req1.getRegistrationContext().triggeringMode, is(TriggeringMode.afterRemoteLoginWhenUnknownUser));

	}
	
	private void checkEnquiries()
	{
		List<EnquiryResponseState> all = enquiryResponseDB.getAll();
		assertThat(all.size(), is(1));
		
		EnquiryResponseState req1 = all.get(0);
		assertThat(req1.getRegistrationContext().triggeringMode, is(TriggeringMode.afterRemoteLoginWhenUnknownUser));
	}
	
	private void checkInvitations()
	{
		List<InvitationWithCode> all = invitationDB.getAll();
		assertThat(all.size(), is(1));
		
		InvitationWithCode i = all.get(0);
		InvitationParam i1 = i.getInvitation();
		assertThat(i1.getType(), is(InvitationType.REGISTRATION));
		assertThat(i1.getGroupSelections().size(), is(2));
		assertThat(i1.getGroupSelections().get(0).getEntry().getSelectedGroups(), is(Lists.newArrayList("/A")));
		assertThat(i1.getGroupSelections().get(1).getEntry().getSelectedGroups(), is(Lists.newArrayList("/A/B")));

	}
	
}
