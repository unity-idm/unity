package pl.edu.icm.unity.tests;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.attrstmnt.CopyParentAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.CopySubgroupAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.EverybodyStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.HasParentAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.HasSubgroupAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.MemberOfStatement;

public class TestPerformance extends DBIntegrationTestBase
{

	int N = 10; // Group n*n*n*n
	int NU = 100; // users
	int ImageAttr = 10; // image attribute
	int StingAttr = 100; // string attribute
	int FloatAttr = 100; // float attributes
	int IntAttr = 100; // int attributes
	String file = "target/tests.csv";
	
	private boolean consoleOut = true;
	private boolean userAttrOut = false;

	private long startT;
	private ArrayList<String> enInGroup;
	
	private void startTimer()
	{
		startT = System.currentTimeMillis();
	}

	private void stopTimer(int ops, String label) throws IOException
	{
		long endT = System.currentTimeMillis();
		long periodMs = endT - startT;
		double periodS = periodMs / 1000.0;
		double opsPerS = (ops * 1000 / periodMs);
		if (consoleOut)
			System.out.println(label + " performed " + ops + " in " + periodS + "s, "+ opsPerS + " ops/s");
		
		FileWriter fw = new FileWriter(file,true);
		PrintWriter pw = new PrintWriter(fw);
		pw.println(label + "," + ops + "," + periodS + "," + opsPerS);
		pw.flush();
		pw.close();
		fw.close();
	
	}

	@Before
	public void setup() throws Exception
	{
		enInGroup = new ArrayList<String>();
		setupPasswordAuthn();
		File f = new File(file);
		if(f.exists())
			f.delete();	
		fillDatabase();	
	}

	private void addUsers(int n) throws EngineException
	{
		for (int i = 0; i < n; i++)
		{
			Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID,
					"user" + i), "cr-pass", EntityState.valid, false);

