/*
 * Copyright (c) 2022-2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files.logo;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.RemoteFileData;

public class LogoFilenameUtils
{
	private static final String LOGOS_ROOT_DIR = "downloadedLogos";
	private static final String KEY_LOCALE_SEPARATOR = "~";

	public static String getLogoFileBasename(LogoCacheKey key, Locale locale, String defaultLocale)
	{
		return getLogoFileBasename(key, locale == null || locale.toString().isBlank() ? defaultLocale : locale.toString());
	}

	public static String getLogoFileBasename(LogoCacheKey key, String localeString)
	{
		return key.asCacheBasename() + KEY_LOCALE_SEPARATOR + localeString;
	}

	public static String getLogosWorkspaceRoot(UnityServerConfiguration conf)
	{
		return Path.of(conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY), LOGOS_ROOT_DIR).toString();
	}

	public static String namespaceDirName(String namespaceId)
	{
		return DigestUtils.md5Hex(namespaceId);
	}

	public static String getExtensionFromDataURI(URI imageURI)
	{
		if (imageURI.getScheme().equals("data"))
		{
			String mimeType = substringBetween(imageURI.toString(), ":", ";");
			if (mimeType == null)
				throw new IllegalStateException("Can not decode extension from data URI (no mime type) " + imageURI);
			String extension = MimeToExtensionTranslator.translateMimeTypeToExtension(mimeType);
			if (extension == null)
				throw new IllegalStateException("Can not decode extension from data URI " + imageURI);
			return extension;
		} else
		{
			throw new IllegalArgumentException("Argument is not a data URI: " + imageURI);
		}
	}

	public static String getExtensionForRemoteFile(RemoteFileData remoteFile)
	{
		if (remoteFile.mimeType != null)
		{
			String coreMimeType = remoteFile.mimeType.contains(";") ?
					remoteFile.mimeType.substring(0, remoteFile.mimeType.indexOf(';')) : remoteFile.mimeType;
			String extension = MimeToExtensionTranslator.translateMimeTypeToExtension(coreMimeType);
			if (extension != null)
				return extension;
		}
		String path = URI.create(remoteFile.getName()).getPath();
		path = path.endsWith("/") ? path.substring(0, path.length()-1) : path;
		String extension = FilenameUtils.getExtension(path);
		if (extension == null || extension.isBlank())
			throw new IllegalStateException("Can not decode extension from URI path " + path +", wrong mime type " + remoteFile.mimeType);
		return extension;
	}

	private static String substringBetween(String value, String open, String close)
	{
		int start = value.indexOf(open);
		if (start < 0)
			return null;
		int end = value.indexOf(close, start + open.length());
		if (end < 0)
			return null;
		return value.substring(start + open.length(), end);
	}

	static class MimeToExtensionTranslator
	{
		private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
			"image/bmp", "bmp",
			"image/gif", "gif",
			"image/png", "png",
			"image/jpeg", "jpeg",
			"image/jpg", "jpeg",
			"image/svg+xml", "svg",
			"image/x-icon", "ico",
			"image/vnd.microsoft.icon", "ico"
		);

		private static String translateMimeTypeToExtension(String mimeType)
		{
			return MIME_TO_EXTENSION.get(mimeType);
		}
	}
}
