package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.TransferUtil;

public class InputStreamDownloadHandler extends AbstractDownloadHandler<InputStreamDownloadHandler>
{
	private final Supplier<ByteArrayInputStream> data;
	private final String filename;

	public InputStreamDownloadHandler(
		Supplier<ByteArrayInputStream> data,
		String filename)
	{
		this.data = data;
		this.filename = filename;
	}

	@Override
	public void handleDownloadRequest(DownloadEvent downloadEvent) throws IOException
	{
		try (OutputStream outputStream = downloadEvent.getOutputStream(); ByteArrayInputStream inputStream = data.get())
		{
			TransferUtil.transfer(inputStream, outputStream, getTransferContext(downloadEvent), getListeners());
		} catch (IOException ioe)
		{
			downloadEvent.getResponse().setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
			notifyError(downloadEvent, ioe);
		}
	}
}
