package pl.edu.icm.unity.saml.idp.web;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.saml.FreemarkerXHTMLHandler;

class FreemarkerXHTMLHandlerTest
{
	@Test
	void shouldEscapeXhtml() throws IOException
	{
		// Given
		UnityServerConfiguration serverConfig = mock(UnityServerConfiguration.class);
		when(serverConfig.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH)).thenReturn("src/test/resources");
		FreemarkerXHTMLHandler handler = new FreemarkerXHTMLHandler(serverConfig);
		StringWriter out = new StringWriter();
		Map<String, String> datamodel = Map.of("variable", "value with \" quote");

		// When
		handler.printXHTMLDocument(out, "test-template.ftl", datamodel);

		// Then
		assertThat(out.toString()).isEqualTo("Somevalue with &quot; quote");
	}
}
