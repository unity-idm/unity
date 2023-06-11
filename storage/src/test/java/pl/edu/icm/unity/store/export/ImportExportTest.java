/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.export;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.audit.AuditEntity;
import pl.edu.icm.unity.base.audit.AuditEvent.AuditEventBuilder;
import pl.edu.icm.unity.base.audit.AuditEventAction;
import pl.edu.icm.unity.base.audit.AuditEventType;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.db.DBDumpContentElements;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityType;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.i18n.I18nMessage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.base.msgtemplates.MessageType;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.EnquiryResponseBuilder;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.base.registration.invite.RegistrationInvitationParam;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.TokenDAO;
import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.generic.CertificateDB;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.EnquiryResponseDB;
import pl.edu.icm.unity.store.api.generic.InputTranslationProfileDB;
import pl.edu.icm.unity.store.api.generic.InvitationDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;
import pl.edu.icm.unity.store.api.generic.OutputTranslationProfileDB;
import pl.edu.icm.unity.store.api.generic.ProcessingRuleDB;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationRequestDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.store.types.StoredCertificate;
import pl.edu.icm.unity.store.types.StoredIdentity;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:META-INF/components.xml" })
public class ImportExportTest
{
	@Autowired
	private StorageCleanerImpl dbCleaner;

	@Autowired
	private TransactionalRunner tx;

	@Autowired
	private ImportExport ie;

	@Autowired
	private RealmDB realmDB;

	@Autowired
	private AuthenticatorConfigurationDB authenticatorDB;

	@Autowired
	private CredentialDB credentialDB;

	@Autowired
	private AuthenticationFlowDB authenticationFlowDB;

	@Autowired
	private GroupDAO groupDAO;

	@Autowired
	private EndpointDB endpointDB;

	@Autowired
	private FileDAO fileDB;

	@Autowired
	private InputTranslationProfileDB inProfileDB;

	@Autowired
	private OutputTranslationProfileDB outProfileDB;

	@Autowired
	private CertificateDB certDB;

	@Autowired
	private NotificationChannelDB notificationChannelDB;

	@Autowired
	private CredentialRequirementDB credReqDB;

	@Autowired
	private ProcessingRuleDB procRuleDB;

	@Autowired
	private AttributeTypeDAO attributeTypeDB;

	@Autowired
	private EnquiryFormDB enquiryFormDB;

	@Autowired
	private RegistrationFormDB registrationFormDB;

	@Autowired
	private RegistrationRequestDB registrationRequestDB;

	@Autowired
	private EnquiryResponseDB enquiryResponseDB;

	@Autowired
	private IdentityTypeDAO idTypeDB;

	@Autowired
	private InvitationDB invitationDB;

	@Autowired
	private AttributeClassDB attrClassDB;

	@Autowired
	private MessageTemplateDB msgTemplDB;

	@Autowired
	private IdentityDAO idDB;

	@Autowired
	private EntityDAO entityDB;

	@Autowired
	private AttributeDAO attrDB;

	@Autowired
	private MembershipDAO membershipDB;

	@Autowired
	private TokenDAO tokenDB;

	@Autowired
	private AuditEventDAO auditDB;

