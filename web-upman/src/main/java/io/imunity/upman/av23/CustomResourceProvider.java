package io.imunity.upman.av23;

import com.vaadin.flow.di.ResourceProvider;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.stream;

public abstract class CustomResourceProvider implements ResourceProvider {
	private final Map<String, CachedStreamData> cache = new ConcurrentHashMap<>();
	private final Set<String> chosenClassPathElement;

	public CustomResourceProvider(String... chosenModules) throws URISyntaxException {
		String currentClassPathElement = getClass()
			.getProtectionDomain()
			.getCodeSource()
			.getLocation()
			.toURI()
			.toString();

		Set<String> classPathElements = stream(System.getProperty("java.class.path").split(File.pathSeparator))
			.filter(classPathElement -> stream(chosenModules).anyMatch(classPathElement::contains))
			.map(path -> new File(path).toURI().toString())
			.collect(Collectors.toSet());

		classPathElements.add(currentClassPathElement);
		this.chosenClassPathElement = classPathElements;
	}

	public Set<String> getChosenClassPathElement() {
		return chosenClassPathElement;
	}

	@Override
	public URL getApplicationResource(String path) {
		return getApplicationResources(path).stream().findAny().orElse(null);
	}

	@Override
	public List<URL> getApplicationResources(String path) {
		Iterable<URL> iterable = getUrls(path);
		return StreamSupport.stream(iterable.spliterator(), false)
			.filter(url -> chosenClassPathElement.stream().anyMatch(classPathElement -> url.toString().startsWith(classPathElement)))
			.collect(Collectors.toList());
	}

	private Iterable<URL> getUrls(String path) {
		Iterator<URL> urlIterator = getUrlIterator(path);
		return () -> urlIterator;
	}

	private Iterator<URL> getUrlIterator(String path) {
		Iterator<URL> urlIterator;
		try {
			urlIterator = getClass().getClassLoader().getResources(path).asIterator();
		} catch (IOException e) {
			urlIterator = Collections.emptyIterator();
		}
		return urlIterator;
	}

	@Override
	public URL getClientResource(String path) {
		return this.getApplicationResource(path);
	}

	@Override
	public InputStream getClientResourceAsStream(String path) throws IOException {
		CachedStreamData cached = this.cache.computeIfAbsent(path, this::loadResourceStreamAsCachedData);

		IOException exception = cached.exception;
		if (exception == null) {
			return new ByteArrayInputStream(cached.data);
		} else {
			throw exception;
		}
	}

	private CachedStreamData loadResourceStreamAsCachedData(String key) {
		URL url = this.getClientResource(key);
		try(InputStream stream = url.openStream()) {
			CachedStreamData cachedStreamData;
			try {
				ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
				IOUtils.copy(stream, tempBuffer);
				cachedStreamData = new CachedStreamData(tempBuffer.toByteArray(), null);
			} catch (Throwable throwable) {
				if (stream != null) {
					try {
						stream.close();
					} catch (Throwable suppressedThrowable) {
						throwable.addSuppressed(suppressedThrowable);
					}
				}

				throw throwable;
			}

			stream.close();

			return cachedStreamData;
		} catch (IOException ioException) {
			return new CachedStreamData(null, ioException);
		}
	}

	private static class CachedStreamData {
		private final byte[] data;
		private final IOException exception;

		private CachedStreamData(byte[] data, IOException exception) {
			this.data = data;
			this.exception = exception;
		}
	}
}
