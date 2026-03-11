package com.authfusion.sso.ldap.service;

import com.authfusion.sso.ldap.config.LdapProperties;
import lombok.extern.slf4j.Slf4j;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Slf4j
public class LdapConnectionService {

    private final LdapProperties properties;

    public LdapConnectionService(LdapProperties properties) {
        this.properties = properties;
    }

    public DirContext createBindConnection() throws NamingException {
        Hashtable<String, String> env = buildEnvironment();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, properties.getBindDn());
        env.put(Context.SECURITY_CREDENTIALS, properties.getBindPassword());
        return new InitialDirContext(env);
    }

    public DirContext createUserConnection(String userDn, String password) throws NamingException {
        Hashtable<String, String> env = buildEnvironment();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, userDn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        return new InitialDirContext(env);
    }

    public boolean testConnection() {
        try {
            DirContext ctx = createBindConnection();
            ctx.close();
            return true;
        } catch (NamingException e) {
            log.warn("LDAP connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private Hashtable<String, String> buildEnvironment() {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, properties.getUrl() + "/" + properties.getBaseDn());
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(properties.getConnectTimeout()));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(properties.getReadTimeout()));
        return env;
    }
}