	@Before
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}

	@Test
	public void shouldExportSystem()
	{
		tx.runInTransaction(() -> {
			try
			{

				realmDB.create(new AuthenticationRealm("test", "", 10, 60,
						RememberMePolicy.allowFor2ndFactor, 10, 10));
				certDB.create(new StoredCertificate("test", "test"));
				authenticatorDB.create(new AuthenticatorConfiguration("test", "", "", "", 0));
				authenticationFlowDB.create(new AuthenticationFlowDefinition("test2", Policy.NEVER,
						Sets.newHashSet()));
				endpointDB.create(
						new Endpoint("test", "", "/x",
								new EndpointConfiguration(new I18nString("test"), "",
										List.of(), null, null, null),
								0));
				fileDB.create(new FileData("test", "test".getBytes(), new Date()));
				inProfileDB.create(new TranslationProfile("test", "test", ProfileType.INPUT,
						List.of()));
				outProfileDB.create(new TranslationProfile("test2", "test", ProfileType.OUTPUT,
						List.of()));

				DBDumpContentElements el = new DBDumpContentElements(true, false, false, false, false, false);
				
				ie.store(new FileOutputStream("target/afterImport.json"), el);

				dbCleaner.deletePreImport(DBDumpContentTypeMapper.getElementsForClearDB(el));

				assertThat(realmDB.getAll().size(), is(0));
				assertThat(authenticatorDB.getAll().size(), is(0));
				assertThat(authenticationFlowDB.getAll().size(), is(0));
				assertThat(endpointDB.getAll().size(), is(0));
				assertThat(fileDB.getAll().size(), is(0));
				assertThat(inProfileDB.getAll().size(), is(0));
				assertThat(outProfileDB.getAll().size(), is(0));
				assertThat(certDB.getAll().size(), is(0));
				assertThat(notificationChannelDB.getAll().size(), is(0));
				assertThat(credentialDB.getAll().size(), is(0));

				ie.load(new BufferedInputStream(new FileInputStream("target/afterImport.json")));

				assertThat(realmDB.get("test"), is(notNullValue()));
				assertThat(certDB.get("test"), is(notNullValue()));
				assertThat(authenticatorDB.get("test"), is(notNullValue()));
				assertThat(authenticationFlowDB.get("test2"), is(notNullValue()));
				assertThat(endpointDB.get("test"), is(notNullValue()));
				assertThat(fileDB.get("test"), is(notNullValue()));
				assertThat(inProfileDB.get("test"), is(notNullValue()));
				assertThat(outProfileDB.get("test2"), is(notNullValue()));

			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Test export/import system elements failed " + e);
			}
		});
	}

	@Test
	public void shouldExportDirSchema()
	{
		tx.runInTransaction(() -> {
			try
			{
				credReqDB.create(new CredentialRequirements("test", "test", Sets.newHashSet()));
				procRuleDB.create(new ScheduledProcessingRule("true",
						new TranslationAction("test", "test"), "", "test"));
				attributeTypeDB.create(new AttributeType("test", "test"));
				groupDAO.create(new Group("/test"));

				EnquiryForm eForm = new EnquiryFormBuilder().withName("test")
						.withType(EnquiryType.STICKY).withTargetGroups(new String[] { "/" })
						.build();
				enquiryFormDB.create(eForm);

				RegistrationForm rForm = new RegistrationFormBuilder().withName("test")
						.withDefaultCredentialRequirement("test").build();
				registrationFormDB.create(rForm);

				idTypeDB.create(new IdentityType("test"));

				attrClassDB.create(new AttributesClass("test", "test", Sets.newHashSet(),
						Sets.newHashSet(), false, Sets.newHashSet()));
				msgTemplDB.create(new MessageTemplate("test", "test",
						new I18nMessage(new I18nString(), new I18nString()), "test",
						MessageType.PLAIN, "test"));

				DBDumpContentElements el = new DBDumpContentElements(false, true, false, false, false, false);
	
				ie.store(new FileOutputStream("target/afterImport.json"), el);

				dbCleaner.deletePreImport(DBDumpContentTypeMapper.getElementsForClearDB(el));

				assertThat(credReqDB.getAll().size(), is(0));
				assertThat(procRuleDB.getAll().size(), is(0));
				assertThat(attributeTypeDB.getAll().size(), is(0));
				assertThat(groupDAO.getAll().size(), is(1));
				assertThat(enquiryFormDB.getAll().size(), is(0));
				assertThat(registrationFormDB.getAll().size(), is(0));
				assertThat(idTypeDB.getAll().size(), is(0));
				assertThat(attrClassDB.getAll().size(), is(0));
				assertThat(msgTemplDB.getAll().size(), is(0));

				ie.load(new BufferedInputStream(new FileInputStream("target/afterImport.json")));

				assertThat(credReqDB.get("test"), is(notNullValue()));
				assertThat(procRuleDB.get("test"), is(notNullValue()));
				assertThat(attributeTypeDB.get("test"), is(notNullValue()));
				assertThat(groupDAO.get("/test"), is(notNullValue()));
				assertThat(enquiryFormDB.get("test"), is(notNullValue()));
				assertThat(registrationFormDB.get("test"), is(notNullValue()));
				assertThat(idTypeDB.get("test"), is(notNullValue()));
				assertThat(attrClassDB.get("test"), is(notNullValue()));
				assertThat(msgTemplDB.get("test"), is(notNullValue()));

			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Test export/import dir schema failed " + e);
			}
		});
	}

	@Test
	public void shouldExportUsers()
	{
		tx.runInTransaction(() -> {
			try
			{
				idTypeDB.create(new IdentityType("test"));
				entityDB.create(new EntityInformation(1));
				idDB.create(new StoredIdentity(new Identity("test", "test", 1, "test")));
				attributeTypeDB.create(new AttributeType("test", "test"));
				attrDB.create(new StoredAttribute(new AttributeExt(
						new Attribute("test", "test", "/", List.of("v1")), false), 1));
				groupDAO.create(new Group("/test"));
				membershipDB.create(new GroupMembership("/test", 1, new Date()));

				Token tk = new Token("test", "test", 1L);
				tk.setCreated(new Date());
				tk.setExpires(new Date());
				tokenDB.create(tk);

				DBDumpContentElements el = new DBDumpContentElements(false, true, true, false, false, false);

				ie.store(new FileOutputStream("target/afterImport.json"), el);

				dbCleaner.deletePreImport(DBDumpContentTypeMapper.getElementsForClearDB(el));

				assertThat(idDB.getAll().size(), is(0));
				assertThat(entityDB.getAll().size(), is(0));
				assertThat(attrDB.getAll().size(), is(0));
				assertThat(membershipDB.getAll().size(), is(0));
				assertThat(tokenDB.getAll().size(), is(0));
				assertThat(attributeTypeDB.getAll().size(), is(0));

				ie.load(new BufferedInputStream(new FileInputStream("target/afterImport.json")));

				assertThat(idDB.getByEntity(1).size(), is(1));
				assertThat(entityDB.getAll().size(), is(1));
				assertThat(attrDB.getAll().size(), is(1));
				assertThat(membershipDB.getAll().size(), is(1));
				assertThat(tokenDB.getAll().size(), is(1));
				assertThat(attributeTypeDB.get("test"), is(notNullValue()));

			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Test export/import users failed " + e);
			}
		});
	}

	@Test
	public void shouldExportAuditLogs()
	{
		tx.runInTransaction(() -> {
			try
			{
				auditDB.create(new AuditEventBuilder().action(AuditEventAction.ADD)
						.type(AuditEventType.ATTRIBUTE)
						.initiator(new AuditEntity(1L, "test", "test")).timestamp(new Date())
						.name("test").build());

				DBDumpContentElements el = new DBDumpContentElements(false, false, false, true, false, false);

				ie.store(new FileOutputStream("target/afterImport.json"), el);

				dbCleaner.deletePreImport(DBDumpContentTypeMapper.getElementsForClearDB(el));

				assertThat(auditDB.getAll().size(), is(0));

				ie.load(new BufferedInputStream(new FileInputStream("target/afterImport.json")));

				assertThat(auditDB.getAll().size(), is(1));

			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Test export/import audit events failed " + e);
			}
		});
	}

	@Test
	public void testExportSignupRequests()
	{
		tx.runInTransaction(() -> {
			try
			{
				attributeTypeDB.create(new AttributeType("test", "test"));
				EnquiryResponseState estate = new EnquiryResponseState();
				estate.setRequestId("test");
				estate.setRequest(new EnquiryResponseBuilder().withFormId("test").build());
				estate.setTimestamp(new Date());
				estate.setRegistrationContext(
						new RegistrationContext(false, TriggeringMode.manualAdmin));
				estate.setStatus(RegistrationRequestStatus.pending);
				enquiryResponseDB.create(estate);

				RegistrationRequestState rstate = new RegistrationRequestState();
				rstate.setRequestId("test");
				rstate.setRequest(new RegistrationRequestBuilder().withFormId("test").build());
				rstate.setTimestamp(new Date());
				rstate.setRegistrationContext(
						new RegistrationContext(false, TriggeringMode.manualAdmin));
				rstate.setStatus(RegistrationRequestStatus.pending);
				registrationRequestDB.create(rstate);

				invitationDB.create(new InvitationWithCode(
						new RegistrationInvitationParam("test", new Date().toInstant()),
						"test"));

				DBDumpContentElements el = new DBDumpContentElements(false, false, false, false, true, false);

				ie.store(new FileOutputStream("target/afterImport.json"), el);

				dbCleaner.deletePreImport(DBDumpContentTypeMapper.getElementsForClearDB(el));

				assertThat(invitationDB.getAll().size(), is(0));
				assertThat(enquiryResponseDB.getAll().size(), is(0));
				assertThat(registrationRequestDB.getAll().size(), is(0));
				assertThat(attributeTypeDB.getAll().size(), is(0));

				ie.load(new BufferedInputStream(new FileInputStream("target/afterImport.json")));

				assertThat(invitationDB.get("test"), is(notNullValue()));
				assertThat(enquiryResponseDB.get("test"), is(notNullValue()));
				assertThat(registrationRequestDB.get("test"), is(notNullValue()));
				assertThat(attributeTypeDB.get("test"), is(notNullValue()));

			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Test export/import signup requests failed " + e);
			}
		});
	}
}
