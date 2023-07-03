/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;

public class TestGlobalAttributes extends DBIntegrationTestBase
{
	private AttributeType at1;
	private EntityParam entity;
	private Group groupA;

	@Before
	@Override
	public void setupAdmin() throws Exception
	{
		super.setupAdmin();
		setupMockAuthn();
		setupAttribtueType();
		createEntity();
		setupUserInGroup();
	}
	
	
	@Test
	public void shouldReportGlobalInRootAsDirect_SpecificAttrQuery() throws Exception
	{
		Attribute direct = StringAttribute.of(at1.getName(), "/", "val");
		attrsMan.createAttribute(entity, direct);
		
		Collection<AttributeExt> singleAttrList = attrsMan.getAllAttributes(entity, true, "/", at1.getName(), false);
		assertThat(singleAttrList).hasSize(1);
		AttributeExt singleAttr = getAttributeByName(singleAttrList, at1.getName());
		assertThat(singleAttr).isNotNull();
		assertThat(singleAttr.isDirect()).isTrue();
		assertThat(new Attribute(singleAttr)).isEqualTo(direct);
	}

	@Test
	public void shouldReportGlobalInRootAsDirect_AllAttrQuery() throws Exception
	{
		Attribute direct = StringAttribute.of(at1.getName(), "/", "val");
		attrsMan.createAttribute(entity, direct);
		
		Collection<AttributeExt> singleAttrList = attrsMan.getAllAttributes(entity, true, "/", null, false);
		AttributeExt singleAttr = getAttributeByName(singleAttrList, at1.getName());
		assertThat(singleAttr).isNotNull();
		assertThat(singleAttr.isDirect()).isTrue();
		assertThat(new Attribute(singleAttr)).isEqualTo(direct);
	}

	
	@Test
	public void shouldAddGlobalInGroup() throws Exception
	{
		Attribute direct = StringAttribute.of(at1.getName(), "/", "val");
		attrsMan.createAttribute(entity, direct);
		
		Collection<AttributeExt> singleAttrList = attrsMan.getAllAttributes(entity, true, "/A", null, false);
		AttributeExt singleAttr = getAttributeByName(singleAttrList, at1.getName());
		assertThat(singleAttr).isNotNull();
		assertThat(singleAttr.isDirect()).isFalse();
		assertThat(new Attribute(singleAttr)).isEqualTo(direct);
		assertThat(attrsMan.getAllAttributes(entity, true, "/A", at1.getName(), false)).isEqualTo(singleAttrList);
	}

	@Test
	public void shouldNotAddGlobalInGroupIfDirectDefined() throws Exception
	{
		Attribute direct = StringAttribute.of(at1.getName(), "/", "val");
		attrsMan.createAttribute(entity, direct);
		
		Attribute directInGroup = StringAttribute.of(at1.getName(), "/A", "DIRECT");
		attrsMan.createAttribute(entity, directInGroup);
		
		Collection<AttributeExt> singleAttrList = attrsMan.getAllAttributes(entity, true, "/A", null, false);
		AttributeExt singleAttr = getAttributeByName(singleAttrList, at1.getName());
		assertThat(singleAttr).isNotNull();
		assertThat(singleAttr.isDirect()).isTrue();
		assertThat(new Attribute(singleAttr)).isEqualTo(directInGroup);
		assertThat(attrsMan.getAllAttributes(entity, true, "/A", at1.getName(), false)).isEqualTo(singleAttrList);
	}

	@Test
	public void shouldNotAddGlobalInGroupIfAddedByStatement() throws Exception
	{
		Attribute direct = StringAttribute.of(at1.getName(), "/", "val");
		attrsMan.createAttribute(entity, direct);
		
		Attribute stmtAttribute = StringAttribute.of(at1.getName(), "/A", "FROM_STATEMENT");
		AttributeStatement statement2 = new AttributeStatement("true", null,
				ConflictResolution.skip,
				stmtAttribute);
		groupA.setAttributeStatements(new AttributeStatement[] {statement2});
		groupsMan.updateGroup("/A", groupA);
		
		Collection<AttributeExt> singleAttrList = attrsMan.getAllAttributes(entity, true, "/A", null, false);
		AttributeExt singleAttr = getAttributeByName(singleAttrList, at1.getName());
		assertThat(singleAttr).isNotNull();
		assertThat(singleAttr.isDirect()).isFalse();
		assertThat(new Attribute(singleAttr)).isEqualTo(stmtAttribute);
		Collection<AttributeExt> allAttributes = attrsMan.getAllAttributes(entity, true, "/A", at1.getName(), false);
		assertThat(new Attribute(getAttributeByName(allAttributes, at1.getName()))).isEqualTo(new Attribute(singleAttr));
	}

	private void setupAttribtueType() throws EngineException
	{
		at1 = createGlobalAT("attrG");
		aTypeMan.addAttributeType(at1);
	}
	
	private AttributeType createGlobalAT(String name)
	{
		AttributeType at = new AttributeType();
		at.setValueSyntax(StringAttributeSyntax.ID);
		at.setFlags(0);
		at.setGlobal(true);
		at.setMaxElements(5);
		at.setMinElements(1);
		at.setName(name);
		at.setSelfModificable(true);
		return at;
	}
	
	private void createEntity() throws EngineException
	{
		Identity id2 = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, "1"), "crMock", 
				EntityState.disabled);
		entity = new EntityParam(id2);
	}
	
	private void setupUserInGroup() throws EngineException
	{
		groupA = new Group("/A");
		groupsMan.addGroup(groupA);
		groupsMan.addMemberFromParent("/A", entity);
	}
}

