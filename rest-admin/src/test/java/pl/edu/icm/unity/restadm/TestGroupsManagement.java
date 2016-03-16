/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.basic.GroupContentsRepresentation;

/**
 * Groups management test
 * @author Krzysztof Benedyczak
 */
public class TestGroupsManagement extends RESTAdminTestBase
{
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
		GroupContentsRepresentation groupContent = Constants.MAPPER.readValue(contents,
				GroupContentsRepresentation.class);
		assertThat(groupContent.getSubGroups().contains("/subgroup"), is(true));
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
		GroupContentsRepresentation groupContent = Constants.MAPPER.readValue(contents,
				GroupContentsRepresentation.class);
		assertThat(groupContent.getSubGroups().contains("/subgroup"), is(false));
	}
}
