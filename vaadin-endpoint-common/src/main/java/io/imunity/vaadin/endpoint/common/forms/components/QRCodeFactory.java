/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms.components;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vaadin.flow.component.html.Image;
import pl.edu.icm.unity.base.exceptions.InternalException;

public class QRCodeFactory
{
	private final static QRCodeWriter barcodeWriter = new QRCodeWriter();

	public static Image createQRCode(String link, int size)
	{
		Image image = new Image();
		BitMatrix bitMatrix;
		try
		{
			bitMatrix = barcodeWriter.encode(link, BarcodeFormat.QR_CODE, size, size);
		} catch (WriterException e)
		{
			throw new InternalException("Link can not be encoded as QRCode", e);
		}

		image.setSrc(new SimpleImageSource(MatrixToImageWriter.toBufferedImage(bitMatrix)).getResource());
		return image;
	}
}
