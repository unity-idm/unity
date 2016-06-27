package pl.edu.icm.unity.server.perf;
/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.engine.SecuredDBIntegrationTestBase;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeStatement2.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Contains all necessary db and time method for integration tests
 */
public class PerformanceTestBase2 extends SecuredDBIntegrationTestBase
{
	private final static String STRING_ATTR_PFX = "string_";

	private static final String AS_STRING_ATTR_PFX = "as_string_"; 
			
	protected TimeHelper timer;

	@Autowired
	protected TransactionalRunner tx;
	@Autowired
	protected DBAttributes attributeDAO;
	@Autowired
	@Qualifier("insecure")
	protected IdentitiesManagement idsMan;
	@Autowired
	@Qualifier("insecure")
	protected GroupsManagement groupsMan;
	@Autowired
	@Qualifier("insecure")
	protected AttributesManagement attrsMan;
	@Autowired
	protected SessionManagement sessionMan;
	@Autowired
	protected IdentityResolver identityResolver;
	
	
	@Before
	@Override
	public void clear() throws EngineException
	{
		//insecureServerMan.resetDatabase();
	}
	
	@Before
	public void setup() throws Exception
	{
		Logger.getLogger("unity.server").setLevel(Level.OFF);
		Logger.getLogger("pl.edu.icm.unity.store").setLevel(Level.OFF);
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Calendar c = Calendar.getInstance();
		String outputFile = "target/test-"+ getClass().getSimpleName() + "-" + 
				dateFormat.format(c.getTime()) + ".csv";
		timer = new TimeHelper(outputFile);
		System.out.println("Test output will be written to " + outputFile);
		
		IdentityParam admin = new IdentityParam(UsernameIdentity.ID, "admin");
		Entity adminUser = idsMan.getEntity(new EntityParam(admin));
		
		setupUserContext("admin", adminUser.getId());
		//setupMockAuthn();
	}
	
	private void setupUserContext(String user, long entityId) throws Exception
	{
		InvocationContext virtualAdmin = new InvocationContext(null, getDefaultRealm());
		LoginSession ls = sessionMan.getCreateSession(entityId, getDefaultRealm(),
				user, false, null);
		virtualAdmin.setLoginSession(ls);
		virtualAdmin.setLocale(Locale.ENGLISH);
		InvocationContext.setCurrent(virtualAdmin);
	}
	
	private static AuthenticationRealm getDefaultRealm()
	{
		return new AuthenticationRealm("DEFAULT_AUTHN_REALM", 
				"For tests", 5, 10, -1, 30*60);
	}
	
	/**
	 * Add users with password credential	
	 *   
	 * @param entities Number of user 
	 * @throws EngineException
	 */
	protected void addUsers(int entities, int identitiesPerEntity) throws EngineException
	{
		for (int i = 0; i < entities; i++)
		{
			Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID,
					"user" + i), "crMock", EntityState.valid, false);

			idsMan.setEntityCredential(new EntityParam(added1), "credential1",
					new PasswordToken("PassWord8743#%$^&*").toJson());
			
			for (int j=0; j<identitiesPerEntity; j++)
			{
				IdentityParam toAdd = new IdentityParam(UsernameIdentity.ID, 
						"user" + i + "_additional" + j);
				idsMan.addIdentity(toAdd, new EntityParam(added1.getEntityId()), false);
			}
		}

	}
	
	/**
	 * Add group tier 
	 * @param parent Parent group
	 * @param n Number of groups in tier
	 * @param d Group Tier
	 * @param maxd Max group tier
	 * @throws EngineException
	 */
	private void addGroupTier(Group parent,int n, int d, int maxd, int statements) throws EngineException
	{
		if (d >= maxd)
			return;
		for (int i = 0; i < n; i++)
		{
			Group g;
			if (parent != null)
				g = new Group(parent, "G" + d + "_" + i);			
			else
				g = new Group("G" + d + "_" + i);
			
			if (parent != null)
				fillGroupStatements(g, statements);
			
			groupsMan.addGroup(g);
			addGroupTier(g, n, d+1, maxd, statements);
		}
	}
	
	/**
	 * Recursive add group. 
	 * @param n Number of groups in each tier
	 * @param d Number of tiers
	 * @throws EngineException
	 */
	protected void addGroups(int n, int d, int statements) throws EngineException
	{
		addGroupTier(null, n, 0, d, statements);
	}

	private void fillGroupStatements(Group toFill, int statements)
	{
		AttributeStatement2[] attributeStatements = new AttributeStatement2[statements];
		for (int i=0; i<statements; i++)
		{
			String attr = STRING_ATTR_PFX + i;
			attributeStatements[i] = new AttributeStatement2("true", 
					toFill.getParentPath(), 
					ConflictResolution.skip, 
					AttributeVisibility.full,
					new AttributeType(AS_STRING_ATTR_PFX + i, new StringAttributeSyntax()), 
					"eattrs['" + attr + "']");
		}
		toFill.setAttributeStatements(attributeStatements);
	}
	
	/**
	 * Add default attribute types. 
	 * @param attributeTypes Number of types 
	 * @throws EngineException
	 */
	protected void addAttributeTypes(int attributeTypes, int attributeStatements) throws EngineException
	{
		for (int i = 0; i < attributeTypes; i++)
		{
			AttributeType type = new AttributeType(STRING_ATTR_PFX + i,
					new StringAttributeSyntax());
			type.setMaxElements(1000);
			attrsMan.addAttributeType(type);
		}

		for (int i = 0; i < attributeStatements; i++)
		{
			AttributeType type = new AttributeType(AS_STRING_ATTR_PFX + i,
					new StringAttributeSyntax());
			type.setMaxElements(1000);
			attrsMan.addAttributeType(type);
		}

		for (int i = 0; i < attributeTypes; i++)
		{
			AttributeType type = new AttributeType("jpeg_" + i,
					new JpegImageAttributeSyntax());
			type.setMaxElements(1000);
			attrsMan.addAttributeType(type);
		}
		
	}
	
	protected void addAttributes(int entities, int attrs) throws EngineException
	{
		Random r = new Random();
		for (int e=0; e < entities; e++)
		{
			EntityParam entity = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user" + e));
			Entity resolved = idsMan.getEntity(entity);
			Map<String, GroupMembership> groups = idsMan.getGroups(entity);
			tx.runInTransaction(() -> {
				for (String group: groups.keySet())
				{
					
					for (int i=0; i<attrs; i++)
					{
						List<String> values = Lists.newArrayList("val1", 
								"sooooooooome loooooonger vaaaaal" + r.nextLong());
						StringAttribute attribute = new StringAttribute(STRING_ATTR_PFX + i, 
								group, AttributeVisibility.full, values);
						attributeDAO.addAttribute(resolved.getId(), attribute, false, 
								SqlSessionTL.get());
					}
				}
			});
		}
	}

	protected void addEntitiesToGroups(int entitites, int groups) throws EngineException
	{
		for (int e = 0; e < entitites; e++)
			for (int g = 0; g < groups; g++)
			{
				EntityParam ep = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user" + e));
				String group = "/G0_" + g;
				groupsMan.addMemberFromParent(group, ep);
			}
	}
}
