/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.junit.Test;

import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement2;
import pl.edu.icm.unity.types.basic.AttributeStatement2.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

public class TestAttributeStatements2 extends DBIntegrationTestBase
{
	private EntityParam entity;
	private Group groupA;
	private Group groupAB;
	private AttributeType at2;
	
	
	@Test
	public void onlyOneEntityGetsAttributeCopiedFromSubgroupIfAssignedWithStatementInSubgroup() throws Exception
	{
		setupStateForConditions();
		Identity id2 = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi2"), "crMock", 
				EntityState.disabled, false);
		EntityParam entity2 = new EntityParam(id2);
		groupsMan.addMemberFromParent("/A", entity2);
		
		AttributeStatement2 statement1 = AttributeStatement2.getFixedEverybodyStatement(
				new StringAttribute("a2", "/A/B", AttributeVisibility.local, "VV"));
		groupAB.setAttributeStatements(new AttributeStatement2[] {statement1});
		groupsMan.updateGroup("/A/B", groupAB);
		
		
		AttributeStatement2 statement2 = new AttributeStatement2("eattrs contains 'a2'", "/A/B",
				ConflictResolution.skip, 
				new StringAttribute("a2", "/A", AttributeVisibility.local, "NEW"));
		groupA.setAttributeStatements(new AttributeStatement2[] {statement2});
		groupsMan.updateGroup("/A", groupA);
		
		
		Collection<AttributeExt<?>> aRet = attrsMan.getAllAttributes(entity2, true, "/A", "a2", false);
		assertThat(aRet.isEmpty(), is(true));
		Collection<AttributeExt<?>> aRet2 = attrsMan.getAllAttributes(entity, true, "/A", "a2", false);
		assertThat(aRet2.size(), is(1));
	}

	
	private void setupStateForConditions() throws Exception
	{
		setupMockAuthn();
		
		at2 = createSimpleAT("a2");
		at2.setMaxElements(Integer.MAX_VALUE);
		attrsMan.addAttributeType(at2);
		
		groupA = new Group("/A");
		groupsMan.addGroup(groupA);
		
		groupAB = new Group("/A/B");
		groupsMan.addGroup(groupAB);

		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "cn=golbi"), "crMock", 
				EntityState.disabled, false);
		entity = new EntityParam(id);
		groupsMan.addMemberFromParent("/A", entity);
		groupsMan.addMemberFromParent("/A/B", entity);
	}
	
	private AttributeType createSimpleAT(String name)
	{
		AttributeType at = new AttributeType();
		at.setValueType(new StringAttributeSyntax());
		at.setDescription(new I18nString("desc"));
		at.setFlags(0);
		at.setMaxElements(5);
		at.setMinElements(1);
		at.setName(name);
		at.setSelfModificable(true);
		at.setVisibility(AttributeVisibility.local);
		return at;
	}
}



















