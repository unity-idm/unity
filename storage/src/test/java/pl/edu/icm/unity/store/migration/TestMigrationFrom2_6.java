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
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.invite.InvitationParam;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;

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
		dbCleaner.reset();
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
				ie.store(new FileOutputStream("target/afterImport.json"));
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
				ie.store(new FileOutputStream("target/afterImport2.json"));
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
		
		RegistrationRequestState req1 = all.get(0);
		assertThat(req1.getRequest().getGroupSelections().size(), is(2));
		assertThat(req1.getRequest().getGroupSelections().get(0).getSelectedGroups().size(), is(1));
		assertThat(req1.getRequest().getGroupSelections().get(0).getSelectedGroups().get(0), is("/A"));
		assertThat(req1.getRequest().getGroupSelections().get(1).getSelectedGroups().size(), is(1));
		assertThat(req1.getRequest().getGroupSelections().get(1).getSelectedGroups().get(0), is("/A/B/C"));

		RegistrationRequestState req2 = all.get(1);
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
		
		InvitationWithCode i = all.get(0);
		InvitationParam i1 = i.getInvitation();
		assertThat(i1.getGroupSelections().size(), is(2));
		assertThat(i1.getGroupSelections().get(0).getEntry().getSelectedGroups(), is(Lists.newArrayList("/A")));
		assertThat(i1.getGroupSelections().get(1).getEntry().getSelectedGroups(), is(Lists.newArrayList("/A/B/C")));

		i = all.get(1);
		InvitationParam i2 = i.getInvitation();
		assertThat(i2.getGroupSelections().size(), is(2));
		assertThat(i2.getGroupSelections().get(0).getEntry().getSelectedGroups(), is(Lists.newArrayList("/A")));
		assertThat(i2.getGroupSelections().get(1).getEntry().getSelectedGroups().isEmpty(), is(true));	
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
				ie.store(new FileOutputStream("target/afterImport2.json"));
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
