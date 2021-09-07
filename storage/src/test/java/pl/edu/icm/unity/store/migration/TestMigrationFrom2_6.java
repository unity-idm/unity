/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.DBDumpContentElements;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.invite.RegistrationInvitationParam;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class TestMigrationFrom2_6
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
	private RegistrationFormDB regFormDB;
	
	@Autowired
	private EnquiryFormDB enquiryFormDB;
	
	@Autowired
	private EnquiryResponseDB enquiryResponseDB;

	@Autowired
	private InvitationDB invitationDB;
	
	@Autowired
	private AttributeDAO attributeDB;
	
	@Before
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}
	
	@Test
	public void testImportFrom2_6_0()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from2.6.x/"
								+ "testbed-from2.6.2-withregReqForGroup.json")));
				ie.store(new FileOutputStream("target/afterImport.json"), new DBDumpContentElements());
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}

			checkRequests();
			checkEnquiries();
			checkInvitations();
		});
	}
	
	@Test
	public void testImportFrom2_6_2()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from2.6.x/"
						+ "local-from2.6.2-enquiryAndRegWithCustomLayouts.json")));
				ie.store(new FileOutputStream("target/afterImport2.json"), new DBDumpContentElements());
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}

			checkEnquiryFormLayout();
			checkRegistratoinFormLayout();
		});
	}
	
	private void checkEnquiryFormLayout()
	{
		List<EnquiryForm> enquiries = enquiryFormDB.getAll();
		assertThat(enquiries.size(), is(1));
		
		EnquiryForm enquiry = enquiries.get(0);
		assertThat(enquiry.getLayout(), notNullValue());
	}

	private void checkRegistratoinFormLayout()
	{
		List<RegistrationForm> forms = regFormDB.getAll();
		assertThat(forms.size(), is(2));
		
		RegistrationForm fbform = forms.get(0);
		assertThat(fbform.getName(), is("fb-form"));
		assertThat(fbform.getFormLayouts().getPrimaryLayout(), nullValue());
		assertThat(fbform.getFormLayouts().getSecondaryLayout(), nullValue());
		assertThat(fbform.getLayoutSettings(), notNullValue());
		
		RegistrationForm formWithCustomLayout = forms.get(1);
		assertThat(formWithCustomLayout.getName(), is("registration with layout"));
		assertThat(formWithCustomLayout.getFormLayouts().getPrimaryLayout(), notNullValue());
		assertThat(formWithCustomLayout.getFormLayouts().getSecondaryLayout(), nullValue());
		assertThat(fbform.getLayoutSettings(), notNullValue());
	}

	private void checkRequests()
	{
		List<RegistrationRequestState> all = regRequestDB.getAll();
		assertThat(all.size(), is(2));
		
		RegistrationRequestState req1 = all.stream().filter(req -> req.getRequestId().equals("a6acb334-7072-49d2-8983-b995350bd74f")).findFirst().get();
		assertThat(req1.getRequest().getGroupSelections().size(), is(2));
		assertThat(req1.getRequest().getGroupSelections().get(0).getSelectedGroups().size(), is(1));
		assertThat(req1.getRequest().getGroupSelections().get(0).getSelectedGroups().get(0), is("/A"));
		assertThat(req1.getRequest().getGroupSelections().get(1).getSelectedGroups().size(), is(1));
		assertThat(req1.getRequest().getGroupSelections().get(1).getSelectedGroups().get(0), is("/A/B/C"));

		RegistrationRequestState req2 = all.stream().filter(req -> req.getRequestId().equals("49cd3080-7b16-431b-8bd3-54f1620b53c1")).findFirst().get();
		assertThat(req2.getRequest().getGroupSelections().size(), is(2));
		assertThat(req2.getRequest().getGroupSelections().get(0).getSelectedGroups().size(), is(0));
		assertThat(req2.getRequest().getGroupSelections().get(1).getSelectedGroups().size(), is(1));
		assertThat(req2.getRequest().getGroupSelections().get(1).getSelectedGroups().get(0), is("/A/B/C"));
	}
	
	private void checkEnquiries()
	{
		List<EnquiryResponseState> all = enquiryResponseDB.getAll();
		assertThat(all.size(), is(1));
		
		EnquiryResponseState req1 = all.get(0);
		assertThat(req1.getRequest().getGroupSelections().size(), is(1));
		assertThat(req1.getRequest().getGroupSelections().get(0).getSelectedGroups().size(), is(1));
		assertThat(req1.getRequest().getGroupSelections().get(0).getSelectedGroups().get(0), is("/D"));
	}
	
	private void checkInvitations()
	{
		List<InvitationWithCode> all = invitationDB.getAll();
		assertThat(all.size(), is(2));
		
		Map<String, InvitationWithCode> byCode = all.stream()
				.collect(Collectors.toMap(i -> i.getRegistrationCode(), i->i));
		
		InvitationWithCode i = byCode.get("1e46b209-92ac-4f2d-a4b8-475bbe956424");
		RegistrationInvitationParam i1 = (RegistrationInvitationParam) i.getInvitation();
		assertThat(i1.getFormPrefill().getGroupSelections().size(), is(2));
		assertThat(i1.getFormPrefill().getGroupSelections().get(0).getEntry().getSelectedGroups(), is(Lists.newArrayList("/A")));
		assertThat(i1.getFormPrefill().getGroupSelections().get(1).getEntry().getSelectedGroups(), is(Lists.newArrayList("/A/B/C")));

		i = byCode.get("7e8d72a8-22e1-40c7-872c-dcb8a85e40cc");
		RegistrationInvitationParam i2 = (RegistrationInvitationParam) i.getInvitation();
		assertThat(i2.getFormPrefill().getGroupSelections().size(), is(2));
		assertThat(i2.getFormPrefill().getGroupSelections().get(0).getEntry().getSelectedGroups(), is(Lists.newArrayList("/A")));
		assertThat(i2.getFormPrefill().getGroupSelections().get(1).getEntry().getSelectedGroups().isEmpty(), is(true));	
	}
	
	@Test
	public void shouldRemoveOrphanedAttributes()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from2.6.x/"
								+ "testbed-from2.6.2-withOrphanedAttr.json")));
				ie.store(new FileOutputStream("target/afterImport2.json"), new DBDumpContentElements());
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}

			checkAttributes();
		});
	}

	private void checkAttributes()
	{
		List<StoredAttribute> attributes = attributeDB.getAttributes(null, null, "/A");
		assertThat(attributes.toString(), attributes.size(), is(0));
	}
}
