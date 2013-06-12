package pl.edu.icm.unity.ws;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.CertificateUtils;
import eu.emi.security.authn.x509.impl.FormatMode;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator;
import eu.unicore.security.canl.LoggingX509TrustManager;
import eu.unicore.util.Log;
import eu.unicore.util.httpclient.HostnameMismatchCallbackImpl;
import eu.unicore.util.httpclient.IClientConfiguration;
import eu.unicore.util.httpclient.NoAuthKeyManager;


/**
 * Some (small) parts of this class remains from the code from Commons 
 * HTTPClient "contrib" section, by Oleg Kalnichevski<br/>
 * 
 * 
 * <p>
 * MySSLSocketFactory validates the identity of the HTTPS server 
 * using a provided {@link X509CertChainValidator}, can present {@link X509Credential} 
 * to authenticate the client and install a standard 
 * </p>
 * 
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 * @author K. Benedyczak
 * </p>
 */

public class MySSLSocketFactory extends SSLSocketFactory
{
	private static final Logger log = Log.getLogger(Log.SECURITY,
			MySSLSocketFactory.class);

	private SSLContext sslcontext = null;
	private IClientConfiguration sec;

	public MySSLSocketFactory(IClientConfiguration sec)
	{
		this.sec = sec;
	}

	private synchronized SSLContext createSSLContext()
	{
		try
		{
			KeyManager km;
			if (sec.doSSLAuthn())
			{
				km = sec.getCredential().getKeyManager();
				if (log.isTraceEnabled())
					debugKS(sec.getCredential());
			} else
			{
				km = new NoAuthKeyManager();
				log.trace("Not authenticating client");
			}
			
			X509TrustManager tm = SocketFactoryCreator.getSSLTrustManager(sec.getValidator());
			tm = new LoggingX509TrustManager(tm,"ssl");
			if (log.isTraceEnabled())
				debugTS(sec.getValidator());
			
			SSLContext sslcontext = SSLContext.getInstance("TLS");
			sslcontext.init(new KeyManager[] {km}, new TrustManager[] {tm}, null);
			
			return sslcontext;
		} catch (Exception e)
		{
			log.fatal(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	private void debugTS(X509CertChainValidator validator)
	{
		X509Certificate trustedCerts[] = validator.getTrustedIssuers();
		for (X509Certificate cert: trustedCerts)
		{
			log.trace("Currently(!) trusted certificate:\n" + 
					CertificateUtils.format(cert, FormatMode.FULL));
		}
	}
	
	private void debugKS(X509Credential c)
	{
		X509Certificate[] certs = c.getCertificateChain();
		X509Certificate[] certs509 = CertificateUtils.convertToX509Chain(certs);
		log.trace("Client's certificate chain:" + 
				CertificateUtils.format(certs509, FormatMode.FULL));
	}	
	
	private SSLContext getSSLContext()
	{
		if (this.sslcontext == null)
		{
			this.sslcontext = createSSLContext();
		}
		return this.sslcontext;
	}

	private void checkHostname(SSLSocket socket) throws IOException
	{
		HostnameMismatchCallbackImpl callback = new HostnameMismatchCallbackImpl(
				sec.getServerHostnameCheckingMode());
		SocketFactoryCreator.connectWithHostnameChecking(socket, callback);
	}
	
	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
	 */
	public Socket createSocket(String host, int port,
			InetAddress clientHost, int clientPort)
			throws IOException, UnknownHostException
	{
		Socket socket = getSSLContext().getSocketFactory().createSocket(host, 
				port, clientHost, clientPort);
		checkHostname((SSLSocket) socket);
		return socket;
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
	 */
	public Socket createSocket(String host, int port) throws IOException,
			UnknownHostException
	{
		Socket socket = getSSLContext().getSocketFactory().createSocket(host,
				port);
		checkHostname((SSLSocket) socket);
		return socket;
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
	 */
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException,
			UnknownHostException
	{
		Socket socket2 = getSSLContext().getSocketFactory().createSocket(socket,
				host, port, autoClose);
		checkHostname((SSLSocket) socket2);
		return socket2;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return createSSLContext().getSupportedSSLParameters().getCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return createSSLContext().getSupportedSSLParameters().getCipherSuites();
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		Socket socket = getSSLContext().getSocketFactory().createSocket(host,port);
		checkHostname((SSLSocket) socket);
		return socket;
	}

	@Override
	public Socket createSocket(InetAddress address, int port,
			InetAddress localAddress, int localPort) throws IOException {
		Socket socket = getSSLContext().getSocketFactory().createSocket(address, port, localAddress, localPort);
		checkHostname((SSLSocket) socket);
		return socket;
	}
}
