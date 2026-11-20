package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.group.Group;

class AdminFormHttpContractTest extends AdminHttpContractTestBase
{
	@Test
	void shouldPreservePopulatedRegistrationAndEnquiryFormLifecycles() throws Exception
	{
		groupsMan.addGroup(new Group("/contract"));
		for (String kind : new String[] {"registration", "enquiry"})
		{
			String path = "/restadm/v1/" + kind + "Form";
			String create = fixture(kind + "-form-create.json");
			ObjectNode expected = (ObjectNode) m.readTree(create);
			if (kind.equals("registration"))
			{
				ObjectNode information = expected.putObject("FormInformation2ndStage");
				information.putNull("DefaultValue");
				information.putObject("Map");
			}

			assertEmpty(exchange("POST", path, create, ContentType.APPLICATION_JSON), 204);
			assertJson(exchange("GET", path + "s"), 200, m.createArrayNode().add(expected));
			expected.put("Description", "Updated ☃");
			assertEmpty(exchange("PUT", path + "?ignoreRequestsAndInvitations=true&ignoreInvitations=true",
					expected.toString(), ContentType.APPLICATION_JSON), 204);
			assertJson(exchange("GET", path + "s"), 200, m.createArrayNode().add(expected));
			assertEmpty(exchange("DELETE", path + "/contract-form?dropRequests=true"), 204);
			assertJson(exchange("GET", path + "s"), 200, "[]");
		}
	}

	@Test
	void shouldPreserveFormParseErrorsAndJsonOnlyMedia() throws Exception
	{
		for (String kind : new String[] {"registration", "enquiry"})
		{
			String path = "/restadm/v1/" + kind + "Form";
			for (String invalid : new String[] {"{", "", "{\"unknown\":true}"})
				assertJson(exchange("POST", path, invalid, ContentType.APPLICATION_JSON), 400, """
						{"message":"Can't perform JSON argument deserialization","error":"IllegalArgumentException"}
						""");
			assertEmpty(exchange("POST", path, "null", ContentType.APPLICATION_JSON), 500);
			assertEmpty(exchange("POST", path, "{}", ContentType.TEXT_PLAIN), 415);
			assertJson(exchange("GET", path + "s"), 200, "[]");
		}
		assertJson(exchange("GET", "/restadm/v1/registrationRequests"), 200, "[]");
		assertJson(exchange("GET", "/restadm/v1/enquiryResponses"), 200, "[]");
		assertThat(exchange("GET", "/restadm/v1/registrationRequest/absent").status()).isEqualTo(400);
		assertThat(exchange("GET", "/restadm/v1/enquiryResponse/absent").status()).isEqualTo(400);
	}
}
