/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.files;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.IllegalURIException;
import pl.edu.icm.unity.engine.api.files.URIAccessException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.files.RemoteFileNetworkClient.ContentsWithType;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.FileDAO;
import pl.edu.icm.unity.test.utils.ExceptionsUtils;
/**
 * 
 * @author P.Piernik
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class URIAccessServiceTest
{
	@Mock
	private UnityServerConfiguration conf;
	@Mock
	private FileDAO dao;
	@Mock
	private PKIManagement pkiMan;	
	@Mock
	private RemoteFileNetworkClient networkClient;
	
	private URIAccessService uriService;
	
	@Before
	public void init()
	{
		when(conf.getBooleanValue(eq(UnityServerConfiguration.RESTRICT_FILE_SYSTEM_ACCESS))).thenReturn(false);
		when(conf.getValue(eq(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH))).thenReturn("target");
		uriService = new URIAccessServiceImpl(conf, dao, pkiMan);
	}
	
	@Test
	public void shouldSaveAndReadFile() throws EngineException
	{
		when(dao.get(any())).thenReturn(new FileData("xx", "demo".getBytes(), new Date()));
		FileData fileData = uriService.readURI(URIHelper.parseURI(URIAccessService.UNITY_FILE_URI_SCHEMA  + ":" + "uuid"), null);
		assertThat(fileData, is(notNullValue()));
		assertThat(fileData.getContents(), is("demo".getBytes()));	
	}
	
	@Test
	public void shouldThrowExceptionWhenFileNotExists()
	{
		String uri = "file:notExists.txt";
		Throwable exception = catchThrowable(() -> uriService.readURI(URIHelper.parseURI(uri), null));
		ExceptionsUtils.assertExceptionType(exception, URIAccessException.class);
	}

	@Test
	public void shouldReadBase64ImageDataUri() throws IllegalURIException, EngineException
	{
		String uri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAAA8CAIAAAB+RarbAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH3QoeCTkHBzO+ygAAAB1pVFh0Q29tbWVudAAAAAAAQ3JlYXRlZCB3aXRoIEdJTVBkLmUHAAAJ5ElEQVRo3u1aW4xdVRn+vv9f58y0M0NLW20oSMAUTA2pEHyUBLCSqCRGjDGG+IIJMfHBBMMLJvogvohP3k188MEXIokKGgMPRiQENUbDxUAkRFug9EJpO9O5nLP3+j8f1t5nLmcPnopRaGcnM3MmZ++11n///u/flISL6TJcZNeWwBf6lWLh7NIzzyiGQO39Hds+eCO9N8mTggBBFqePLz79jCVCwZmd26+/gdYD3+zZejjQkX+eevjB6oXnl158ARqmnbvSvit6l79nz+fu7l+5v9wWUVd/f6E6fjIsW63pg9f57ssggQKMkxwyYvkvT9XLA5MUse36D6TXf/rjkz94IE3NBbISL7v3/kvv+OxkyiJFEK987cvnnvid9ack+cz0Ffd/b+ZDt6zRCwhIIiiKYPXa0RM//NbZx37lFKynnhFenz45fOOUP/fc7E2H+lfuB0KwwbNPH7n3Lpxeyqilqrf36mt//QQ4csyy9pghwGIPggt/ePzoV76ExXMBsaq333IoAfKZHXnak7ZFNdDC/IS+QQgkAJxZsrlZSwkyTvdjjeoFMSAjiOIQef7M8W9+deHPT/rUNKmQORQ0qkeT3I02irW8sGD0PNs3TZlQv/YPASw/IsAuP2KRmBBAW1zpeX84M+sK5LDlbCYT5HIBDst+HvEgCEBOMAGiAQYS1JhSCIFBcumPTy489bh5T8gSCFQaKqqIijJGtt3vbp+FkmQpQg4XXZYACJQo5M2ihlI0R0P2yAaGBARAIgkSwsA6QqSHTSwtWVaVRFIQBAnr/MwaM4gCoTjz6M+ZXAyXZzHt3PGuT905ddV7e9ccqI+fIGLb+w+qdR3LRGayXlZNmcOJ1mHhQnAs6bbmbe0sE+GkhIzIppRNBgoZbgjExObl2g+iwYIq4Vq0ABY/piAWq4eGrxwRzeRSTrt3X/6N72w7cLCs07/sipHXSCItCJgQEmFQzap8BZICaZ0OLYJaDW8b6Q9w0SwIUjQTpfZkk1q5SSBGBUIlNbFogK37CRBgFIFa8+fMIEYGpq9+3/RV+9VEtxAahWARhgCBYCYsQJMDZX1t5s/iaoAX9xaVI5rcCRib79Wc7LwEZpOtA4CJJFtljn6TbByPgHx47BXBKRoIt9YDQbApNOQoF7HEPtzXm5ObFz2uX0FGyGlqTRP/c+BB/n+Bxxa03BL4QhWYF4vAksSABGYySqWZ5Aq01EGpvJ0Pas0vFXwrEWCWAERBCKO7NLqxe0s2FV6ju8c3DEijFcZrjqEpaoRcSmBMrCoQASBbNrHzlOII4JcbCMkAk7eYBEBQgBjILVTqaAkkKtXNmg2Sim6fJdmuBmmDYpKRRBA0IpP57EJ19BXlPImFDWAvcZjXlr4NVbE+dSKWlppT50rmjX1NWh4MXj3sU9NFTNLS3r3o9QSMYUaSYUrVsaMaVqKoTStcIKygDEvV6RNYz2GlLLgoKCi6nXnkwflHH54IcUiiiJQX5ulWwOTY5vHyfV+sj75GEBbK7rOzQjgg2uLzTy/fc1cCo4SDuPee++Zu/Wh0ZhSRMzOH7/508UeiFt200WcJlLCkIIOGlap67XLJaJkymEsZ0spAy4PJmofSPZgpggKdY3xgdfL1+qUXNawzCSoFsiPJw4SQqY6z8xU8UJOGleXq2MslWMYlFkLmPHNa5pIMgKzuwoVBmQA4UIsgfD3jwbp0W5kAE6LmJv457tIBOYsLeSh3FDnWomeriSBZGYs3KZTM6qiMzKhdFiBgaA43LrFMLgXNAgAdCnltSmPSBkSjZdSkWSStD/Xk4QQNxohgKOecq8lrWg5yup8ZSR5j+rbts2nHJb64vNohL52TwYhKcp/CVL8PC0Sisrnt2gMgOvogyiRZXlkUCDIDQgSGY6mUJHKAQM1s3rPe1DqBRQEKZsKVq0sOfXzu5tsQMWFlsmQnv/tAdexYZkdd8tlL9n392xoMSosaES/f/RnMzBAGVNPXHNz1+S94fxuoAJCr7QduAGTornBcWd73wI9Q100HSYzvqTbLCwHzwbN/Pf3LnymvBmkqD7pcUg1N7b927ubbzquUn/rJ95EDfUA2vv3Uges4+i/nqKMHy5KRvmNm5robY/u0g4AyyKYf7wpiKup67qZDEAo39u9TDDCvwCMPYU3NsUyIysqiewbOoztsSi+zhaOTOmgoALGEqCJIZQRMyL2gyeCgEAC84Qi6jyCpRLjYdM+beF3T55a6b7GxqlsSWDgPZhk1MfDQyJ0oRLCLNlXTrxdAB9JDQdCCYm1S8WW2HbHa9r5LdySqlo6EYFDHUQthKFAMogO0mSiBhp5CLM4yefdPCDAEKIvOwFsDOgkpgy5ZJpwWYNAARMNGFBaK3YhCqDmitAiE2A11KDJGZO3GRGpBI7KQ5QadVxehYuUsJ7xTXpJAEFGwEUmDyNoK2GImBIRplAQJdpMuAlLDEFIGazijDp8OQlZAbTstWAct1eLPgucnpnjYRgetkEwxnrRGcL01jQmUjARJqqjY2q8Jbtq5kcxtneebNXhma7pAkhvus3V9zdv7+q90sFuMx5bAWwJ30xpv68DdQPEEoxAITijOe4dgQBmiIYS8hn7RWvgD1AACFWVgadwkbvKOySotJAPqZk4wqu2xufIDKiOjAMoYrYbMMykFemZIrmaIk2W0/8C2RvYEZJopNRBCEAgVgslACkmA5STPzD0KEi3ETmzHKPgijEAYmJSEujABEprPHc8RkMmaGkmSSURtOShDZWJkesCbKW7k8/U35gBACsHaVukKSlrFQ4WFEK02KbydNdI7w0qFBSgNKxxUZl3ArKxMqrwzllTwSMtFCGEKZhloYACpHi5xZVnKhUVgf3pi8xZKh3LklXPo91OG2FceNkCywAC1KKXYfWpGy5VsECId1utnhI/JTARg6E8hQsNFiQz0Lt3TKrkwoB3YsowqRyMdo9fVsqoAatRhAKs3Xp9/5CFIAaW5HTtu/xT7U5O5cxnP1tWJNxYe+wVqZZfv3HPpRz6B6R6UARAORsiMEIKywd+eXfjT79Vzc5u7+fb+vn2bwlaC0rnHH1s5fEQ25CDPffhj/av3s1W12DEfbshFKkOELGzht78ZvnpYVAzrXZ+8czVptD1oTJq6C1EalHFtNh31su2YWCYLwgLlb1ff2pUc1MV8NBQdygi+i/oCy2w6ACKYbT2nxbfwJt6GDbW2jLTz2JKMR2N74i1ttrpF+y83z6Ubz1ZeWeDWq4dbAm8JvCXwO1XgiyJ7WfvSmmID4L9AL0YZ3fGicWkSYIx3dBfqlQaHX8LKigDS1PC0F7K5+eIdt3JlUSRlwWywC9vSKc1sG5w4BjeVBhTWvpV4gVp4C0tvCbwl8JbA76TrX/xD8nUn72SOAAAAAElFTkSuQmCC";
		FileData fdata = uriService.readURI(URIHelper.parseURI(uri), null);
		assertThat(fdata.getContents(), is(notNullValue()));
	}

	@Test
	public void shouldReadImageUrl() throws IllegalURIException, EngineException, MalformedURLException, IOException, URISyntaxException
	{
		String uri = "https://unity-idm.eu/site-wp/wp-content/uploads/2016/11/lezka.png";
		URI parsedURI = URIHelper.parseURI(uri);
		when(networkClient.download(eq(parsedURI.toURL()), any())).thenReturn(new ContentsWithType(new byte[] {}, "image/jpg"));
		uriService = new URIAccessServiceImpl(conf, dao, networkClient);
		uriService.readImageURI(parsedURI, "demo");
		verify(networkClient).download(eq(parsedURI.toURL()), any());
	}
}
