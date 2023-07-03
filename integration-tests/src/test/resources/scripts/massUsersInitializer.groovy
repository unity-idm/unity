/*
 * Creates many users
 *
 * Depends on defaultContentInitializer.groovy
 */
import pl.edu.icm.unity.engine.server.EngineInitialization
import pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider
import pl.edu.icm.unity.stdext.attr.EnumAttribute
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax
import pl.edu.icm.unity.stdext.attr.StringAttribute
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken
import pl.edu.icm.unity.stdext.identity.UsernameIdentity
import pl.edu.icm.unity.stdext.identity.X500Identity
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner.TxRunnable;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner.TxRunnableRet;
import pl.edu.icm.unity.store.types.StoredIdentity
import pl.edu.icm.unity.store.types.StoredAttribute
import pl.edu.icm.unity.base.attribute.Attribute
import pl.edu.icm.unity.base.attribute.AttributeExt
import pl.edu.icm.unity.base.attribute.AttributeStatement
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution
import pl.edu.icm.unity.base.attribute.AttributeType
import pl.edu.icm.unity.base.entity.EntityParam
import pl.edu.icm.unity.base.entity.EntityState
import pl.edu.icm.unity.base.entity.EntityInformation
import pl.edu.icm.unity.base.group.Group
import pl.edu.icm.unity.base.group.GroupContents
import pl.edu.icm.unity.base.group.GroupMembership
import pl.edu.icm.unity.base.identity.Identity
import pl.edu.icm.unity.base.identity.IdentityParam
import pl.edu.icm.unity.base.verifiable.VerifiableEmail
import pl.edu.icm.unity.base.confirmation.ConfirmationInfo
import groovy.transform.Field


@Field final String FNAME_ATTR = "firstname"
@Field final String LNAME_ATTR = "surname"
@Field final String EMAIL_ATTR = "email";
@Field final int ADMIN_IN_GROUPS = 1000;
@Field final int USER_IN_GROUPS = 5;
@Field final int ENTITIES = 30;
@Field final int GROUPS = 20;
@Field final int IN_BATCH = 1000;

//if (!isColdStart)
//{
//	log.info("Database already initialized with content, skipping...");
//	return;
//}

log.info("Creating demo content...");

try
{
	GroupContents rootContents = groupsManagement.getContents("/", GroupContents.GROUPS);
	
	Map<String, AttributeType> existingATs = attributeTypeManagement.getAttributeTypesAsMap();
	if (!existingATs.containsKey(FNAME_ATTR) || !existingATs.containsKey(EMAIL_ATTR))
	{
		log.error("Demo contents can be only installed if standard types were installed " +  
			"prior to it. Attribute types cn, o and email are required.");
		return;
	}
	
	createExampleGroups();
	createExampleUsers();
	addAdminToGroups();
	addUsersToGroups();
	
} catch (Exception e)
{
	log.warn("Error loading demo contents. This can happen and by far is not critical. " +
			"It means that demonstration contents was not loaded to your database, " +
			"usaully due to conflict with its existing data", e);
}

void createExampleGroups()
{
	groupsManagement.addGroup(new Group("/root"));
	GroupDAO dbGroups = applicationContext.getBean(GroupDAO.class);
	TransactionalRunner tx = applicationContext.getBean(TransactionalRunner.class);
	for (int j=0; j<GROUPS; j++)
	{
		List<Group> list = new ArrayList<>();    
		for (int i=0; i<IN_BATCH; i++)
		{
			Group grp = new Group("/root/grp" + (IN_BATCH*j + i));
			AttributeStatement fnameStmt = new AttributeStatement("true", "/", ConflictResolution.skip, 
				FNAME_ATTR, "eattr['firstname']");
			AttributeStatement lnameStmt = new AttributeStatement("true", "/", ConflictResolution.skip, 
				LNAME_ATTR, "eattr['surname']");
			AttributeStatement emailStmt = new AttributeStatement("true", "/", ConflictResolution.skip, 
				EMAIL_ATTR, "eattr['email']");
			AttributeStatement[] statements = [fnameStmt, lnameStmt, emailStmt];
			grp.setAttributeStatements(statements);
			list.add(grp);
		}
		
		tx.runInTransaction(new TxRunnable() 
		{
			public void run() { 
				dbGroups.createList(list);
			}
		});
		log.info("Added {} groups", list.size());
	}
}

void setCredentialForFirst()
{
	EntityParam entityP = new EntityParam(3);
	Attribute a = EnumAttribute.of("sys:AuthorizationRole", "/", "System Manager");
	attributesManagement.createAttribute(entityP, a);
	PasswordToken pToken = new PasswordToken("the!test12");
	entityCredentialManagement.setEntityCredential(entityP, EngineInitialization.DEFAULT_CREDENTIAL, pToken.toJson());
}



