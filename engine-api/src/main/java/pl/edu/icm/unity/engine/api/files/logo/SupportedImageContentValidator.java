/*
 * Copyright (c) 2018-2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files.logo;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Sniffs raw bytes of a downloaded/decoded logo against the magic signature expected for the file's
 * (mime-type or URL-derived) extension, so that a remote source can not have arbitrary content cached
 * and later served under an image extension/content-type just by declaring a matching Content-Type or
 * file suffix.
 */
class SupportedImageContentValidator
{
	private static final byte[] PNG_SIGNATURE =
			{ (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
	private static final byte[] BMP_SIGNATURE = { 'B', 'M' };
	private static final byte[] ICO_SIGNATURE = { 0x00, 0x00, 0x01, 0x00 };
	private static final byte[] JPEG_SIGNATURE = { (byte) 0xFF, (byte) 0xD8 };
	private static final byte[] GIF87_SIGNATURE = "GIF87a".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] GIF89_SIGNATURE = "GIF89a".getBytes(StandardCharsets.US_ASCII);

	static boolean isSupportedImage(byte[] content, String extension)
	{
		if (content == null || content.length == 0 || extension == null)
			return false;
		switch (extension.toLowerCase(Locale.ROOT))
		{
		case "png":
			return startsWith(content, PNG_SIGNATURE);
		case "bmp":
			return startsWith(content, BMP_SIGNATURE);
		case "ico":
			return startsWith(content, ICO_SIGNATURE);
		case "jpeg":
		case "jpg":
			return startsWith(content, JPEG_SIGNATURE);
		case "gif":
			return startsWith(content, GIF87_SIGNATURE) || startsWith(content, GIF89_SIGNATURE);
		case "svg":
			return looksLikeSvg(content);
		default:
			return false;
		}
	}

	private static boolean looksLikeSvg(byte[] content)
	{
		int probeLength = Math.min(content.length, 512);
		String probe = new String(content, 0, probeLength, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
		return probe.contains("<svg");
	}

	private static boolean startsWith(byte[] content, byte[] signature)
	{
		if (content.length < signature.length)
			return false;
		for (int i = 0; i < signature.length; i++)
			if (content[i] != signature[i])
				return false;
		return true;
	}

	private SupportedImageContentValidator()
	{
	}
}
