package pl.edu.icm.unity.oauth.as.webauthz.externalScript.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.nimbusds.langtag.LangTag;
import com.nimbusds.langtag.LangTagException;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Prompt;
import com.nimbusds.openid.connect.sdk.claims.ACR;

public class InputRequestTest {
    @Test
    public void testFromAuthorizationRequestBasicFields() throws URISyntaxException {
        AuthorizationRequest req = new AuthorizationRequest.Builder(
                new ResponseType("code"),
                new ClientID("client123")
        )
        .scope(new Scope("openid", "profile"))
        .responseMode(ResponseMode.QUERY)
        .customParameter("custom1", "value1")
        .redirectionURI(new URI("https://redirect.uri"))
        .build();

        InputRequest input = InputRequest.fromAuthorizationRequest(req);
        assertEquals(List.of("code"), input.responseType());
        assertEquals("client123", input.clientID());
        assertEquals("https://redirect.uri", input.redirectURI());
        assertEquals("query", input.responseMode());
        assertEquals(Map.of("custom1", List.of("value1")), input.customParams());
        assertNotNull(input.scope());
        assertEquals(2, input.scope().size());
        assertEquals("openid", input.scope().get(0).value());
        assertNull(input.scope().get(0).requirement());
    }

    @Test
    public void testFromAuthorizationRequestWithAuthenticationRequest() throws LangTagException {
        AuthenticationRequest req = new AuthenticationRequest.Builder(
                new ResponseType("code"),
                new Scope("openid"),
                new ClientID("client456"),
                URI.create("https://redirect.uri"))
            .acrValues(List.of(new ACR("urn:acr:test")))
            .uiLocales(List.of(new LangTag("en")))
            .prompt(new Prompt(Prompt.Type.LOGIN))
            .build();

        InputRequest input = InputRequest.fromAuthorizationRequest(req);
        assertEquals(List.of("code"), input.responseType());
        assertEquals("client456", input.clientID());
        assertEquals("https://redirect.uri", input.redirectURI());
        assertNotNull(input.acrValues());
        assertEquals(List.of("urn:acr:test"), input.acrValues());
        assertEquals(List.of("en"), input.uiLocales());
        assertEquals(List.of("login"), input.prompt());
    }

    @Test
    public void testFromAuthorizationRequestNullFields() {
        AuthorizationRequest req = new AuthorizationRequest.Builder(
                new ResponseType("code"),
                new ClientID("client789")
        ).build();

        InputRequest input = InputRequest.fromAuthorizationRequest(req);
        assertEquals(List.of("code"), input.responseType());
        assertEquals("client789", input.clientID());
        assertNull(input.redirectURI());
        assertNull(input.responseMode());
        assertThat(input.customParams()).isEmpty();
        assertNull(input.scope());
        assertNull(input.resources());
        assertNull(input.requestURI());
        assertNull(input.prompt());
        assertNull(input.acrValues());
        assertNull(input.uiLocales());
    }
}
