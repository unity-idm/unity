package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;

abstract class AdminHttpContractTestBase extends RESTAdminTestBase
{
	{
		m.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
	}

	protected String fixture(String name) throws Exception
	{
		try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("/io/imunity/rest/admin/contract/" + name)))
		{
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	protected HttpResult exchange(String method, String path) throws Exception
	{
		return exchange(method, path, null, null);
	}

	protected HttpResult exchange(String method, String path, String body, ContentType contentType) throws Exception
	{
		HttpUriRequestBase request = new HttpUriRequestBase(method, URI.create(path));
		if (contentType != null)
			request.setHeader("Content-Type", contentType.toString());
		if (body != null)
			request.setEntity(new StringEntity(body, contentType));
		try (var response = client.executeOpen(host, request, getClientContext(host)))
		{
			return new HttpResult(response.getCode(), response.getEntity() == null ? null
					: response.getEntity().getContentType(), response.getEntity() == null ? ""
					: EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
		}
	}

	protected void assertJson(HttpResult actual, int status, String expected) throws Exception
	{
		assertJson(actual, status, m.readTree(expected));
	}

	protected void assertJson(HttpResult actual, int status, JsonNode expected) throws Exception
	{
		assertThat(actual.status()).as(actual.body()).isEqualTo(status);
		assertThat(actual.contentType()).startsWith("application/json");
		assertThat(m.readTree(actual.body())).isEqualTo(expected);
	}

	protected void assertEmpty(HttpResult actual, int status)
	{
		assertThat(actual.status()).as(actual.body()).isEqualTo(status);
		assertThat(actual.body()).isEmpty();
	}

	protected record HttpResult(int status, String contentType, String body)
	{
	}

}
