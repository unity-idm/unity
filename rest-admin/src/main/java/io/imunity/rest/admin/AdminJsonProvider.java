package io.imunity.rest.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;

/** Admin transport retains Constants.MAPPER visibility and each resource family's parse failures. */
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
public final class AdminJsonProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object>
{
	private final ObjectMapper mapper = newMapper();

	static ObjectMapper newMapper()
	{
		return Constants.MAPPER.copy();
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return type.getPackageName().startsWith("io.imunity.rest.api")
				|| Arrays.stream(annotations).anyMatch(AdminJsonBody.class::isInstance);
	}

	@Override
	public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> headers, InputStream input) throws IOException
	{
		AdminJsonBody.Failure failure = Arrays.stream(annotations).filter(AdminJsonBody.class::isInstance)
				.map(AdminJsonBody.class::cast).map(AdminJsonBody::value).findFirst().orElse(null);
		try
		{
			return mapper.readValue(readText(input, mediaType), mapper.constructType(genericType));
		} catch (Exception e)
		{
			if (failure == AdminJsonBody.Failure.POLICY)
				throw new BadRequestException("Can't perform JSON deserialization", e);
			if (failure == AdminJsonBody.Failure.FORM)
				throw new IllegalArgumentException("Can't perform JSON argument deserialization");
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}

	public static String readText(InputStream input, MediaType mediaType) throws IOException
	{
		String charset = mediaType == null ? null : mediaType.getParameters().get("charset");
		return new String(input.readAllBytes(),
				charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset));
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return type != String.class && type != byte[].class && !InputStream.class.isAssignableFrom(type)
				&& !StreamingOutput.class.isAssignableFrom(type)
				&& mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
	}

	@Override
	public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> headers, OutputStream output) throws IOException
	{
		mapper.writeValue(output, value);
	}
}
