/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.performance;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.Before;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.rest.TestRESTBase;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
/**
 * Contains all necessary db and time method for integration tests
 * @author P.Piernik
 */
public abstract class PerformanceTestBase extends TestRESTBase
{
	public final int TEST_REPETITIONS = 10;
	
	public final int GROUP_TIERS = 3; 
	public final int GROUP_IN_TIER = 10; 
	public final int USERS = 100;  

	public final int IMAGE_ATTRIBUTES = 10; 
	public final int STRING_ATTRIBUTES = 100; 
	public final int FLOAT_ATTRIBUTES = 100;
	public final int INT_ATTRIBUTES = 100; 
			
	protected TimeHelper timer;
	
	@Before
	public void setup() throws Exception
	{
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
	        Configuration config = ctx.getConfiguration();
	        config.getLoggerConfig("unity.server").setLevel(Level.OFF);
	        ctx.updateLoggers();
	        
		setupPasswordAuthn();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Calendar c = Calendar.getInstance();
		String outputFile = "target/test-"+ getClass().getSimpleName() + "-" + 
				dateFormat.format(c.getTime()) + ".csv";
		timer = new TimeHelper(outputFile);
		System.out.println("Test output will be written to " + outputFile);
	}
	
	/**
	 * Add users with password credential	
	 *   
	 * @param n Number of user 
	 * @throws EngineException
	 */
	protected void addUsers(int n) throws EngineException
	{
		for (int i = 0; i < n; i++)
		{
			Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID,
					"user" + i), "cr-pass", EntityState.valid, false);

