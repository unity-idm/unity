/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.ldap.model.entry.*;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Note: default settings contain one system user with
 * "cn=system administrator".
 */
public class LdapServerFacade
{
    private LdapServer impl;

    private DirectoryService ds;

    private String host;

    private int port;

    private String name;

    private String workdir;

    public String partitionResource = "partitions.zip";

    public LdapServerFacade(String host, int port, String nameOrNull, String workdir)
    {
        this.host = host;
        this.port = port;
        this.name = nameOrNull;
        this.workdir = workdir;

        this.impl = null;
        this.ds = null;
    }

    public DirectoryService getDs()
    {
        return ds;
    }

    public Attribute getAttribute(String uid, String upId) throws LdapException
    {
        AttributeType at = ds.getSchemaManager().lookupAttributeTypeRegistry(
                null == upId ? uid : upId);
        Attribute da = new DefaultAttribute(uid, at);
        return da;
    }

    /**
     * Initialise the LDAP server and the directory service.
     *
     * @param deleteWorkDir - should we reuse the LDAP initialisation data
     * @param interceptor - unity's LDAP interceptor
     */
    public void init(boolean deleteWorkDir, BaseInterceptor interceptor) throws Exception
    {
        impl = new LdapServer();
        impl.setServiceName(name);
        impl.setTransports(new TcpTransport(host, port));

        ds = new DefaultDirectoryService();
        ds.getChangeLog().setEnabled(false);
        // ?
        ds.setDenormalizeOpAttrsEnabled(true);

        // prepare the working dir with all the required settings
        boolean shouldStartClose = prepareWorkDir(deleteWorkDir);

        // load the required data
        loadData();

        // see https://issues.apache.org/jira/browse/DIRSERVER-1954
        if (shouldStartClose)
        {
            ds.startup();
            ds.shutdown();
        }
        impl.setDirectoryService(ds);

        // "inject" unity's code
        setUnityInterceptor(interceptor);
    }


