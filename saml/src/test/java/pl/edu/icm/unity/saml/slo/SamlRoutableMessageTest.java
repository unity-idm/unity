package pl.edu.icm.unity.saml.slo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.xmlbeans.XmlObject;
import org.junit.jupiter.api.Test;

import eu.unicore.samly2.binding.SAMLMessageType;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.saml.idp.web.FreemarkerXHTMLHandler;

class SamlRoutableMessageTest
{
	@Test
	void shouldReturnPostContentsWithEscapedRelayState() throws IOException
	{
		UnityServerConfiguration serverConfig = mock(UnityServerConfiguration.class);
		when(serverConfig.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH)).thenReturn("saml/src/main/resources");
		FreemarkerXHTMLHandler handler = new FreemarkerXHTMLHandler(serverConfig);
		
		XmlObject xmlMessage = mock(XmlObject.class);
		when(xmlMessage.xmlText()).thenReturn("<SAMLRequest/>");
		
		SamlRoutableUnsignedMessage message = new SamlRoutableUnsignedMessage(
				xmlMessage, SAMLMessageType.SAMLRequest, "relay state with \" quote", "http://destination");
		
		String postContents = message.getPOSTConents(handler);
		
		assertThat(postContents).contains("name=\"RelayState\" value=\"relay state with &quot; quote\"");
		assertThat(postContents).contains("name=\"SAMLRequest\" value=\"PFNBTUxSZXF1ZXN0Lz4=\"");
		assertThat(postContents).contains("action=\"http://destination\"");
	}
}
