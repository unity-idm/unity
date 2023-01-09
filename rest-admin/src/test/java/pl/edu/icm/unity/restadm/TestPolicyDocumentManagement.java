/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentId;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertEquals;


public class TestPolicyDocumentManagement extends RESTAdminTestBase
{
	private ObjectMapper mapper = Constants.MAPPER;


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

		HttpPut put = new HttpPut("/restadm/v1/policy-documents/" + id.id);
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
			.withContent(Map.of("en", "la la"))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		String idJson = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestPolicyDocumentId id = JsonUtil.parse(idJson, RestPolicyDocumentId.class);

		HttpPut put = new HttpPut("/restadm/v1/policy-documents/" + id.id + "?revision=true");
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
		assertThat(document.revision).isEqualTo(2);
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
			assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getCode());
		}
	}

	@Test
	public void updatedPolicyDocumentContentIsReturned() throws Exception
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

		HttpPut put = new HttpPut("/restadm/v1/policy-documents/" + id.id);
		RestPolicyDocumentRequest updateBuild = RestPolicyDocumentRequest.builder()
			.withName("Zla")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Zla"))
			.withContentType(PolicyDocumentContentType.LINK.name())
			.withContent(Map.of("en", "link2"))
			.build();
		put.setEntity(new StringEntity(mapper.writeValueAsString(updateBuild)));

		try(ClassicHttpResponse response = client.executeOpen(host, put, getClientContext(host))){
			assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getCode());
		}

		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/" + id.id + "/content");
		try(ClassicHttpResponse response = client.executeOpen(host, getPolicyDocument, getClientContext(host)))
		{
			assertThat(response.getHeader("Content-Type").getValue()).isEqualTo("text/plain");
			String content = new BasicHttpClientResponseHandler().handleResponse(response);
			assertThat(content).isEqualTo("link2");
		}
	}

	@Test
	public void addedPolicyDocumentEmbeddedContentIsReturned() throws Exception
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

		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/" + id.id + "/content");

		try(ClassicHttpResponse response = client.executeOpen(host, getPolicyDocument, getClientContext(host)))
		{
			assertThat(response.getHeader("Content-Type").getValue()).isEqualTo("text/html");
			String content = new BasicHttpClientResponseHandler().handleResponse(response);
			assertThat(content).isEqualTo("la la");
		}
	}

	@Test
	public void addedPolicyDocumentLinkContentIsReturned() throws Exception
	{
		HttpPost add = new HttpPost("/restadm/v1/policy-documents");
		RestPolicyDocumentRequest build = RestPolicyDocumentRequest.builder()
			.withName("Ala")
			.withMandatory(true)
			.withDisplayedName(Map.of("en", "Ola"))
			.withContentType(PolicyDocumentContentType.LINK.name())
			.withContent(Map.of("en", "link"))
			.build();
		add.setEntity(new StringEntity(mapper.writeValueAsString(build)));

		String idJson = client.execute(host, add, getClientContext(host), new BasicHttpClientResponseHandler());
		RestPolicyDocumentId id = JsonUtil.parse(idJson, RestPolicyDocumentId.class);

		HttpGet getPolicyDocument = new HttpGet("/restadm/v1/policy-documents/" + id.id + "/content");

		try(ClassicHttpResponse response = client.executeOpen(host, getPolicyDocument, getClientContext(host)))
		{
			assertThat(response.getHeader("Content-Type").getValue()).isEqualTo("text/plain");
			String content = new BasicHttpClientResponseHandler().handleResponse(response);
			assertThat(content).isEqualTo("link");
		}
	}
}