    /**
     * Enable TLS support
     */
    public void initTLS(String keystoreFileName, String password, boolean forceTls)
    {
        // TLS support
        try {
            impl.setKeystoreFile(LdapServerKeys.getKeystore(keystoreFileName, password).getAbsolutePath());
            impl.setCertificatePassword(password);
            StartTlsHandler handler = new StartTlsHandler();
            impl.addExtendedOperationHandler(handler);
            // force TLS on every connection
            impl.setConfidentialityRequired(forceTls);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getAdminDN()
    {
        return ServerDNConstants.ADMIN_SYSTEM_DN;
    }

    /**
     * Apache DS has to be initialised with schemas etc. - this directory
     * will be used to hold all the required configuration which is then
     * loaded
     *
     * @return True if the work directory has been cleaned
     */
    private boolean prepareWorkDir(boolean delete_work_dir) throws IOException
    {
        boolean fromScratch = false;
        File workdirF = new File(workdir);
        if (delete_work_dir && workdirF.exists())
        {
            FileUtils.deleteDirectory(workdirF);
        }
        boolean shouldExtract = !workdirF.exists();
        ds.setInstanceLayout(new InstanceLayout(workdirF));

        if (shouldExtract)
        {
            // indicate that we start for the first time
            fromScratch = true;

            // copy resources (kind-of minimalistic version from apacheds) to the destination directory
            InputStream jarZipIs = LdapServerFacade.class.getClassLoader().getResourceAsStream(
                partitionResource);
            ZipInputStream zis = new ZipInputStream(jarZipIs);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
            {
                File entryDestination = new File(workdirF,  entry.getName());
                if (entry.isDirectory())
                {
                    entryDestination.mkdirs();
                } else
                {
                    entryDestination.getParentFile().mkdirs();
                    OutputStream out = new FileOutputStream(entryDestination);
                    IOUtils.copy(zis, out);
                    out.close();
                }
            }
            zis.close();
            
            loadExtraMAttributeTypes(workdir);
        }

        return fromScratch;
    }
    
    /**
     * TODO: make this so it copies in ldiff files if they exist in some configured
     *location
     */
    protected void loadExtraMAttributeTypes(String workdir) {
        ///opt/unity-server/data/workspace/ldapServer/partitions/schema/ou=schema/cn=core/ou=attributetypes
        File ldiffFileMemberof = new File(workdir+"/ldapServer/partitions/schema/ou=schema/cn=core/ou=attributetypes/m-oid=2.16.840.1.113894.1.1.424.ldif");
        
        System.out.println("Adding extra attribute types");
        System.out.println("LdiffFile = "+ldiffFileMemberof.toString());
        
        PrintWriter pw = null;
        try {
            pw= new PrintWriter(new FileWriter(ldiffFileMemberof));
            pw.println("version: 1");
            pw.println("dn: m-oid=2.16.840.1.113894.1.1.424,ou=attributeTypes,cn=core,ou=schema");
            pw.println("m-oid: 2.16.840.1.113894.1.1.424");
            pw.println("m-name: memberof");
            pw.println("m-description: Member of group");
            pw.println("m-syntax: 1.3.6.1.4.1.1466.115.121.1.15");
            pw.println("m-usage: USER_APPLICATIONS");
            pw.println("m-supattributetype: name");
            pw.println("m-substr: caseIgnoreSubstringsMatch");
            pw.println("m-equality: caseIgnoreMatch");
            pw.println("m-collective: FALSE");
            pw.println("m-singlevalue: FALSE");
            pw.println("m-obsolete: FALSE");
            pw.println("m-nousermodification: FALSE");
            pw.println("objectclass: metaAttributeType");
            pw.println("objectclass: metaTop");
            pw.println("objectclass: top");
            pw.println("creatorsname: uid=admin,ou=system");
        } catch(Exception ex) {
            if(pw != null) {
                pw.close();
            }
        }
        
        File ldiffFileCn = new File(workdir+"/ldapServer/partitions/schema/ou=schema/cn=system/ou=attributetypes/m-oid=2.5.4.3.ldif");
        System.out.println("Updating common name definition");
        if(ldiffFileCn.exists()) {
            ldiffFileCn.delete();
            System.out.println("Removed existing definition");
        }
        System.out.println("LdiffFile = "+ldiffFileCn.toString());
        pw = null;
        try {
            pw= new PrintWriter(new FileWriter(ldiffFileCn));
            pw.println("version: 1");
            pw.println("dn: m-oid=2.5.4.3,ou=attributeTypes,cn=system,ou=schema");
            pw.println("m-singlevalue: FALSE");
            pw.println("m-obsolete: FALSE");
            pw.println("m-description: RFC2256: common name(s) for which the entity is known by");
            pw.println("m-usage: USER_APPLICATIONS");
            pw.println("creatorsname: uid=admin,ou=system");
            pw.println("m-collective: FALSE");
            pw.println("m-oid: 2.5.4.3");
            pw.println("m-supattributetype: name");
            pw.println("m-substr: caseExactSubstringsMatch");
            pw.println("m-nousermodification: FALSE");
            pw.println("m-syntax: 1.3.6.1.4.1.1466.115.121.1.15");
            pw.println("objectclass: metaAttributeType");
            pw.println("objectclass: metaTop");
            pw.println("objectclass: top");
            pw.println("m-name: cn");
            pw.println("m-name: commonName");
            pw.println("m-equality: caseExactMatch");
        } catch(Exception ex) {
            if(pw != null) {
                pw.close();
            }
        }
    }

    /**
     * Load data required by the ldap directory implementation from a directory.
     */
    private void loadData() throws Exception
    {
        File schemaPartitionDirectory = new File(ds.getInstanceLayout()
                .getPartitionsDirectory(), "schema");
        SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
        SchemaManager schemaManager = new DefaultSchemaManager(loader);
        schemaManager.loadAllEnabled();
        ds.setSchemaManager(schemaManager);

        LdifPartition schemaLdifPartition = new LdifPartition(schemaManager,
                ds.getDnFactory());
        schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());
        SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
        schemaPartition.setWrappedPartition(schemaLdifPartition);
        ds.setSchemaPartition(schemaPartition);

        JdbmPartition systemPartition = new JdbmPartition(ds.getSchemaManager(),
                ds.getDnFactory());
        systemPartition.setId("system");
        systemPartition.setPartitionPath(new File(ds.getInstanceLayout()
                .getPartitionsDirectory(), systemPartition.getId()).toURI());
        systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
        systemPartition.setSchemaManager(ds.getSchemaManager());

        ds.setSystemPartition(systemPartition);
    }

    private void setUnityInterceptor(BaseInterceptor injectedInterceptor)
    {
        List<Interceptor> interceptors = ds.getInterceptors();
        // find Normalization interceptor in chain
        int insertionPosition = -1;
        for (int pos = 0; pos < interceptors.size(); ++pos)
        {
            Interceptor interceptor = interceptors.get(pos);
            if (interceptor instanceof NormalizationInterceptor)
            {
                insertionPosition = pos;
                break;
            }
        }
        // insert our new interceptor just behind
        interceptors.add(insertionPosition + 1, injectedInterceptor);
        ds.setInterceptors(interceptors);
    }

    /**
     * Start the directory service and the embedded ldap server
     */
    public void start() throws Exception
    {
        ds.startup();
        impl.start();
    }

    public void stop() throws Exception
    {
        impl.stop();
        ds.shutdown();
    }
}
