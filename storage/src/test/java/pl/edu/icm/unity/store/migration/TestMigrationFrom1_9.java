/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.migration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.ac.AttributeClassHandler;
import pl.edu.icm.unity.store.objstore.authn.AuthenticatorConfigurationHandler;
import pl.edu.icm.unity.store.objstore.bulk.ProcessingRuleHandler;
import pl.edu.icm.unity.store.objstore.cred.CredentialHandler;
import pl.edu.icm.unity.store.objstore.credreq.CredentialRequirementHandler;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.store.objstore.notify.NotificationChannelHandler;
import pl.edu.icm.unity.store.objstore.realm.RealmHandler;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;
import pl.edu.icm.unity.store.objstore.reg.eresp.EnquiryResponseHandler;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;
import pl.edu.icm.unity.store.objstore.reg.req.RegistrationRequestHandler;
import pl.edu.icm.unity.store.objstore.tprofile.InputTranslationProfileHandler;
import pl.edu.icm.unity.store.objstore.tprofile.OutputTranslationProfileHandler;
import pl.edu.icm.unity.store.types.StoredAttribute;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class TestMigrationFrom1_9
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;

	@Autowired
	protected TransactionalRunner tx;
	
	@Autowired
	protected ImportExport ie;
	
	@Autowired
	private ObjectStoreDAO genericDao;
	
	@Autowired
	private AttributeTypeDAO atDAO;
	@Autowired
	private IdentityTypeDAO itDAO;
	@Autowired
	private AttributeDAO attrDAO;
	@Autowired
	private IdentityDAO identityDAO;
	@Autowired
	private EntityDAO entDAO;
	@Autowired
	private GroupDAO groupDAO;
	@Autowired
	private MembershipDAO memberDAO;	
	
	
	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}

	
	
	@Test
	public void testImportFrom1_9_x()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from1.9.x/"
						+ "testbed-from1.9.2-complete.json")));
				ie.store(new FileOutputStream("target/afterImport.json"));
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}
			
			assertThat(atDAO.getAll().size(), is(25));
			assertThat(itDAO.getAll().size(), is(7));
			assertThat(attrDAO.getAll().size(), is(58));
			assertThat(identityDAO.getAll().size(), is(45));
			assertThat(entDAO.getAll().size(), is(14));
			assertThat(groupDAO.getAll().size(), is(14));
			assertThat(memberDAO.getAll().size(), is(26));
			
			
			assertThat(genericDao.getObjectsOfType(
					AttributeClassHandler.ATTRIBUTE_CLASS_OBJECT_TYPE).size(), 
					is(3)); 
			assertThat(genericDao.getObjectsOfType(
					AuthenticatorConfigurationHandler.AUTHENTICATOR_OBJECT_TYPE ).size(), 
					is(8)); 
			assertThat(genericDao.getObjectsOfType(
					CredentialHandler.CREDENTIAL_OBJECT_TYPE).size(), 
					is(3)); 
			assertThat(genericDao.getObjectsOfType(
					CredentialRequirementHandler.CREDENTIAL_REQ_OBJECT_TYPE).size(), 
					is(3)); 
			assertThat(genericDao.getObjectsOfType(
					MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE).size(), 
					is(8)); 
			assertThat(genericDao.getObjectsOfType(
					NotificationChannelHandler.NOTIFICATION_CHANNEL_ID).size(), 
					is(1)); 
			assertThat(genericDao.getObjectsOfType(
					RealmHandler.REALM_OBJECT_TYPE).size(), 
					is(3)); 
			assertThat(genericDao.getObjectsOfType(
					InputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE).size(),
					is(8)); 
			assertThat(genericDao.getObjectsOfType(
					OutputTranslationProfileHandler.TRANSLATION_PROFILE_OBJECT_TYPE).size(), 
					is(1)); 
			assertThat(genericDao.getObjectsOfType(
					ProcessingRuleHandler.PROCESSING_RULE_OBJECT_TYPE).size(), 
					is(1)); 
			assertThat(genericDao.getObjectsOfType(
					EndpointHandler.ENDPOINT_OBJECT_TYPE).size(), 
					is(10)); 
			assertThat(genericDao.getObjectsOfType(
					RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE).size(), 
					is(5)); 
			assertThat(genericDao.getObjectsOfType(
					EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE).size(), 
					is(1)); 
			assertThat(genericDao.getObjectsOfType(
					RegistrationRequestHandler.REGISTRATION_REQUEST_OBJECT_TYPE).size(), 
					is(12)); 
			assertThat(genericDao.getObjectsOfType(
					EnquiryResponseHandler.ENQUIRY_RESPONSE_OBJECT_TYPE).size(), 
					is(1)); 
			assertThat(genericDao.getObjectsOfType(
					InvitationHandler.INVITATION_OBJECT_TYPE).size(), 
					is(1));
		});
	}
	
	
	@Test
	public void testPasswordImportFrom1_9_x()
	{
		tx.runInTransaction(() -> {
			try
			{
				ie.load(new BufferedInputStream(new FileInputStream(
						"src/test/resources/updateData/from1.9.x/"
						+ "testbed-from1.9.5-password.json")));
				ie.store(new FileOutputStream("target/afterImport.json"));
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Import failed " + e);
			}
			
			
			List<StoredAttribute> attributes = attrDAO.getAll().stream().
					filter(sa -> sa.getAttribute().getName().equals("sys:Credential:Secured password")).
					collect(Collectors.toList());
			for (StoredAttribute sa: attributes)
			{
				String value = sa.getAttribute().getValues().get(0);
				assertThat(value, containsString("SHA256"));
			}
		});
	}
}