			idsMan.setEntityCredential(new EntityParam(added1), "credential1",
					new PasswordToken("PassWord8743#%$^&*").toJson());
		}

	}

	private void fillDatabase() throws EngineException, IOException
	{

		startTimer();
		addGroups(N);
		stopTimer(N * N * N * N, "Group add");

		startTimer();
		addUsers(NU);
		stopTimer(NU, "Add user");

		startTimer();
		stopTimer(relockUser(NU, N), "Add user to groups (from parent operation)");

		startTimer();
		addAttributeTypes(N + 1);
		stopTimer(N + N + N + N, "Add attribute types");

		// startTimer();
		ArrayList<GroupContents> con = getGroupsContents(N);
		// stopTimer(N * N * N * N, "Get group content");

		int extraTypes = addAttrStatmentTypes();

		Map<String, AttributeType> attributeTypesAsMap = attrsMan.getAttributeTypesAsMap();

		startTimer();
		addAttrStatments(con, attributeTypesAsMap);
		stopTimer(N * N * N * N, "Update group, add attr statment");

		ArrayList<Entity> entities = getAllEntities(NU);

		startTimer();
		stopTimer(addRandomAttributeToEntities(entities, attributeTypesAsMap, ImageAttr,
				StingAttr, IntAttr, FloatAttr, extraTypes),
				"Add attribute to entity");

	}

	@Test
	public void testFull() throws EngineException, IOException
	{

		if (consoleOut)
			System.out.println("Starting test getters:");

		startTimer();
		getUsersAttr(NU);
		stopTimer(NU, "Get attributes for user");

		startTimer();
		getAllEntities(NU);
		stopTimer(NU, "Get entities");

		startTimer();
		getGroupsContents(N);
		stopTimer(N * N * N * N, "Get group content");

	}

	private int relockUser(int n, int g) throws EngineException
	{
		int op = 0;
		Random r = new Random();
		enInGroup.clear();
		for (int i = 0; i < n; i++)
		{
			enInGroup.add("/");
		}

		for (int d = 0; d < 4; d++)
		{

			for (int i = 0; i < n; i++)
			{

				if (r.nextBoolean())
				{
					String base = enInGroup.get(i).equals("/") ? "" : enInGroup
							.get(i);
					String t = getRandomGroup(g, base.split("/").length - 1);
					enInGroup.set(i, base + "/" + t);
					groupsMan.addMemberFromParent(enInGroup.get(i),
							new EntityParam(new IdentityParam(
									UsernameIdentity.ID, "user"
											+ i)));
					op++;
				}
			}
		}
		return op;

	}

	private String getRandomGroup(int n, int g)
	{
		Random r = new Random();
		return "G" + g + "_" + r.nextInt(n);
	}

	private void addGroups(int n) throws EngineException
	{

		for (int i = 0; i < n; i++)
		{
			Group g = new Group("G0_" + i);
			groupsMan.addGroup(g);
			for (int j = 0; j < n; j++)
			{
				Group g2 = new Group(g, "G1_" + j);
				groupsMan.addGroup(g2);

				for (int k = 0; k < n; k++)
				{
					Group g3 = new Group(g2, "G2_" + k);
					groupsMan.addGroup(g3);

					for (int h = 0; h < n; h++)
					{
						Group g4 = new Group(g3, "G3_" + h);
						groupsMan.addGroup(g4);
					}
				}

			}
		}
	}

	private void getUsersAttr(int n) throws EngineException
	{

		for (int i = 0; i < n; i++)
		{
			Collection<AttributeExt<?>> attributes = attrsMan.getAttributes(
					new EntityParam(new IdentityParam(UsernameIdentity.ID,
							"user" + i)), enInGroup.get(i), null);
			if (consoleOut&&userAttrOut)
			{
				System.out.println("USER " + i);
				for (AttributeExt<?> a : attributes)
				{
					System.out.println("ATTR:" + a.getName() + " VAL:"
							+ a.getValues().get(0) + " GROUP:"
							+ a.getGroupPath());
				}
			}
		}
	}

	private ArrayList<Entity> getAllEntities(int n) throws EngineException
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

	private int addRandomAttributeToEntities(ArrayList<Entity> entities,
			Map<String, AttributeType> attributeTypesAsMap, int imageAttr,
			int stringAttr, int intAttr, int floatAttr, int extraAttr)
			throws EngineException, IOException
	{
		int op = 0;
		Collection<AttributeType> v = (Collection<AttributeType>) attributeTypesAsMap
				.values();
		Random r = new Random();

		for (int i = 0; i < imageAttr; i++)
		{

			BufferedImage im = new BufferedImage(1000, 1000, 1);
			String typeName = "jpeg_" + r.nextInt((v.size() - extraAttr) / 4 - 1);
			Attribute<?> a = new Attribute(typeName, attributeTypesAsMap.get(typeName)
					.getValueType(), enInGroup.get(i%NU),
					AttributeVisibility.full, Collections.singletonList(im));
			EntityParam par = new EntityParam(entities.get(i%NU).getId());
			attrsMan.setAttribute(par, a, true);
			op++;
		}

		for (int i = 0; i < stringAttr; i++)
		{

			String typeName = "string_" + r.nextInt((v.size() - extraAttr) / 4 - 1);
			Attribute<?> a = new Attribute(typeName, attributeTypesAsMap.get(typeName)
					.getValueType(), enInGroup.get(i%NU),
					AttributeVisibility.full,
					Collections.singletonList(new String(typeName)));
			EntityParam par = new EntityParam(entities.get(i%NU).getId());
			attrsMan.setAttribute(par, a, true);
			op++;
		}

		for (int i = 0; i < intAttr; i++)
		{

			String typeName = "int_" + r.nextInt((v.size() - extraAttr) / 4 - 1);
			Attribute<?> a = new Attribute(typeName, attributeTypesAsMap.get(typeName)
					.getValueType(), enInGroup.get(i%NU),
					AttributeVisibility.full,
					Collections.singletonList(new Long(i + 100)));
			EntityParam par = new EntityParam(entities.get(i%NU).getId());
			attrsMan.setAttribute(par, a, true);
			op++;
		}

		for (int i = 0; i < floatAttr; i++)
		{

			String typeName = "float_" + r.nextInt((v.size() - extraAttr) / 4 - 1);
			Attribute<?> a = new Attribute(typeName, attributeTypesAsMap.get(typeName)
					.getValueType(), enInGroup.get(i%NU),
					AttributeVisibility.full,
					Collections.singletonList(new Double(i + 100)));
			EntityParam par = new EntityParam(entities.get(i%NU).getId());
			attrsMan.setAttribute(par, a, true);
			op++;
		}

		return op;

	}

	private void addAttributeTypes(int n) throws EngineException
	{

		for (int i = 0; i < n; i++)
		{
			AttributeType type = new AttributeType("int_" + i,
					new IntegerAttributeSyntax());
			attrsMan.addAttributeType(type);
		}

		for (int i = 0; i < n; i++)
		{
			AttributeType type = new AttributeType("string_" + i,
					new StringAttributeSyntax());
			attrsMan.addAttributeType(type);
		}

		for (int i = 0; i < n; i++)
		{
			AttributeType type = new AttributeType("float_" + i,
					new FloatingPointAttributeSyntax());
			attrsMan.addAttributeType(type);
		}

		for (int i = 0; i < n; i++)
		{
			AttributeType type = new AttributeType("jpeg_" + i,
					new JpegImageAttributeSyntax());
			attrsMan.addAttributeType(type);
		}

	}

	public ArrayList<GroupContents> getGroupsContents(int n) throws EngineException
	{

		ArrayList<GroupContents> groupsC = new ArrayList<GroupContents>();

		for (int i = 0; i < n; i++)
		{
			GroupContents contents = groupsMan.getContents("/G0_" + i,
					GroupContents.EVERYTHING);
			groupsC.add(contents);

			for (int j = 0; j < n; j++)
			{
				GroupContents contents2 = groupsMan.getContents("/G0_" + i + "/G1_"
						+ j, GroupContents.EVERYTHING);
				groupsC.add(contents2);

				for (int k = 0; k < n; k++)
				{
					GroupContents contents3 = groupsMan.getContents("/G0_" + i
							+ "/G1_" + j + "/G2_" + k,
							GroupContents.EVERYTHING);
					groupsC.add(contents3);

					for (int h = 0; h < n; h++)
					{
						GroupContents contents4 = groupsMan.getContents(
								"/G0_" + i + "/G1_" + j + "/G2_"
										+ k + "/G3_" + h,
								GroupContents.EVERYTHING);
						groupsC.add(contents4);
					}
				}
			}
		}
		return groupsC;
	}

	private int addAttrStatmentTypes() throws EngineException
	{
		int i = 0;
		AttributeType type = new AttributeType("everybody", new StringAttributeSyntax());
		attrsMan.addAttributeType(type);
		i++;

		type = new AttributeType("memberof", new StringAttributeSyntax());
		attrsMan.addAttributeType(type);
		i++;

		type = new AttributeType("ho1", new StringAttributeSyntax());
		attrsMan.addAttributeType(type);
		i++;
		
		type = new AttributeType("ho2", new StringAttributeSyntax());
		attrsMan.addAttributeType(type);
		i++;

		return i;
	}

	private void addAttrStatments(ArrayList<GroupContents> groups,
			Map<String, AttributeType> attributeTypesAsMap) throws EngineException
	{

		for (GroupContents c : groups)
		{

			Group g = c.getGroup();
			String path = g.getParentPath().equals("/") ? g.getParentPath()
					+ g.getName() : g.getParentPath() + "/" + g.getName();

			ArrayList<AttributeStatement> asts = new ArrayList<AttributeStatement>();
			AttributeStatement st = new EverybodyStatement();
			Attribute<?> a = new Attribute("everybody", attributeTypesAsMap.get(
					"everybody").getValueType(), path,
					AttributeVisibility.full,
					Collections.singletonList(new String(g.getName()
							+ "_everybody")));
			st.setAssignedAttribute(a);
			st.setConflictResolution(ConflictResolution.merge);
			asts.add(st);

			st = new MemberOfStatement();
			a = new Attribute("memberof", attributeTypesAsMap.get("memberof")
					.getValueType(), path, AttributeVisibility.full,
					Collections.singletonList(new String(g.getName()
							+ "_memberof")));
			st.setAssignedAttribute(a);
			st.setConditionGroup(path);
			st.setConflictResolution(ConflictResolution.merge);
			asts.add(st);

			st = new CopyParentAttributeStatement();
			st.setConditionAttribute(new Attribute("string_0", attributeTypesAsMap.get(
					"string_0").getValueType(),
					new Group(path).getParentPath(), AttributeVisibility.full,
					null));
			st.setConflictResolution(ConflictResolution.merge);
			asts.add(st);	
			if (path.split("/").length > 1 && path.split("/").length < 4)
			{
				st = new CopySubgroupAttributeStatement();
				st.setConditionAttribute(new Attribute("string_0",
						attributeTypesAsMap.get("string_0").getValueType(),
						c.getSubGroups().get(0), AttributeVisibility.full,
						null));
				st.setConflictResolution(ConflictResolution.merge);
				asts.add(st);
						
				st = new HasSubgroupAttributeStatement();
				st.setConditionAttribute(new Attribute("everybody", attributeTypesAsMap.get(
						"everybody").getValueType(),
						c.getSubGroups().get(0), AttributeVisibility.full,
						null));
				
				st.setAssignedAttribute(new Attribute("ho2", attributeTypesAsMap.get(
						"ho2").getValueType(),
						path, AttributeVisibility.full,
						Collections.singletonList(new String(g.getName()+"_ho2"))));
				st.setConditionGroup(c.getSubGroups().get(0));	
				st.setConflictResolution(ConflictResolution.merge);
				asts.add(st);
			}
			
			st = new HasParentAttributeStatement();
			st.setConditionAttribute(new Attribute("everybody", attributeTypesAsMap.get(
					"everybody").getValueType(),
					new Group(path).getParentPath(), AttributeVisibility.full,
					null));
			
			st.setAssignedAttribute(new Attribute("ho1", attributeTypesAsMap.get(
					"ho1").getValueType(),
					path, AttributeVisibility.full,
					Collections.singletonList(new String(g.getName()+"_ho1"))));
			
			st.setConflictResolution(ConflictResolution.merge);
			asts.add(st);
		
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
