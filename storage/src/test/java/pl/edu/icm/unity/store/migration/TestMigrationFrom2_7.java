/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam.InvitationType;
import pl.edu.icm.unity.base.registration.invitation.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@ExtendWith(SpringExtension.class)
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
	
	@BeforeEach
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
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
				ie.store(new FileOutputStream("target/afterImport.json"), new DBDumpContentElements());
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
				ie.store(new FileOutputStream("target/afterImport.json"), new DBDumpContentElements());
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
		assertThat(all).hasSize(1);
		
		RegistrationRequestState req1 = all.get(0);
		assertThat(req1.getRegistrationContext().triggeringMode).isEqualTo(TriggeringMode.afterRemoteLoginWhenUnknownUser);

	}
	
	private void checkEnquiries()
	{
		List<EnquiryResponseState> all = enquiryResponseDB.getAll();
		assertThat(all).hasSize(1);
		
		EnquiryResponseState req1 = all.get(0);
		assertThat(req1.getRegistrationContext().triggeringMode).isEqualTo(TriggeringMode.afterRemoteLoginWhenUnknownUser);
	}
	
	private void checkInvitations()
	{
		List<InvitationWithCode> all = invitationDB.getAll();
		assertThat(all).hasSize(1);
		
		InvitationWithCode i = all.get(0);
		RegistrationInvitationParam i1 = (RegistrationInvitationParam) i.getInvitation();
		assertThat(i1.getType()).isEqualTo(InvitationType.REGISTRATION);
		assertThat(i1.getFormPrefill().getGroupSelections()).hasSize(2);
		assertThat(i1.getFormPrefill().getGroupSelections().get(0).getEntry().getSelectedGroups()).isEqualTo(Lists.newArrayList("/A"));
		assertThat(i1.getFormPrefill().getGroupSelections().get(1).getEntry().getSelectedGroups()).isEqualTo(Lists.newArrayList("/A/B"));

	}
	
}
