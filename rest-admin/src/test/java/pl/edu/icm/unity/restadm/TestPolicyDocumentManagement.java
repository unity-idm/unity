/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentId;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;


public class TestPolicyDocumentManagement extends RESTAdminTestBase
{
	private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

	@BeforeEach
	public void setUp()
	{
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	@Test
	public void addedPolicyDocumentIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		String idJson = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestPolicyDocumentId id = JsonUtil.parse(idJson, RestPolicyDocumentId.class);

		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/" + id.id);

		String contents = client.execute(host, getPolicyDocument, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestPolicyDocument document = JsonUtil.parse(contents, RestPolicyDocument.class);

		assertThat(document.id).isEqualTo(id.id);
		assertThat(document.name).isEqualTo("Ala");
		assertThat(document.contentType).isEqualTo(PolicyDocumentContentType.EMBEDDED.name());
		assertThat(document.displayedName).isEqualTo(Map.of("en", "Ola"));
		assertThat(document.revision).isEqualTo(1);
		assertThat(document.content).isEqualTo(Map.of("en", "la la"));
	}

	@Test
	public void notFullBodyOfPolicyDocumentNotAdded() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host)))
		{
			assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void notAddedPolicyDocumentIsNotReturned() throws Exception
	{
		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/" + 1234);

		try(ClassicHttpResponse response = client.executeOpen(host, getPolicyDocument, getClientContext(host)))
		{
			assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void notAddedPolicyDocumentIsNotRemoved() throws Exception
	{
		HttpDelete getPolicyDocument = new HttpDelete("/restadm/v1/policy-documents/" + 1234);

		try(ClassicHttpResponse response = client.executeOpen(host, getPolicyDocument, getClientContext(host)))
		{
			assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void addedPoliciesDocumentsAreReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());

		HttpPost add2 = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build2 = RestPolicyDocumentRequest.builder()
			.withName("Ola")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
		add2.setEntity(new StringEntity(mapper.writeValueAsString(build2)));
		client.execute(host, add2, getClientContext(host), new BasicHttpClientResponseHandler());

		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/");
		String contents = client.execute(host, getPolicyDocument, getClientContext(host),
			new BasicHttpClientResponseHandler());
		List<RestPolicyDocument> documents = JsonUtil.parseToList(contents, RestPolicyDocument.class);

		assertThat(documents.size()).isEqualTo(2);
		assertThat(documents.stream().map(d -> d.name).collect(Collectors.toSet())).isEqualTo(Set.of("Ala", "Ola"));
	}

	@Test
	public void policiesDocumentWithExistingNameNotAdded() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));
		client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());

		HttpPost add2 = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build2 = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
		add2.setEntity(new StringEntity(mapper.writeValueAsString(build2)));
		try(ClassicHttpResponse response = client.executeOpen(host, add2, getClientContext(host)))
		{
			assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void trashBodyOfPolicyDocumentNotAdded() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/policy-documents");
		add.setEntity(new StringEntity(mapper.writeValueAsString("sfsdfsdfsfdkjfjsd")));
		try(ClassicHttpResponse response = client.executeOpen(host, add, getClientContext(host))){
			assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void updatedPolicyDocumentIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		String idJson = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestPolicyDocumentId id = JsonUtil.parse(idJson, RestPolicyDocumentId.class);

		HttpPut put = new HttpPut("/restadm/v1/policy-documents/" + id.id + "?incrementRevision=false");
		RestPolicyDocumentRequest updateBuild = RestPolicyDocumentRequest.builder()
			.withName("Zla")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Zla"))
			.withContentType(PolicyDocumentContentType.LINK.name())
			.withContent(Map.of("en", "lo lo"))
			.build();
		put.setEntity(new StringEntity(mapper.writeValueAsString(updateBuild)));

		try(ClassicHttpResponse response = client.executeOpen(host, put, getClientContext(host))){
			assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/" + id.id);

		String contents = client.execute(host, getPolicyDocument, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestPolicyDocument document = JsonUtil.parse(contents, RestPolicyDocument.class);

		assertThat(document.id).isEqualTo(id.id);
		assertThat(document.name).isEqualTo("Zla");
		assertThat(document.contentType).isEqualTo(PolicyDocumentContentType.LINK.name());
		assertThat(document.displayedName).isEqualTo(Map.of("en", "Zla"));
		assertThat(document.revision).isEqualTo(1);
		assertThat(document.content).isEqualTo(Map.of("en", "lo lo"));
	}

	@Test
	public void notAddedPolicyDocumentIsNotUpdated() throws Exception
	{
		HttpPut put = new HttpPut("/restadm/v1/policy-documents/" + "123?incrementRevision=false");
		RestPolicyDocumentRequest updateBuild = RestPolicyDocumentRequest.builder()
			.withName("Zla")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Zla"))
			.withContentType(PolicyDocumentContentType.LINK.name())
			.withContent(Map.of("en", "lo lo"))
			.build();
		put.setEntity(new StringEntity(mapper.writeValueAsString(updateBuild)));

		try(ClassicHttpResponse response = client.executeOpen(host, put, getClientContext(host))){
			assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void updatedPolicyDocumentIsReturnedWithRevision() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "bb bb"))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		String idJson = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestPolicyDocumentId id = JsonUtil.parse(idJson, RestPolicyDocumentId.class);

		HttpPut put = new HttpPut("/restadm/v1/policy-documents/" + id.id + "?incrementRevision=true");
		RestPolicyDocumentRequest updateBuild = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Zla"))
			.withContentType(PolicyDocumentContentType.LINK.name())
			.withContent(Map.of("en", "lo lo"))
			.build();
		put.setEntity(new StringEntity(mapper.writeValueAsString(updateBuild)));

		try(ClassicHttpResponse response = client.executeOpen(host, put, getClientContext(host))){
			assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/" + id.id);

		String contents = client.execute(host, getPolicyDocument, getClientContext(host),
			new BasicHttpClientResponseHandler());
		RestPolicyDocument document = JsonUtil.parse(contents, RestPolicyDocument.class);

		assertThat(document.id).isEqualTo(id.id);
		assertThat(document.name).isEqualTo("Ala");
		assertThat(document.contentType).isEqualTo(PolicyDocumentContentType.LINK.name());
		assertThat(document.displayedName).isEqualTo(Map.of("en", "Zla"));
		assertThat(document.revision).isEqualTo(2);
		assertThat(document.content).isEqualTo(Map.of("en", "lo lo"));
	}

	@Test
	public void wrongPathBring404() throws Exception
	{
		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/123/content");
		try(ClassicHttpResponse response = client.executeOpen(host, getPolicyDocument, getClientContext(host))){
			assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void removedGroupIsNotReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.EMBEDDED.name())
			.withContent(Map.of("en", "la la"))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		String idJson = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestPolicyDocumentId id = JsonUtil.parse(idJson, RestPolicyDocumentId.class);

		HttpDelete deleteDocument = new HttpDelete("/restadm/v1/policy-documents/" + id.id);
		try(ClassicHttpResponse response = client.executeOpen(host, deleteDocument, getClientContext(host)))
		{
			assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/" + id.id);
		try(ClassicHttpResponse response = client.executeOpen(host, getPolicyDocument, getClientContext(host)))
		{
			assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getCode());
		}
	}
}
