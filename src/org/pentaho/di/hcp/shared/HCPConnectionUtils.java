package org.pentaho.di.hcp.shared;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.persist.MetaStoreFactory;
import org.pentaho.metastore.util.PentahoDefaults;

public class HCPConnectionUtils {
  private static Class<?> PKG = HCPConnectionUtils.class; // for i18n purposes, needed by Translator2!!
  
  private static MetaStoreFactory<HCPConnection> staticFactory;
  public static MetaStoreFactory<HCPConnection> getConnectionFactory(IMetaStore metaStore) {
    if (staticFactory==null) {
      staticFactory = new MetaStoreFactory<HCPConnection>(HCPConnection.class, metaStore, PentahoDefaults.NAMESPACE);
    }
    return staticFactory;
  }

  public static void newConnection(Shell shell, MetaStoreFactory<HCPConnection> factory) {
    HCPConnection connection = new HCPConnection();
    boolean ok = false;
    while (!ok) {
      HCPConnectionDialog dialog = new HCPConnectionDialog(shell, connection);
      if (dialog.open()) {
        // write to metastore...
        try {
          if (factory.loadElement(connection.getName())!=null) {
            MessageBox box = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_ERROR);
            box.setText(BaseMessages.getString(PKG, "HCPConnectionUtils.Error.ConnectionExists.Title"));
            box.setMessage(BaseMessages.getString(PKG, "HCPConnectionUtils.Error.ConnectionExists.Message"));
            int answer = box.open();      
            if ((answer&SWT.YES)!=0) {
              factory.saveElement(connection);
              ok=true;
            }
          } else {
            factory.saveElement(connection);
            ok=true;
          }
        } catch(Exception exception) {
          new ErrorDialog(shell,
              BaseMessages.getString(PKG, "HCPConnectionUtils.Error.ErrorSavingConnection.Title"),
              BaseMessages.getString(PKG, "HCPConnectionUtils.Error.ErrorSavingConnection.Message"),
              exception);
        }
      }
    }
    
  }

  public static void editConnection(Shell shell, MetaStoreFactory<HCPConnection> factory, String connectionName) {
    if (StringUtils.isEmpty(connectionName)) {
      return;
    }
    try {
      HCPConnection hcpConnection = factory.loadElement(connectionName);
      if (hcpConnection==null) {
        newConnection(shell, factory);
      } else {
        HCPConnectionDialog hcpConnectionDialog = new HCPConnectionDialog(shell, hcpConnection);
        if (hcpConnectionDialog.open()) {
          factory.saveElement(hcpConnection);
        }
      }
    } catch(Exception exception) {
      new ErrorDialog(shell,
          BaseMessages.getString(PKG, "HCPConnectionUtils.Error.ErrorEditingConnection.Title"),
          BaseMessages.getString(PKG, "HCPConnectionUtils.Error.ErrorEditingConnection.Message"),
          exception);
    }
  }

  public static void deleteConnection(Shell shell, MetaStoreFactory<HCPConnection> factory, String connectionName) {
    if (StringUtils.isEmpty(connectionName)) {
      return;
    }
    
    MessageBox box = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_ERROR);
    box.setText(BaseMessages.getString(PKG, "HCPConnectionUtils.DeleteConnectionConfirmation.Title"));
    box.setMessage(BaseMessages.getString(PKG, "HCPConnectionUtils.DeleteConnectionConfirmation.Message", connectionName));
    int answer = box.open();      
    if ((answer&SWT.YES)!=0) {
      try {
        factory.deleteElement(connectionName);
      } catch(Exception exception) {
        new ErrorDialog(shell,
            BaseMessages.getString(PKG, "HCPConnectionUtils.Error.ErrorDeletingConnection.Title"),
            BaseMessages.getString(PKG, "HCPConnectionUtils.Error.ErrorDeletingConnection.Message", connectionName),
            exception);
      }
    }

  }

}
