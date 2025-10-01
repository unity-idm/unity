package pl.edu.icm.unity.store.migration.to4_1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.store.export.JsonDumpUpdate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class JsonDumpUpdateFromV20 implements JsonDumpUpdate
{
	private final ObjectMapper objectMapper;

	public JsonDumpUpdateFromV20(@Autowired ObjectMapper objectMapper)
	{
		this.objectMapper = objectMapper;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 20;
	}

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ArrayNode authenticators = root.withObject("contents").withArray("authenticator");

		for (JsonNode entry : authenticators)
		{
			ObjectNode authenticator = (ObjectNode) entry.get("obj");
			UpdateHelperTo4_1.removeLocalOAuthCredential(authenticator);
		}

		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}
}
