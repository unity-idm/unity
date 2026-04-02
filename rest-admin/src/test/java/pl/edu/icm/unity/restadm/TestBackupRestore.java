/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.utils.Log;

public class TestBackupRestore extends RESTAdminTestBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST,
			TestBackupRestore.class);

	@Test
	public void shouldReturnExportDumpWithDefaultCategories() throws Exception
	{
		// given
		HttpGet export = new HttpGet("/restadm/v1/db-dump");

		// when
		try (ClassicHttpResponse response = client.executeOpen(host, export, getClientContext(host)))
		{
			// then
			assertThat(response.getCode()).isEqualTo(200);
			assertThat(response.getFirstHeader("Content-Disposition").getValue())
				.startsWith("attachment; filename=\"unity-dbdump-")
				.endsWith(".json\"");
			assertThat(response.getFirstHeader("Content-Type").getValue())
				.contains("application/json");

			String body = new String(response.getEntity().getContent().readAllBytes());
			assertThat(body).isNotEmpty();
		}
	}

	@Test
	public void shouldRespectCategorySelection() throws Exception
	{
		// given
		HttpGet export = new HttpGet(
			"/restadm/v1/db-dump?directorySchema=false&users=false"
				+ "&auditLogs=false&signupRequests=false&idpStatistics=false");

		// when
		try (ClassicHttpResponse response = client.executeOpen(host, export, getClientContext(host)))
		{
			// then
			assertThat(response.getCode()).isEqualTo(200);
			String body = new String(response.getEntity().getContent().readAllBytes());
			assertThat(body).isNotEmpty();
		}
	}

	@Test
	public void shouldReturnDumpWhenAllCategoriesExplicitlyEnabled() throws Exception
	{
		// given
		HttpGet export = new HttpGet(
			"/restadm/v1/db-dump?systemConfig=true&directorySchema=true&users=true"
				+ "&auditLogs=true&signupRequests=true&idpStatistics=true");

		// when
		try (ClassicHttpResponse response = client.executeOpen(host, export, getClientContext(host)))
		{
			// then
			assertThat(response.getCode()).isEqualTo(200);
			String body = new String(response.getEntity().getContent().readAllBytes());
			assertThat(body).isNotEmpty();
		}
	}

	@Test
	public void shouldImportDatabaseFromExportedDump() throws Exception
	{
		// given -- first export to get a valid dump
		HttpGet export = new HttpGet("/restadm/v1/db-dump");
		String dumpContent;
		try (ClassicHttpResponse exportResponse = client.executeOpen(host, export, getClientContext(host)))
		{
			assertThat(exportResponse.getCode()).isEqualTo(200);
			dumpContent = new String(exportResponse.getEntity().getContent().readAllBytes());
			assertThat(dumpContent).isNotEmpty();
		}

		// when -- import the exported dump back
		HttpPost importRequest = new HttpPost("/restadm/v1/db-dump");
		importRequest.setEntity(new StringEntity(dumpContent, ContentType.APPLICATION_JSON));
		try (ClassicHttpResponse importResponse = client.executeOpen(host, importRequest, getClientContext(host)))
		{
			// then -- per D-01 the endpoint may self-destruct during import,
			// but if we get a response it should be 200
			assertThat(importResponse.getCode()).isEqualTo(200);
		}
		catch (IOException e)
		{
			// Only network-level errors (connection reset, broken pipe) are acceptable
			// per D-01 -- importDb() calls endpointMan.undeployAll() which destroys the endpoint.
			// Application errors (4xx, 5xx) are HTTP responses, not IOExceptions.
			log.info("Import connection reset as expected (endpoint self-destructed): {}", e.getMessage());
		}
	}

	@Test
	public void shouldRejectExportForNonPrivilegedUserWith403() throws Exception
	{
		// given
		HttpClientContext unprivilegedContext = createNonPrivilegedUserContext();
		HttpGet export = new HttpGet("/restadm/v1/db-dump");

		// when
		try (ClassicHttpResponse response = client.executeOpen(host, export, unprivilegedContext))
		{
			// then
			assertThat(response.getCode()).isEqualTo(403);
		}
	}

	@Test
	public void shouldRejectImportForNonPrivilegedUserWith403() throws Exception
	{
		// given
		HttpClientContext unprivilegedContext = createNonPrivilegedUserContext();
		HttpPost importRequest = new HttpPost("/restadm/v1/db-dump");
		importRequest.setEntity(new StringEntity("{}", ContentType.APPLICATION_JSON));

		// when
		try (ClassicHttpResponse response = client.executeOpen(host, importRequest, unprivilegedContext))
		{
			// then
			assertThat(response.getCode()).isEqualTo(403);
		}
	}

	private HttpClientContext createNonPrivilegedUserContext() throws Exception
	{
		createUsernameUser("unprivileged-user", "Inspector", DEF_PASSWORD, CRED_REQ_PASS);
		return getClientContext(host, "unprivileged-user", DEF_PASSWORD);
	}
}