void createExampleUsers()
{
	EntityDAO dbEntities = applicationContext.getBean(EntityDAO.class);
	IdentityDAO dbIdentities = applicationContext.getBean(IdentityDAO.class);
	MembershipDAO dbMembership = applicationContext.getBean(MembershipDAO.class);
	AttributeDAO dbAttributes = applicationContext.getBean(AttributeDAO.class);
	TransactionalRunner tx = applicationContext.getBean(TransactionalRunner.class);
	for (int j=0; j<ENTITIES; j++)
	{
		List<Group> list = new ArrayList<>();    
		for (int i=0; i<IN_BATCH; i++)
		{
			list.add(new EntityInformation());
		}
		
		List<Long> ids = tx.runInTransactionRet(new TxRunnableRet() 
		{
			public List<Long> run() { 
				return dbEntities.createList(list);
			}
		});
		log.info("Added {} entities", list.size());
	
		int base = IN_BATCH*j;
		List<Identity> identities1 = new ArrayList<>();
		for (int i=0; i<IN_BATCH; i++)
		{
			String value = "user-id-" + (base+i);
			identities1.add(new StoredIdentity(new Identity("identifier", value,
					ids.get(i), "identifier:" + value)));
			identities1.add(new StoredIdentity(new Identity(UsernameIdentity.ID, value,
					ids.get(i), "username:" + value)));
		}
		
		tx.runInTransaction(new TxRunnable() 
		{
			public void run() { 
				dbIdentities.createList(identities1);
			}
		});
		log.info("Added {} identities", identities1.size());
		
		List<GroupMembership> memberships = new ArrayList<>();
		Date date = new Date();
		for (int i=0; i<IN_BATCH; i++)
		{
			memberships.add(new GroupMembership("/", ids.get(i), date));
		}
		
		tx.runInTransaction(new TxRunnable() 
		{
			public void run() { 
				dbMembership.createList(memberships);
			}
		});
		log.info("Added {} to / group", memberships.size());
		
		List<StoredAttribute> attributes = new ArrayList<>();
		for (int i=0; i<IN_BATCH; i++)
		{
			Attribute credReq = StringAttribute.of(CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS,
				"/", "sys:all");
			attributes.add(new StoredAttribute(new AttributeExt(credReq, true), ids.get(i)));

			int suffix = base+i		
			VerifiableEmail emailVal = new VerifiableEmail("some" + suffix + "@example.com", new ConfirmationInfo(true));
			emailVal.getConfirmationInfo().setConfirmationDate(System.currentTimeMillis());
			emailVal.getConfirmationInfo().setConfirmed(true);
			Attribute emailA = VerifiableEmailAttribute.of(EMAIL_ATTR, "/", emailVal);
			attributes.add(new StoredAttribute(new AttributeExt(emailA, true), ids.get(i)))

			Attribute fnameA = StringAttribute.of(FNAME_ATTR, "/", "Demo " + suffix);
			attributes.add(new StoredAttribute(new AttributeExt(fnameA, true), ids.get(i)))

			Attribute lnameA = StringAttribute.of(LNAME_ATTR, "/", "User " + suffix);
			attributes.add(new StoredAttribute(new AttributeExt(lnameA, true), ids.get(i)))
		}
		
		tx.runInTransaction(new TxRunnable() 
		{
			public void run() { 
				dbAttributes.createList(attributes);
			}
		});
		log.info("Added {} attributes", attributes.size());
	}
}

void addAdminToGroups()
{
	MembershipDAO dbMembership = applicationContext.getBean(MembershipDAO.class);
	TransactionalRunner tx = applicationContext.getBean(TransactionalRunner.class);
	
	List<GroupMembership> memberships = new ArrayList<>();
	Date date = new Date();
	memberships.add(new GroupMembership("/root", 1l, date));
	for (int i=0; i<ADMIN_IN_GROUPS; i++)
	{
		memberships.add(new GroupMembership("/root/grp" + (i % (GROUPS * IN_BATCH)), 1l, date));
	}
		
	tx.runInTransaction(new TxRunnable() 
	{
		public void run() { 
			dbMembership.createList(memberships);
		}
	});
	log.info("Added admin to {} groups", memberships.size());
}

void addUsersToGroups()
{
	MembershipDAO dbMembership = applicationContext.getBean(MembershipDAO.class);
	TransactionalRunner tx = applicationContext.getBean(TransactionalRunner.class);
	
	Date date = new Date();
	for (int j=0; j<ENTITIES; j++)
	{
		List<GroupMembership> memberships = new ArrayList<>();
		for (int i=0; i<IN_BATCH; i++)
		{
			int entityNo = j*IN_BATCH + i;
			memberships.add(new GroupMembership("/root", entityNo+2, date));
		}
		tx.runInTransaction(new TxRunnable() 
		{
			public void run() { 
				dbMembership.createList(memberships);
			}
		});
		log.info("Added {} user memberships to /root", memberships.size());
	}
	
	for (int j=0; j<ENTITIES; j++)
	{
		List<GroupMembership> memberships = new ArrayList<>();
		for (int i=0; i<IN_BATCH; i++)
		{
			int entityNo = j*IN_BATCH + i;
			for (int k=0; k<USER_IN_GROUPS; k++)
			{
				memberships.add(new GroupMembership("/root/grp" + ((entityNo+k) % (GROUPS * IN_BATCH)), entityNo+2, date));
			}		
		}
		tx.runInTransaction(new TxRunnable() 
		{
			public void run() { 
				dbMembership.createList(memberships);
			}
		});
		log.info("Added {} user memberships to groups", memberships.size());
	}
}


