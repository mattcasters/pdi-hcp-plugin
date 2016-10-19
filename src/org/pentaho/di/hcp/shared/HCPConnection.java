package org.pentaho.di.hcp.shared;

import java.security.MessageDigest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.metastore.persist.MetaStoreAttribute;
import org.pentaho.metastore.persist.MetaStoreElementType;

@MetaStoreElementType(name = "HCP Connection", description = "A shared connection to a Hitachi Content Platform server")
public class HCPConnection extends Variables implements Cloneable {
  private String name;

  @MetaStoreAttribute
  private String server;

  @MetaStoreAttribute
  private String port;

  @MetaStoreAttribute
  private String tenant;

  @MetaStoreAttribute
  private String namespace;

  @MetaStoreAttribute
  private String username;

  @MetaStoreAttribute(password = true)
  private String password;

  @MetaStoreAttribute(key = "SSL")
  private boolean useSSL;

  public HCPConnection() {
  }

  @Override
  public String toString() {
    return name == null ? super.toString() : name.toString();
  }

  @Override
  public int hashCode() {
    return name == null ? super.hashCode() : name.hashCode();
  }

  @Override
  public boolean equals(Object object) {

    if (object == this) {
      return true;
    }
    if (!(object instanceof HCPConnection)) {
      return false;
    }

    HCPConnection hcp = (HCPConnection) object;

    return name == null ? false : name.equalsIgnoreCase(hcp.name);
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getTenant() {
    return tenant;
  }

  public void setTenant(String tenant) {
    this.tenant = tenant;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isUseSSL() {
    return useSSL;
  }

  public void setUseSSL(boolean useSSL) {
    this.useSSL = useSSL;
  }

  /**
   * TODO: implement connection to HCP
   * 
   * @return true if the connection works, false if it doesn't
   * @throws Exception
   *           In case something goes wrong (so it usually doesn't return false)
   */
  public boolean test() throws Exception {
    throw new Exception("Test funcationality not yet implemented: unclear API");
  }

  public String getRestUrl(VariableSpace space) {

    /*
     * Construct the following URL:
     * 
     * http://tenant.namespace.server:port/rest
     */
    StringBuilder url = new StringBuilder();

    if (useSSL) {
      url.append("https://");
    } else {
      url.append("http://");
    }

    url.append(space.environmentSubstitute(tenant)).append('.');
    url.append(space.environmentSubstitute(namespace)).append('.');
    url.append(space.environmentSubstitute(server));

    String realPort = space.environmentSubstitute(port);
    if (StringUtils.isNotEmpty(realPort)) {
      url.append(':').append(realPort);
    }

    url.append("/rest");

    return url.toString();
  }

  public String getAuthorizationHeader() {

    try {
      String user64 = new String(Base64.encodeBase64(username.getBytes(Const.XML_ENCODING)));
      MessageDigest messageDigest = MessageDigest.getInstance("MD5");
      messageDigest.update(password.getBytes(Const.XML_ENCODING));
      byte[] md5PasswordHash = messageDigest.digest();
      String md5PasswordHex = new String(Hex.encodeHex(md5PasswordHash));
      String header = "HCP " + user64 + ":" + md5PasswordHex;

      return header;
    } catch (Exception e) {
      throw new RuntimeException("Unable to encode authorization header for user '" + username + "'", e);
    }
  }

}
