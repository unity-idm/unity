package pl.edu.icm.unity.saml.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.jetty.http.HttpTester;
import org.junit.jupiter.api.Test;

import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.ServletTestServer;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.saml.sp.FakeSAMLSPConfiguration;
import pl.edu.icm.unity.saml.sp.SLOSPManager;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorDocument;
import xmlbeans.org.oasis.saml2.metadata.SPSSODescriptorType;

class LocalSPMetadataManagerTest
{
	@Test
	void shouldAdvertiseOnlyPostForAuthenticationAndPreserveRedirectLogout() throws Exception
	{
		String consumerAddress = "https://unity.example/unitygw/spSAMLResponseConsumer";
		String logoutAddress = "https://unity.example/logout";
		String logoutReplyAddress = "https://unity.example/unitygw/SAMLSLOResponseConsumer";
		MultiMetadataServlet metadataServlet = new MultiMetadataServlet("/saml-sp-metadata");
		SLOSPManager sloManager = mock(SLOSPManager.class);
		when(sloManager.getAsyncServletURL("slo")).thenReturn(logoutAddress);
		when(sloManager.getSyncServletURL("slo")).thenReturn("https://unity.example/logout-soap");
		SLOReplyInstaller replyInstaller = mock(SLOReplyInstaller.class);
		when(replyInstaller.getServletURL()).thenReturn(logoutReplyAddress);
		LocalSPMetadataManager manager = new LocalSPMetadataManager(mock(ExecutorsService.class),
				consumerAddress, sloManager, replyInstaller, metadataServlet, mock(URIAccessService.class));
		SAMLSPConfiguration configuration = FakeSAMLSPConfiguration.getFakeBuilder()
				.withRequesterCredential(null)
				.withPublishMetadata(true)
				.withSloPath("slo")
				.build();
		manager.updateConfiguration(configuration);

		try (ServletTestServer server = new ServletTestServer(metadataServlet, "/saml-sp-metadata/*"))
		{
			HttpTester.Response response = server.get("/saml-sp-metadata/meta");

			assertThat(response.getStatus()).isEqualTo(200);
			assertThat(response.get("Content-Type")).startsWith("application/samlmetadata+xml");
			SPSSODescriptorType descriptor = EntityDescriptorDocument.Factory.parse(response.getContent())
					.getEntityDescriptor().getSPSSODescriptorArray(0);
			assertThat(descriptor.getAssertionConsumerServiceArray()).singleElement().satisfies(endpoint ->
			{
				assertThat(endpoint.getBinding()).isEqualTo(SAMLConstants.BINDING_HTTP_POST);
				assertThat(endpoint.getLocation()).isEqualTo(consumerAddress);
				assertThat(endpoint.getIsDefault()).isTrue();
			});
			assertThat(descriptor.getSingleLogoutServiceArray()).extracting(EndpointType::getBinding)
					.containsExactlyInAnyOrder(SAMLConstants.BINDING_HTTP_POST, SAMLConstants.BINDING_HTTP_REDIRECT,
							SAMLConstants.BINDING_SOAP);
			assertThat(descriptor.getSingleLogoutServiceArray()).filteredOn(endpoint ->
					SAMLConstants.BINDING_HTTP_REDIRECT.equals(endpoint.getBinding())).singleElement()
					.satisfies(endpoint ->
					{
						assertThat(endpoint.getLocation()).isEqualTo(logoutAddress);
						assertThat(endpoint.getResponseLocation()).isEqualTo(logoutReplyAddress);
					});
		}
	}
}
