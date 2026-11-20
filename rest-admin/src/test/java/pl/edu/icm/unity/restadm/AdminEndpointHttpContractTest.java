package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.i18n.I18nString;

class AdminEndpointHttpContractTest extends AdminTransportHttpTestBase
{
	@Test
	void shouldPreserveCompleteEndpointDeploymentAndListingTrees() throws Exception
	{
		var configuration = new EndpointConfiguration(new I18nString("Zażółć ☃"), "Example endpoint",
				List.of("password"), "example=true", "realm");
		var endpoint = new Endpoint("example", "RESTAdmin", "/example", configuration, 7);
		var realm = new AuthenticationRealm("realm", "Example realm", 10, 100, RememberMePolicy.disallow, 1, 600);
		var type = new EndpointTypeDescription("RESTAdmin", "Admin REST", "REST", Map.of("v1", "/v1"));
		var resolved = new ResolvedEndpoint(endpoint, realm, type);
		when(endpointManagement.deploy(eq("RESTAdmin"), eq("example"), eq("/example"), any())).thenReturn(resolved);
		when(endpointManagement.getDeployedEndpoints()).thenReturn(List.of(resolved));
		String expected = fixture("endpoint.json");

		var deployed = exchange("POST", "/v1/endpoint/example?typeId=RESTAdmin&address=/example", """
				{"displayedName":{"DefaultValue":"Zażółć ☃","Map":{}},"description":"Example endpoint",
				 "authenticationOptions":["password"],"configuration":"example=true","realm":"realm"}
				""", "application/json");

		assertThat(deployed.statusCode()).as(deployed.body()).isEqualTo(200);
		assertThat(deployed.headers().firstValue("content-type").orElse("")).startsWith("application/json");
		assertThat(json.readTree(deployed.body())).isEqualTo(json.readTree(expected));
		assertJson("/v1/endpoints", "[" + expected + "]");
	}
}
