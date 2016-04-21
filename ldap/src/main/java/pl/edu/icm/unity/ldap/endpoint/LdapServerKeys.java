/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.*;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import pl.edu.icm.unity.ldaputils.LDAPAttributeTypesConverter;
import sun.security.x509.*;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Key management utilities class, taken from
 * https://github.com/cloudfoundry/uaa/blob/master/uaa/src/test/java/org/springframework/security/ldap/server/ApacheDsSSLContainer.java
 */
public class LdapServerKeys
{
    private static final int keysize = 1024;
    private static final String commonName = "unity-idm";
    private static final String organizationalUnit = "UAA";
    private static final String organization = "unity-idm";
    private static final String city = "Local";
    private static final String state = "EU";
    private static final String country = "EU";
    private static final long validity = 1096; // 3 years
    private static final String alias = "uaa-ldap";


    private static X509Certificate getSelfCertificate(X500Name x500Name, Date issueDate, long validForSeconds, KeyPair keyPair, String signatureAlgorithm)
        throws CertificateEncodingException
    {
        try {
            Date expirationDate = new Date();
            expirationDate.setTime(issueDate.getTime() + validForSeconds * 1000L);

            X509CertInfo certInfo = new X509CertInfo();
            certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
            certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber((new Random()).nextInt() & Integer.MAX_VALUE));
            certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(AlgorithmId.get(signatureAlgorithm)));

            certInfo.set(X509CertInfo.SUBJECT, x500Name);
            certInfo.set(X509CertInfo.ISSUER, x500Name);

            certInfo.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
            certInfo.set(X509CertInfo.VALIDITY, new CertificateValidity(issueDate, expirationDate));

            X509CertImpl selfSignedCert = new X509CertImpl(certInfo);
            selfSignedCert.sign(keyPair.getPrivate(), signatureAlgorithm);
            return selfSignedCert;
        } catch (Exception ioe) {
            throw new CertificateEncodingException("Error during creation of self-signed Certificate: " + ioe.getMessage());
        }
    }

    /**
     * Get the keystore (or create it).
     */
    public static File getKeystore(String keystoreFileName, String password) throws Exception
    {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        File keystore = new File(keystoreFileName);

        if (keystore.exists()) {
            return keystore;
        }
        keyStore.load(null, null);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keysize);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        X509Certificate[] chain = {
            getSelfCertificate(new X500Name(
                commonName, organizationalUnit, organization, city, state, country), new Date(), (long) validity * 24 * 60 * 60, keyPair, "SHA1WithRSA"
            )
        };
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), password.toCharArray(), chain);

        if (!keystore.createNewFile()) {
            throw new FileNotFoundException("Unable to create file:" + keystore);
        }
        keyStore.store(new FileOutputStream(keystore, false), password.toCharArray());
        return keystore;
    }

}