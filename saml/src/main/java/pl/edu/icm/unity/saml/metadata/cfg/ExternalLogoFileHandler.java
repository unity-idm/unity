/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.metadata.cfg;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static java.util.Base64.*;

@Component
public class ExternalLogoFileHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, ExternalLogoFileHandler.class);
	private static final int EXPERIMENTAL_THREADS_NUMBER_BASED_ON_TESTS_EXPIRATION = 10;
	private final String workspaceDir;
	private final String defaultLocale;

	public ExternalLogoFileHandler(UnityServerConfiguration conf, MessageSource msg)
	{
		workspaceDir = conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY);
		defaultLocale = msg.getLocale().toString();
	}

	public void downloadLogoFiles(TrustedIdPs trustedIdPs)
	{
		CompletableFuture.runAsync(() ->
				trustedIdPs.getEntrySet().stream()
						.parallel()
						.forEach(this::downloadFiles),
				Executors.newFixedThreadPool(EXPERIMENTAL_THREADS_NUMBER_BASED_ON_TESTS_EXPIRATION)
		);
	}

	private void downloadFiles(Map.Entry<TrustedIdPKey, TrustedIdPConfiguration> entry)
	{
		entry.getValue().logoURI
				.getMap()
				.forEach((locale, uri) ->
						{
							String catalogName = getCatalogName(entry.getValue().federationId);
							//FIXME clean old catalog if exists???
							String baseFileName = getBaseFileName(entry.getKey(), new Locale(locale));
							createFile(catalogName, baseFileName, uri);
						}
				);
	}

	public File getFile(String federationId, TrustedIdPKey trustedIdPKey, Locale locale)
	{
		String catalogName = getCatalogName(federationId);
		String fileName = getBaseFileName(trustedIdPKey, locale);
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(Path.of(workspaceDir, catalogName), fileName + ".*"))
		{
			Iterator<Path> iterator = paths.iterator();
			if(iterator.hasNext())
				return iterator.next().toFile();
			return null;
		}
		catch (IOException e)
		{
			return null;
		}
	}

	private void createFile(String catalog, String name, String logoURI)
	{
		try
		{
			URI uri = URI.create(logoURI);
			if(uri.getScheme().equals("data"))
				createFileBasedOnDataURI(catalog, name, logoURI);
			else
				createFile(catalog, name, uri);
		} catch (Exception e)
		{
			log.debug("Logo file with uri {} cannot be downloaded: {}", logoURI, e.getMessage());
		}
	}

	private void createFile(String catalog, String name, URI uri) throws IOException
	{
		URLConnection urlConnection = uri.toURL().openConnection();
		ReadableByteChannel readableByteChannel = Channels.newChannel(urlConnection.getInputStream());
		String mimeType = getMimeType(urlConnection.getContentType());
		if(mimeType == null || !mimeType.startsWith("image"))
			throw new IllegalArgumentException("For uri:" + uri + " Server return mime type: " + mimeType + " instead of image type!");
		File yourFile = prepareFile(catalog, name, mimeType);
		try (FileOutputStream fileOutputStream = new FileOutputStream(yourFile, false))
		{
			fileOutputStream.getChannel()
					.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
		}
	}

	private void createFileBasedOnDataURI(String catalog, String name, String logoURI) throws IOException
	{
		int dataStartIndex = logoURI.indexOf(",") + 1;
		String data = logoURI.substring(dataStartIndex);
		byte[] decoded = getDecoder().decode(data);
		String mimeType = StringUtils.substringBetween(logoURI, ":", ";");
		File yourFile = prepareFile(catalog, name, mimeType);
		Files.write(yourFile.toPath(), decoded);
	}

	private File prepareFile(String catalog, String name, String mimeType) throws IOException
	{
		new File(workspaceDir + "/" + catalog).mkdir();
		String extension = getExtension(mimeType);
		File yourFile = new File(workspaceDir + "/" + catalog + "/" + name + "." + extension);
		yourFile.createNewFile();
		return yourFile;
	}

	private String getExtension(String mimeType)
	{
		return MimeToExtensionTranslator.translateMimeTypeToExtension(mimeType);
	}

	private String getBaseFileName(TrustedIdPKey trustedIdPKey, Locale locale)
	{
		return trustedIdPKey.getMetadata().map(metadata -> metadata.entityHex + metadata.index).orElse(trustedIdPKey.asString()) + (locale.toString().isBlank() ? defaultLocale : locale);
	}

	private static String getCatalogName(String federationId)
	{
		return DigestUtils.md5Hex(federationId);
	}

	private String getMimeType(String contentType)
	{
		return contentType == null ? null : removeParameters(contentType);
	}

	private String removeParameters(String contentType)
	{
		return contentType.split(";")[0];
	}

	static class MimeToExtensionTranslator
	{
		private static Map<String, String> mimeTypesToExtensions = Map.ofEntries(
				Map.entry("image/bmp", "bmp"),
				Map.entry("image/cis-cod", "cod"),
				Map.entry("image/gif", "gif"),
				Map.entry("image/ief", "ief"),
				Map.entry("image/png", "png"),
				Map.entry("image/jpeg", "jpeg"),
				Map.entry("image/pipeg", "jfif"),
				Map.entry("image/svg+xml", "svg"),
				Map.entry("image/tiff", "tif"),
				Map.entry("image/x-cmu-raster", "ras"),
				Map.entry("image/x-cmx", "cmx"),
				Map.entry("image/x-icon", "ico"),
				Map.entry("image/x-portable-anymap", "pnm"),
				Map.entry("image/x-portable-bitmap", "pbm"),
				Map.entry("image/x-portable-graymap", "pgm"),
				Map.entry("image/x-portable-pixmap", "ppm"),
				Map.entry("image/x-rgb", "rgb"),
				Map.entry("image/vnd.microsoft.icon", "ico"),
				Map.entry("image/x-xbitmap", "xbm"),
				Map.entry("image/x-xpixmap", "xpm"),
				Map.entry("image/x-xwindowdump", "xwd")
		);

		private static String translateMimeTypeToExtension(String mimeType)
		{
			return mimeTypesToExtensions.get(mimeType);
		}
	}
}
