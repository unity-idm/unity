package pl.edu.icm.unity.saml;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;

import jakarta.servlet.Servlet;

public class ServletTestServer implements AutoCloseable
{
	private final Server server = new Server();
	private final LocalConnector connector = new LocalConnector(server);

	public ServletTestServer(Servlet servlet, String pathSpec) throws Exception
	{
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		context.addServlet(new ServletHolder(servlet), pathSpec);
		server.addConnector(connector);
		server.setHandler(context);
		server.start();
	}

	public HttpTester.Response get(String target) throws Exception
	{
		return send("GET", target, null);
	}

	public HttpTester.Response post(String target, String form) throws Exception
	{
		return send("POST", target, form);
	}

	private HttpTester.Response send(String method, String target, String form) throws Exception
	{
		HttpTester.Request request = HttpTester.newRequest();
		request.setMethod(method);
		request.setURI(target);
		request.put("Host", "localhost");
		request.put("Connection", "close");
		if (form != null)
		{
			request.put("Content-Type", "application/x-www-form-urlencoded");
			request.setContent(form);
		}
		return HttpTester.parseResponse(connector.getResponse(request.generate()));
	}

	@Override
	public void close() throws Exception
	{
		server.stop();
	}
}
