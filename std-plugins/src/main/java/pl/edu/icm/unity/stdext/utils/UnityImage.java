/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Class providing Image related information and operations.
 * Currently supports JPG, GIF and PNG image types.
 *
 * @author R. Ledzinski
 */
public class UnityImage
{
	private static final String JSON_TYPE_NAME = "type";
	private static final String JSON_VALUE_NAME = "value";

	private byte[] image;
	private ImageType type;

	/**
	 * Enumeration representing support image types.
	 */
	public enum ImageType
	{
		JPG("image/jpeg"),
		PNG("image/png"),
		GIF("image/gif");

		private String mimeType;

		private ImageType(String mimeType)
		{
			this.mimeType = mimeType;
		}

		public String getMimeType()
		{
			return mimeType;
		}

		public static String getSupportedMimeTypes(String delimiter)
		{
			return Arrays.asList(values()).stream().
					map(ImageType::getMimeType).
					collect(Collectors.joining(delimiter));
		}

		public String toExt()
		{
			return toString().toLowerCase();
		}

		public static ImageType fromExt(String ext)
		{
			return valueOf(ext.toUpperCase());
		}

		public static ImageType fromMimeType(String mimeType)
		{
			for (ImageType type : values())
			{
				if (type.mimeType.equals(mimeType))
					return type;
			}
			throw new InternalException("Unsupported mimeType: " + mimeType);
		}
	}

	public UnityImage(String serializedObject) throws IOException
	{
		deserialize(serializedObject);
	}

	public UnityImage(byte[] rawData, ImageType type)
	{
		setImage(rawData, type);
	}

	public UnityImage(BufferedImage bufferedImage, ImageType type)
	{
 		ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
		BufferedImage bi;
		try
		{
			// Make sure BufferedImage is supported - convert if required
			bi = convertType(bufferedImage);
			/* Default ImageIO configuration should not introduce any compression or quality impact.
			Translation from BufferedImage to byte[] and back to BufferedImage gives the same same byte[] for gif and
			png formats. There are some variations for jpg - at this moment it doesn't seems to be high issue and
			current solution should fit the purpose.
			*/
			ImageIO.write(bi, type.toExt(), bos);
		} catch (IOException e)
		{
			throw new InternalException("Image can not be encoded as " + type, e);
		}

		this.image = bos.toByteArray();
		this.type = type;
	}

	public UnityImage(Path path) throws IOException
	{
		this.image = Files.readAllBytes(path);
		int dotIdx = path.toString().lastIndexOf('.');
		if (dotIdx == -1)
			throw new IllegalArgumentException("Cannot extract extension from path: '" + path.toString() + "'");
		this.type = ImageType.fromExt(path.toString().substring(dotIdx + 1));
	}

	public byte[] getImage()
	{
		return image;
	}

	/**
	 * Recreate the image to match given maximum width and height keeping current aspect ratio.
	 * If image already fits the size no action in taken.
	 *
	 * Method returns new byte array - current object is not modified.
	 *
	 * @param maxWidth
	 * @param maxHeight
	 */
	public byte[] getScaledDownImage(int maxWidth, int maxHeight)
	{
		BufferedImage bufferedImage = getBufferedImage();
		int w = bufferedImage.getWidth();
		int h = bufferedImage.getHeight();

		if (w <= maxWidth && h <= maxHeight)
			return Arrays.copyOf(image, image.length); // Scaling not needed, image smaller than limitation

		// Calculated multiplies
		double ratioW = maxWidth / (double) w;
		double ratioH = maxHeight / (double) h;
		double ratio = ratioW > ratioH ? ratioH : ratioW;
		int newWidth = new Double(w * ratio).intValue();
		int newHeight = new Double(h * ratio).intValue();

		// Redraw image
		BufferedImage resized = new BufferedImage(newWidth, newHeight, bufferedImage.getType());
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(bufferedImage, 0, 0, newWidth, newHeight, 0, 0, w, h, null);
		g.dispose();

		// Convert to raw data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(100000);
		BufferedImage bi;
		try
		{
			// Make sure BufferedImage is supported - convert if required
			bi = convertType(resized);
			/* Default ImageIO configuration should not introduce any compression or quality impact.
			Translation from BufferedImage to byte[] and back to BufferedImage gives the same same byte[] for gif and
			png formats. There are some variations for jpg - at this moment it doesn't seems to be high issue and
			current solution should fit the purpose.
			*/
			ImageIO.write(bi, type.toExt(), bos);
		} catch (IOException e)
		{
			throw new InternalException("Image can not be encoded as " + type, e);
		}
		return bos.toByteArray();
	}

	public BufferedImage getBufferedImage()
	{
		BufferedImage bi;
		ByteArrayInputStream bis = new ByteArrayInputStream(image);
		try
		{
			bi = ImageIO.read(bis);
		} catch (IOException e)
		{
			throw new InternalException("Image can not be decoded", e);
		}
		return bi;
	}

	public ImageType getType()
	{
		return type;
	}

	public int getWidth()
	{
		return getBufferedImage().getWidth();
	}

	public int getHeight()
	{
		return getBufferedImage().getHeight();
	}

	/**
	 * Updates object's fields, if given buffer contains data that may be converted to image.
	 * If not possible to convert object remains unchanged.
	 *
	 * @param image Byte array containing image data.
	 */
	public void setImage(byte[] image, ImageType type)
	{
		this.image = image;
		this.type = type;
	}

	/**
	 * OpenJDK doesn't allow to JPEG encode Buffered images of certain types. For those
	 * types this methods rewrites the source image into BufferedImage.TYPE_INT_RGB which is supported.
	 * For other cases the original image is returned.
	 *
	 * @param src
	 * @return
	 */
	public static BufferedImage convertType(BufferedImage src)
	{
		int srcType = src.getType();
		if (srcType != BufferedImage.TYPE_INT_ARGB
				&& srcType != BufferedImage.TYPE_INT_ARGB_PRE
				&& srcType != BufferedImage.TYPE_4BYTE_ABGR
				&& srcType != BufferedImage.TYPE_4BYTE_ABGR_PRE)
			return src;

		BufferedImage bi2 = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = bi2.getGraphics();
		g.drawImage(src, 0, 0, Color.WHITE, null);
		g.dispose();
		return bi2;
	}

	/**
	 * Recreate the image to match given maximum width and height keeping current aspect ratio.
	 * If image already fits the size no action in taken.
	 *
	 * @param maxWidth
	 * @param maxHeight
	 */
	public void scaleDown(int maxWidth, int maxHeight)
	{
		setImage(getScaledDownImage(maxWidth, maxHeight), type);
	}

	public String serialize()
	{
		ObjectNode node = Constants.MAPPER.createObjectNode();
		node.put(JSON_TYPE_NAME, type.toString());
		node.put(JSON_VALUE_NAME, Base64.getEncoder().encodeToString(image));

		return node.toString();
	}

	public void deserialize(String stringRepresentation) throws IOException
	{
		ObjectNode node = (ObjectNode) Constants.MAPPER.readTree(stringRepresentation);
		byte[] rawData = Base64.getDecoder().decode(node.get(JSON_VALUE_NAME).asText());
		ImageType type = ImageType.valueOf(node.get(JSON_TYPE_NAME).asText());
		setImage(rawData, type);
	}
}
