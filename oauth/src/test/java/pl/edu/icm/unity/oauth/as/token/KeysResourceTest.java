package pl.edu.icm.unity.oauth.as.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import jakarta.ws.rs.core.MediaType;
import pl.edu.icm.unity.oauth.as.OAuthASProperties.RefreshTokenIssuePolicy;
import pl.edu.icm.unity.oauth.as.token.access.TokenTestBase;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;

public class KeysResourceTest extends TokenTestBase
{
	@BeforeEach
	public void init()
	{
		setupPlain(RefreshTokenIssuePolicy.ALWAYS);
	}

	@Test
	public void shouldReturnJWKSetForApplicationJson() throws Exception
	{
		HTTPResponse response = getKeys(MediaType.APPLICATION_JSON);

		assertThat(response.getStatusCode()).isEqualTo(200);
		assertThat(response.getHeaderValue("Content-Type")).startsWith(MediaType.APPLICATION_JSON);
		assertThat(JWKSet.parse(response.getBody()).getKeys()).hasSize(1);
	}

	@Test
	public void shouldReturnJWKSetForJWKSetMediaType() throws Exception
	{
		HTTPResponse response = getKeys(JWKSet.MIME_TYPE);

		assertThat(response.getStatusCode()).isEqualTo(200);
		assertThat(MediaType.valueOf(response.getHeaderValue("Content-Type"))
				.isCompatible(MediaType.valueOf(JWKSet.MIME_TYPE))).isTrue();
		assertThat(JWKSet.parse(response.getBody()).getKeys()).hasSize(1);
	}

	@Test
	public void shouldReturnNotAcceptableForUnsupportedMediaType() throws Exception
	{
		HTTPResponse response = getKeys(MediaType.APPLICATION_XML);

		assertThat(response.getStatusCode()).isEqualTo(406);
	}

	private HTTPResponse getKeys(String accept) throws Exception
	{
		HTTPRequest request = new HTTPRequest(Method.GET, new URI(getOauthUrl("/oauth/jwk")));
		request.setAccept(accept);
		HTTPRequest securedRequest = new HttpRequestConfigurer().secureRequest(request,
				pkiMan.getValidator("MAIN"), ServerHostnameCheckingMode.NONE);
		return securedRequest.send();
	}
}
