/**********************************************************************
 *                     Copyright (c) 2022, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.saml.metadata.cfg;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.RemoteFileData;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;

class LogoFilenameUtils
{
	static String getLogoFileBasename(TrustedIdPKey trustedIdPKey, Locale locale, String defaultLocale)
	{
		return getLogoFileBasename(trustedIdPKey, locale == null || locale.toString().isBlank() ? defaultLocale : locale.toString());
	}

	static String getLogoFileBasename(TrustedIdPKey trustedIdPKey, String localeString)
	{
		return trustedIdPKey.getSourceData()
				.map(metadata -> metadata.entityHex + metadata.index)
				.orElse(trustedIdPKey.asString()) 
			+ localeString;
	}

	static String getLogosWorkspace(UnityServerConfiguration conf)
	{
		return Path.of(conf.getValue(UnityServerConfiguration.WORKSPACE_DIRECTORY), "downloadedIdPLogos").toString();
	}

	static String federationDirName(String federationId)
	{
		return DigestUtils.md5Hex(federationId);
	}

	static String getExtensionFromDataURI(URI imageURI)
	{
		if (imageURI.getScheme().equals("data"))
		{
			String mimeType = StringUtils.substringBetween(imageURI.toString(), ":", ";");
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

	static String getExtensionForRemoteFile(RemoteFileData remoteFile)
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
			throw new IllegalStateException("Can not decode extension from URI path " + path);
		return extension;
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
