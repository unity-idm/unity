/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMember;
import pl.edu.icm.unity.types.basic.GroupProperty;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Groups management test
 * @author Krzysztof Benedyczak
 */
public class TestGroupsManagement extends RESTAdminTestBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, TestGroupsManagement.class);
	
	@Test
	public void addedGroupIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/group/subgroup");
		HttpResponse response = client.execute(host, add, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());

		HttpGet getGroupContents = new HttpGet("/restadm/v1/group/%2F");
		response = client.execute(host, getGroupContents, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		GroupContents groupContent = JsonUtil.parse(contents, GroupContents.class);
		assertThat(groupContent.getSubGroups().contains("/subgroup"), is(true));
	}
	
	@Test
	public void addedGroupsWithConfigsAreReturned() throws Exception
	{
		Group groupToAdd1 = new Group("/g1");
		Group groupToAdd12 = new Group("/g1/g2");

		HttpPost addGroups = new HttpPost("/restadm/v1/groups");
		String jsonString = JsonUtil.toJsonString(Lists.newArrayList(groupToAdd1, groupToAdd12));
		log.info("Groups to add:\n" + jsonString);
		addGroups.setEntity(new StringEntity(jsonString, ContentType.APPLICATION_JSON));
		HttpResponse addGroupResponse = client.execute(host, addGroups, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), addGroupResponse.getStatusLine().getStatusCode());

		HttpGet getGroupContents = new HttpGet("/restadm/v1/group/%2F");
		HttpResponse response = client.execute(host, getGroupContents, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		GroupContents groupContent = JsonUtil.parse(contents, GroupContents.class);
		assertThat(groupContent.getSubGroups().contains("/g1"), is(true));
		
		getGroupContents = new HttpGet("/restadm/v1/group/%2Fg1");
		response = client.execute(host, getGroupContents, localcontext);
		String contents2 = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		groupContent = JsonUtil.parse(contents2, GroupContents.class);
		assertThat(groupContent.getSubGroups().contains("/g1/g2"), is(true));
	}
	
	@Test
	public void addedGroupsWithPropertiesAreReturned() throws Exception
	{
		Group groupToAdd1 = new Group("/g1");
		groupToAdd1.setProperties(Arrays.asList(new GroupProperty("k1", "v1")));

		HttpPost addGroups = new HttpPost("/restadm/v1/groups");
		String jsonString = JsonUtil.toJsonString(Lists.newArrayList(groupToAdd1));
		log.info("Group to add:\n" + jsonString);
		addGroups.setEntity(new StringEntity(jsonString, ContentType.APPLICATION_JSON));
		HttpResponse addGroupResponse = client.execute(host, addGroups, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), addGroupResponse.getStatusLine().getStatusCode());

		HttpGet	getGroupContents = new HttpGet("/restadm/v1/group/%2Fg1/meta");
		HttpResponse response = client.execute(host, getGroupContents, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		Group group = JsonUtil.parse(contents, Group.class);
		assertThat(group.getProperties().get("k1").value, is("v1"));
	}


	@Test
	public void recursivelyAddedGroupIsReturned() throws Exception
	{
		String encodedSlash = "%2F";
		String params = "?withParents=true";
		String uri = "/restadm/v1/group/subgroup1" + encodedSlash + "subgroup2" + encodedSlash + "subgroup3" + params;
		HttpPost add = new HttpPost(uri);
		HttpResponse response = client.execute(host, add, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());

		HttpGet getGroupContents =
			new HttpGet("/restadm/v1/group/" + encodedSlash + "subgroup1" + encodedSlash + "subgroup2");
		response = client.execute(host, getGroupContents, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		GroupContents groupContent = JsonUtil.parse(contents, GroupContents.class);
		assertThat(groupContent.getSubGroups().contains("/subgroup1/subgroup2/subgroup3"), is(true));
	}

	@Test
	public void removedGroupIsNotReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/group/subgroup");
		HttpResponse addResponse = client.execute(host, add, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), addResponse.getStatusLine().getStatusCode());

		HttpDelete delete = new HttpDelete("/restadm/v1/group/subgroup");
		HttpResponse deleteResponse = client.execute(host, delete, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatusLine().getStatusCode());

		HttpGet getGroupContents = new HttpGet("/restadm/v1/group/%2F");
		HttpResponse getResponse = client.execute(host, getGroupContents, localcontext);
		String contents = EntityUtils.toString(getResponse.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
		GroupContents groupContent = JsonUtil.parse(contents, GroupContents.class);
		assertThat(groupContent.getSubGroups().contains("/subgroup"), is(false));
	}
	
	@Test
	public void addedAttributeStatementIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/group/subgroup");
		HttpResponse addResponse = client.execute(host, add, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), addResponse.getStatusLine().getStatusCode());

		
		AttributeStatement statement = new AttributeStatement("true", "/", ConflictResolution.skip, 
				"sys:AuthorizationRole", "eattr['name']");
		HttpPut addStmt = new HttpPut("/restadm/v1/group/subgroup/statements");
		String jsonString = JsonUtil.toJsonString(Lists.newArrayList(statement));
		log.info("Statements:\n" + jsonString);
		addStmt.setEntity(new StringEntity(jsonString, ContentType.APPLICATION_JSON));
		HttpResponse addStmtResponse = client.execute(host, addStmt, localcontext);
		assertEquals(Status.NO_CONTENT.getStatusCode(), addStmtResponse.getStatusLine().getStatusCode());
		
		HttpGet getGroupContents = new HttpGet("/restadm/v1/group/subgroup/statements");
		HttpResponse getResponse = client.execute(host, getGroupContents, localcontext);
		String contents = EntityUtils.toString(getResponse.getEntity());
		log.info("Statements:\n" + contents);
		assertEquals(contents, Status.OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
		List<AttributeStatement> groupStatements = Constants.MAPPER.readValue(contents, 
				new TypeReference<List<AttributeStatement>>(){});
		assertThat(groupStatements.size(), is(1));
	}
	
	@Test
	public void shouldAddEntityToGroupAndItsParents() throws Exception
	{
		// given
		setupUserContext(DEF_USER, null);
		Group a = new Group("/A");
		groupsMan.addGroup(a);
		Group ab = new Group("/A/B");
		groupsMan.addGroup(ab);
		Group abc = new Group("/A/B/C");
		groupsMan.addGroup(abc);
		EntityParam entityParam = new EntityParam(new IdentityTaV(UsernameIdentity.ID, DEF_USER));
		groupsMan.addMemberFromParent("/A", entityParam);
		
		// expect
		Set<String> existingGroups = idsMan.getGroups(entityParam).keySet();
		assertThat(existingGroups, equalTo(Sets.newHashSet("/", "/A")));
		
		HttpPost add = new HttpPost("/restadm/v1/group/%2FA%2FB%2FC/entity/" +  DEF_USER + "?identityType=" + UsernameIdentity.ID);
		
		// when
		HttpResponse addResponse = client.execute(host, add, localcontext);
		
		// then
		assertEquals(Status.NO_CONTENT.getStatusCode(), addResponse.getStatusLine().getStatusCode());
		existingGroups = idsMan.getGroups(entityParam).keySet();
		assertThat(existingGroups, equalTo(Sets.newHashSet("/", "/A", "/A/B", "/A/B/C")));
	}
	
	@Test
	public void shouldReturnEntityWithBulQuery() throws Exception
	{
		// given
		setupUserContext(DEF_USER, null);
		
		HttpGet get = new HttpGet("/restadm/v1/group-members/%2F");
		
		// when
		HttpResponse getResponse = client.execute(host, get, localcontext);
		
		// then
		String contents = EntityUtils.toString(getResponse.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), getResponse.getStatusLine().getStatusCode());
		List<GroupMember> groupContent = Constants.MAPPER.readValue(contents, 
				new TypeReference<List<GroupMember>>(){});
		assertThat(groupContent.size(), is(2));
	}
}
