package io.imunity.upman.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

/** JSON transport registered only by the UpMan application. */
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
final class UpmanJsonProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object>
{
	private final ObjectMapper mapper = newMapper();

	static ObjectMapper newMapper()
	{
		return new ObjectMapper().findAndRegisterModules()
				.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.NON_PRIVATE);
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return type.getPackageName().equals(UpmanJsonProvider.class.getPackageName())
				|| type.getPackageName().startsWith("io.imunity.rest.api.types");
	}

	@Override
	public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, String> headers, InputStream input) throws IOException
	{
		return read(input, genericType, mediaType);
	}

	Object read(InputStream input, Type type, MediaType mediaType) throws IOException
	{
		String charset = mediaType.getParameters().get("charset");
		String json = new String(input.readAllBytes(),
				charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset));
		return parse(json, type);
	}

	private Object parse(String json, Type type)
	{
		try
		{
			return mapper.readValue(json, mapper.constructType(type));
		} catch (Exception e)
		{
			if (e.getCause() instanceof BadRequestException badRequest)
				throw badRequest;
			throw new BadRequestException("Can't perform JSON deserialization", e);
		}
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		// Unity's exception mappers already supply serialized JSON strings.
		return type != String.class && type != byte[].class && !InputStream.class.isAssignableFrom(type)
				&& mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
	}

	@Override
	public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> headers, OutputStream output) throws IOException
	{
		mapper.writeValue(output, value);
	}
}