			eCredMan.setEntityCredential(new EntityParam(added1), "credential1",
					new PasswordToken("PassWord8743#%$^&*").toJson());
		}

	}
	
	/**
	 * Move user to random group
	 * @param n Number of user in db
	 * @param lg Number of groups in db
	 * @param t Number of group tiers
	 * @return List of group. Index in list represent user id
	 * @throws EngineException
	 */
	protected ArrayList<String> moveUserToGroup(int n, int lg,int t) throws EngineException
	{
		ArrayList<String> enInGroup = new ArrayList<String>();	
		Random r = new Random();
		for (int i = 0; i < n; i++)
		{
			enInGroup.add("/");
		}

		for (int d = 0; d < t; d++)
		{
			for (int i = 0; i < n; i++)
			{
				if (r.nextBoolean())
				{
					String base = enInGroup.get(i).equals("/") ? "" : enInGroup
							.get(i);
					String g = getRandomGroup(lg, base.split("/").length - 1, r);
					enInGroup.set(i, base + "/" + g);
					groupsMan.addMemberFromParent(enInGroup.get(i),
							new EntityParam(new IdentityParam(
									UsernameIdentity.ID, "user"
											+ i)));
				}
			}
		}
		return enInGroup;
	}

	/**
	 * Get random group number from tier
	 * @param n Number of groups in db
	 * @param g Group depth
	 * @param r Random
	 * @return group name
	 */
	private String getRandomGroup(int n, int g, Random r)
	{
		return "G" + g + "_" + r.nextInt(n);
	}
	
	/**
	 * Add group tier 
	 * @param parent Parent group
	 * @param n Number of groups in tier
	 * @param d Group Tier
	 * @param maxd Max group tier
	 * @throws EngineException
	 */
	private void addGroupTier(Group parent,int n, int d, int maxd) throws EngineException
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
			
			groupsMan.addGroup(g);
			addGroupTier(g, n, d+1, maxd);
		}
	}
	
	/**
	 * Recursive add group. 
	 * @param n Number of groups in each tier
	 * @param d Number of tiers
	 * @throws EngineException
	 */
	protected void addGroups(int n, int d) throws EngineException
	{
		addGroupTier(null, n, 0, d);
	}

	/**
	 * Get group content and recursive get subgroup content
	 * @param g 
	 * @return
	 * @throws EngineException
	 */	
	protected ArrayList<GroupContents> getGroupContent(Group g) throws EngineException
	{
		ArrayList<GroupContents> groupsC = new ArrayList<GroupContents>();
		String path = getGroupPath(g);

		GroupContents contents = groupsMan.getContents(path,
					GroupContents.EVERYTHING);
		groupsC.add(contents);		
		for (String sg:contents.getSubGroups())
		{
			groupsC.addAll(getGroupContent(new Group(sg)));
		}	
		
		return groupsC;
	}
	/**
	 * Calculate group number. 
	 * @param n Group number in each tier
	 * @param t Number of tier
	 * @return
	 */
	protected int getGroupSize(int n, int t)
	{
		int sum = 0;
		for (int i=0;i<=t;i++)
		{
			sum += Math.pow(n, i);
		}
		return sum;
	}
	
	/**
	 * Build string path from group
	 * @param g
	 * @return
	 */
	private String getGroupPath(Group g)
	{
		String path;
		if(g.getParentPath()==null)
		{
			path = g.getName();	
		}else
		{
			path = g.getParentPath().equals("/") ? g.getParentPath()
					+ g.getName() : g.getParentPath() + "/" + g.getName();
		}
		return path;
	}
	
	/**
	 * Get users attributes
	 * @param n Number of users
	 * @param enInGroup List of user group
	 * @param showToConsole If true user attr is printed to console
	 * @throws EngineException
	 */
	
	protected void getUsersAttr(int n, ArrayList<String> enInGroup, boolean showToConsole) throws EngineException
	{
		
		for (int i = 0; i < n; i++)
		{
			Collection<AttributeExt> attributes = attrsMan.getAttributes(
					new EntityParam(new IdentityParam(UsernameIdentity.ID,
							"user" + i)), enInGroup.get(i), null);
			
			if (showToConsole)
			{
				System.out.println("USER " + i);
				for (AttributeExt a : attributes)
				{
					System.out.println("ATTR:" + a.getName() + " VAL:"
							+ a.getValues().get(0) + " GROUP:"
							+ a.getGroupPath());
				}
			}
		}
			
	}	
	
	/**
	 * Get all entities from db. 
	 * @param n Number of users
	 * @return List of all entities from db
	 * @throws EngineException
	 */
	protected ArrayList<Entity> getAllEntities(int n) throws EngineException
	{
		ArrayList<Entity> es = new ArrayList<Entity>();
		for (int i = 0; i < n; i++)
		{
			Entity e = idsMan.getEntity(new EntityParam(new IdentityTaV(
					UsernameIdentity.ID, "user" + i)));
			es.add(e);
		}
		return es;
	}
	
	/**
	 * Add default attribute types. 
	 * @param n Number of types 
	 * @throws EngineException
	 */
	protected void addAttributeTypes(int n) throws EngineException
	{
		for (int i = 0; i < n; i++)
		{
			AttributeType type = new AttributeType("int_" + i,
					IntegerAttributeSyntax.ID);
			aTypeMan.addAttributeType(type);
		}

		for (int i = 0; i < n; i++)
		{
			AttributeType type = new AttributeType("string_" + i,
					StringAttributeSyntax.ID);
			aTypeMan.addAttributeType(type);
		}

		for (int i = 0; i < n; i++)
		{
			AttributeType type = new AttributeType("float_" + i,
					FloatingPointAttributeSyntax.ID);
			aTypeMan.addAttributeType(type);
		}

		for (int i = 0; i < n; i++)
		{
			AttributeType type = new AttributeType("jpeg_" + i,
					JpegImageAttributeSyntax.ID);
			aTypeMan.addAttributeType(type);
		}
		
	}
	
	
	/**
	 * Add random attributes to user
	 * @param entities
	 * @param enInGroup
	 * @param attributeTypesAsMap
	 * @param imageAttr
	 * @param stringAttr
	 * @param intAttr
	 * @param floatAttr
	 * @return
	 * @throws EngineException
	 * @throws IOException
	 */	
	protected int addRandomAttributeToEntities(ArrayList<Entity> entities,ArrayList<String> enInGroup, 
			Map<String, AttributeType> attributeTypesAsMap, int imageAttr,
			int stringAttr, int intAttr, int floatAttr)
			throws EngineException, IOException
	{
		int op = 0;
		int NU = entities.size();
		Collection<AttributeType> v = (Collection<AttributeType>) attributeTypesAsMap
				.values();
		Random r = new Random();
		
		int nDefAttr = 0;
		for(AttributeType t:v)
		{
			if(!t.getName().startsWith("ex_"))
				nDefAttr++;
		}
		
		for (int i = 0; i < imageAttr; i++)
		{
			BufferedImage im = new BufferedImage(1000, 1000, 1);
			String typeName = "jpeg_" + r.nextInt((nDefAttr / 4) - 2);
			Attribute a = JpegImageAttribute.of(typeName, enInGroup.get(i%NU), im);
			EntityParam par = new EntityParam(entities.get(i%NU).getId());
			attrsMan.setAttribute(par, a);
			op++;
		}

		for (int i = 0; i < stringAttr; i++)
		{
			String typeName = "string_" + r.nextInt((nDefAttr  / 4) - 2);
			Attribute a = StringAttribute.of(typeName, enInGroup.get(i%NU), typeName);
			EntityParam par = new EntityParam(entities.get(i%NU).getId());
			attrsMan.setAttribute(par, a);
			op++;
		}

		for (int i = 0; i < intAttr; i++)
		{
			String typeName = "int_" + r.nextInt((nDefAttr / 4) - 2);
			Attribute a = IntegerAttribute.of(typeName, enInGroup.get(i%NU),
					new Long(i + 100));
			EntityParam par = new EntityParam(entities.get(i%NU).getId());
			attrsMan.setAttribute(par, a);
			op++;
		}

		for (int i = 0; i < floatAttr; i++)
		{
			String typeName = "float_" + r.nextInt((nDefAttr / 4) - 2);
			Attribute a = FloatingPointAttribute.of(typeName, enInGroup.get(i%NU),
					new Double(i + 100));
			EntityParam par = new EntityParam(entities.get(i%NU).getId());
			attrsMan.setAttribute(par, a);
			op++;
		}
		return op;
	}
	
	/**
	 * Add additional attr type used in attr statment. Prefix "ex_"
	 * @throws EngineException
	 */
	protected void addAttributeTypeForStatments() throws EngineException
	{
		
		AttributeType type = new AttributeType("ex_everybody", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(type);
		type = new AttributeType("ex_memberof", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(type);
		type = new AttributeType("ex_ho1", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(type);	
		type = new AttributeType("ex_ho2", StringAttributeSyntax.ID);
		aTypeMan.addAttributeType(type);		
	}
	
	
	/**
	 * Add a set of attribute statements to all groups 
	 * @param groups
	 * @param attributeTypesAsMap
	 * @param d Numbers of group tiers
	 * @throws EngineException
	 */	
	protected void addAttrStatments(ArrayList<GroupContents> groups, 
			Map<String, AttributeType> attributeTypesAsMap,	int d) throws EngineException
	{
		for (GroupContents c : groups)
		{
			Group g = c.getGroup();
			String path = getGroupPath(g);
			if (path.equals("/"))
					continue;

			ArrayList<AttributeStatement> asts = new ArrayList<AttributeStatement>();

			Attribute a = StringAttribute.of("ex_everybody", path,
					new String(g.getName() + "_everybody"));
			AttributeStatement stFixed = new AttributeStatement("true", null, ConflictResolution.merge, a);
			asts.add(stFixed);

			if (path.split("/").length > 1 && path.split("/").length < d)
			{
				String group = c.getSubGroups().get(0);
				AttributeStatement stDynamic = new AttributeStatement("true", group, 
					ConflictResolution.merge, 
					"ex_ho2", "eattrs['ex_ho2']");
				asts.add(stDynamic);
			}
			addStatments(g, asts);			
			groupsMan.updateGroup(path, g);
		}
	}

	private void addStatments(Group g, ArrayList<AttributeStatement> asts)
	{
		AttributeStatement[] sts = Arrays.copyOf(g.getAttributeStatements(),
				g.getAttributeStatements().length + asts.size());

		for (int i = 0; i < asts.size(); i++)
		{
			sts[g.getAttributeStatements().length + i] = asts.get(i);
		}
		g.setAttributeStatements(sts);
	}	
}
