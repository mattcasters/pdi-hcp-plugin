package org.pentaho.di.hcp.shared;

import org.junit.Test;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;

import junit.framework.TestCase;

public class HCPConnectionTest extends TestCase {
  
  @Test
  public void testGetAuthorization() {
    HCPConnection connection = new HCPConnection();
    connection.setUsername("pentaho");
    connection.setPassword("MyP@ssw0rd!");
    String authHeader = connection.getAuthorizationHeader();
    assertEquals("HCP cGVudGFobw==:42c853d6bbd0cfddc2d0978df437fa97", authHeader);
  }

  @Test
  public void testGetUrl() {
    VariableSpace space = new Variables();
    space.setVariable("SERVER", "hcpdemo.com");
    space.setVariable("PORT", "8000");
    space.setVariable("NAMESPACE", "hcp-demo");
    space.setVariable("TENANT", "pentaho");
    
    HCPConnection connection = new HCPConnection();
    connection.setServer("${SERVER}");
    connection.setPort("${PORT}");
    connection.setNamespace("${NAMESPACE}");
    connection.setTenant("${TENANT}");
    
    String restUrl = connection.getRestUrl(space);
    assertEquals("http://pentaho.hcp-demo.hcpdemo.com:8000/rest", restUrl);    
  }

}
