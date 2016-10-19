/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.hcp.steps.get;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.hcp.shared.HCPConnection;
import org.pentaho.di.hcp.shared.HCPConnectionUtils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.persist.MetaStoreFactory;
import org.pentaho.metastore.util.PentahoDefaults;

public class HCPGetDialog extends BaseStepDialog implements StepDialogInterface {
  private static Class<?> PKG = HCPGetMeta.class; // for i18n purposes, needed by Translator2!!

  private CCombo wConnection;
  private ComboVar wSourceFileField;
  private ComboVar wTargetFileField;
  private Text wResponseCodeField;
  private Text wResponseTimeField;
  private Text wResponseSizeField;
  
  private HCPGetMeta input;

  private Button wNewConnection, wEditConnection, wDeleteConnection;
  
  private static MetaStoreFactory<HCPConnection> staticFactory;
  private static MetaStoreFactory<HCPConnection> getConnectionFactory(IMetaStore metaStore) {
    if (staticFactory==null) {
      staticFactory = new MetaStoreFactory<HCPConnection>(HCPConnection.class, metaStore, PentahoDefaults.NAMESPACE);
    }
    return staticFactory;
  }

  public HCPGetDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (BaseStepMeta) in, transMeta, sname );
    input = (HCPGetMeta) in;
  }

  public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    props.setLook( shell );
    setShellImage( shell, input );

    ModifyListener lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        input.setChanged();
      }
    };
    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( BaseMessages.getString( PKG, "HCPGetDialog.Shell.Title" ) );

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Stepname line
    wlStepname = new Label( shell, SWT.RIGHT );
    wlStepname.setText( BaseMessages.getString( PKG, "HCPGetDialog.Stepname.Label" ) );
    props.setLook( wlStepname );
    fdlStepname = new FormData();
    fdlStepname.left = new FormAttachment( 0, 0 );
    fdlStepname.right = new FormAttachment( middle, -margin );
    fdlStepname.top = new FormAttachment( 0, margin );
    wlStepname.setLayoutData( fdlStepname );
    wStepname = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wStepname.setText( stepname );
    props.setLook( wStepname );
    wStepname.addModifyListener( lsMod );
    fdStepname = new FormData();
    fdStepname.left = new FormAttachment( middle, 0 );
    fdStepname.top = new FormAttachment( 0, margin );
    fdStepname.right = new FormAttachment( 100, 0 );
    wStepname.setLayoutData( fdStepname );
    Control lastControl = wStepname;

    // Connection
    //  first add 3 buttons to the right, then fill the rest of the line with a combo drop-down
    //
    wDeleteConnection = new Button( shell, SWT.PUSH);
    wDeleteConnection.setText( BaseMessages.getString( PKG, "HCPGetDialog.DeleteConnection.Label" ) );
    props.setLook( wDeleteConnection );
    FormData fdDeleteConnection = new FormData();
    fdDeleteConnection.right = new FormAttachment( 100, 0 );
    fdDeleteConnection.top = new FormAttachment( lastControl, margin );
    wDeleteConnection.setLayoutData( fdDeleteConnection );
    wDeleteConnection.addSelectionListener(new SelectionAdapter() { @Override
    public void widgetSelected(SelectionEvent event) {
      deleteConnection();
    } });
    
    wEditConnection = new Button( shell, SWT.PUSH);
    wEditConnection.setText( BaseMessages.getString( PKG, "HCPGetDialog.EditConnection.Label" ) );
    props.setLook( wEditConnection );
    FormData fdEditConnection = new FormData();
    fdEditConnection.right = new FormAttachment( wDeleteConnection, -margin );
    fdEditConnection.top = new FormAttachment( lastControl, margin );
    wEditConnection.setLayoutData( fdEditConnection );
    wEditConnection.addSelectionListener(new SelectionAdapter() { @Override
    public void widgetSelected(SelectionEvent event) {
      editConnection();
    } });
    
    wNewConnection = new Button( shell, SWT.PUSH);
    wNewConnection.setText( BaseMessages.getString( PKG, "HCPGetDialog.NewConnection.Label" ) );
    props.setLook( wNewConnection );
    FormData fdNewConnection = new FormData();
    fdNewConnection.right = new FormAttachment( wEditConnection, -margin );
    fdNewConnection.top = new FormAttachment( lastControl, margin );
    wNewConnection.setLayoutData( fdNewConnection );
    wNewConnection.addSelectionListener(new SelectionAdapter() { @Override
    public void widgetSelected(SelectionEvent event) {
      newConnection();
    } });

    
    Label wlConnection = new Label( shell, SWT.RIGHT );
    wlConnection.setText( BaseMessages.getString( PKG, "HCPGetDialog.Connection.Label" ) );
    props.setLook( wlConnection );
    FormData fdlConnection = new FormData();
    fdlConnection.left = new FormAttachment( 0, 0 );
    fdlConnection.right = new FormAttachment( middle, -margin );
    fdlConnection.top = new FormAttachment( lastControl, margin );
    wlConnection.setLayoutData( fdlConnection );
    wConnection = new CCombo( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wConnection.setToolTipText( BaseMessages.getString( PKG, "HCPGetDialog.Connection.Tooltip" ) );
    props.setLook( wConnection );
    wConnection.addModifyListener( lsMod );
    FormData fdConnection = new FormData();
    fdConnection.left = new FormAttachment( middle, 0 );
    fdConnection.top = new FormAttachment( lastControl, margin );
    fdConnection.right = new FormAttachment( wNewConnection, -margin );
    wConnection.setLayoutData( fdConnection );
    wConnection.addFocusListener( new FocusAdapter() {
      public void focusGained( org.eclipse.swt.events.FocusEvent event ) {
        Cursor busy = new Cursor( shell.getDisplay(), SWT.CURSOR_WAIT );
        shell.setCursor( busy );
        try {
          List<String> names = getConnectionFactory(metaStore).getElementNames();
          Collections.sort(names);
          wConnection.setItems(names.toArray(new String[names.size()]));
        } catch(Exception exception) {
          new ErrorDialog(shell, 
              BaseMessages.getString(PKG, "HCPGetDialog.Error.ErrorGettingConnectionsList.Title"), 
              BaseMessages.getString(PKG, "HCPGetDialog.Error.ErrorGettingConnectionsList.Message"), exception);
        }
        shell.setCursor( null );
        busy.dispose();
      }
    } );
    lastControl = wConnection;
    
    // Source file field
    //
    Label wlSourceFileField = new Label( shell, SWT.RIGHT );
    wlSourceFileField.setText( BaseMessages.getString( PKG, "HCPGetDialog.SourceFileField.Label" ) );
    props.setLook( wlSourceFileField );
    FormData fdlSourceFileField = new FormData();
    fdlSourceFileField.left = new FormAttachment( 0, 0 );
    fdlSourceFileField.right = new FormAttachment( middle, -margin );
    fdlSourceFileField.top = new FormAttachment( lastControl, margin );
    wlSourceFileField.setLayoutData( fdlSourceFileField );
    wSourceFileField = new ComboVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wSourceFileField.setToolTipText( BaseMessages.getString( PKG, "HCPGetDialog.SourceFileField.Tooltip" ) );
    props.setLook( wSourceFileField );
    wSourceFileField.addModifyListener( lsMod );
    FormData fdSourceFileField = new FormData();
    fdSourceFileField.left = new FormAttachment( middle, 0 );
    fdSourceFileField.top = new FormAttachment( lastControl, margin );
    fdSourceFileField.right = new FormAttachment( 100, 0 );
    wSourceFileField.setLayoutData( fdSourceFileField );
    wSourceFileField.addFocusListener( new FocusAdapter() {
      public void focusGained( org.eclipse.swt.events.FocusEvent e ) {
        Cursor busy = new Cursor( shell.getDisplay(), SWT.CURSOR_WAIT );
        shell.setCursor( busy );
        BaseStepDialog.getFieldsFromPrevious( wSourceFileField, transMeta, stepMeta );
        shell.setCursor( null );
        busy.dispose();
      }
    } );
    lastControl = wSourceFileField;

    // Target file field
    //
    Label wlTargetFileField = new Label( shell, SWT.RIGHT );
    wlTargetFileField.setText( BaseMessages.getString( PKG, "HCPGetDialog.TargetFileField.Label" ) );
    props.setLook( wlTargetFileField );
    FormData fdlTargetFileField = new FormData();
    fdlTargetFileField.left = new FormAttachment( 0, 0 );
    fdlTargetFileField.right = new FormAttachment( middle, -margin );
    fdlTargetFileField.top = new FormAttachment( lastControl, margin );
    wlTargetFileField.setLayoutData( fdlTargetFileField );
    wTargetFileField = new ComboVar( transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wTargetFileField.setToolTipText( BaseMessages.getString( PKG, "HCPGetDialog.TargetFileField.Tooltip" ) );
    props.setLook( wTargetFileField );
    wTargetFileField.addModifyListener( lsMod );
    FormData fdTargetFileField = new FormData();
    fdTargetFileField.left = new FormAttachment( middle, 0 );
    fdTargetFileField.top = new FormAttachment( lastControl, margin );
    fdTargetFileField.right = new FormAttachment( 100, 0 );
    wTargetFileField.setLayoutData( fdTargetFileField );
    wTargetFileField.addFocusListener( new FocusAdapter() {
      public void focusGained( org.eclipse.swt.events.FocusEvent e ) {
        Cursor busy = new Cursor( shell.getDisplay(), SWT.CURSOR_WAIT );
        shell.setCursor( busy );
        BaseStepDialog.getFieldsFromPrevious( wTargetFileField, transMeta, stepMeta );
        shell.setCursor( null );
        busy.dispose();
      }
    } );
    lastControl = wTargetFileField;
    
    // Response code field
    //
    Label wlResponseCodeField = new Label( shell, SWT.RIGHT );
    wlResponseCodeField.setText( BaseMessages.getString( PKG, "HCPGetDialog.ResponseCodeField.Label" ) );
    props.setLook( wlResponseCodeField );
    FormData fdlResponseCodeField = new FormData();
    fdlResponseCodeField.left = new FormAttachment( 0, 0 );
    fdlResponseCodeField.right = new FormAttachment( middle, -margin );
    fdlResponseCodeField.top = new FormAttachment( lastControl, margin );
    wlResponseCodeField.setLayoutData( fdlResponseCodeField );
    wResponseCodeField = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wResponseCodeField );
    wResponseCodeField.addModifyListener( lsMod );
    FormData fdResponseCodeField = new FormData();
    fdResponseCodeField.left = new FormAttachment( middle, 0 );
    fdResponseCodeField.top = new FormAttachment( lastControl, margin );
    fdResponseCodeField.right = new FormAttachment( 100, 0 );
    wResponseCodeField.setLayoutData( fdResponseCodeField );
    lastControl = wResponseCodeField;

    // Response time field
    //
    Label wlResponseTimeField = new Label( shell, SWT.RIGHT );
    wlResponseTimeField.setText( BaseMessages.getString( PKG, "HCPGetDialog.ResponseTimeField.Label" ) );
    props.setLook( wlResponseTimeField );
    FormData fdlResponseTimeField = new FormData();
    fdlResponseTimeField.left = new FormAttachment( 0, 0 );
    fdlResponseTimeField.right = new FormAttachment( middle, -margin );
    fdlResponseTimeField.top = new FormAttachment( lastControl, margin );
    wlResponseTimeField.setLayoutData( fdlResponseTimeField );
    wResponseTimeField = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wResponseTimeField );
    wResponseTimeField.addModifyListener( lsMod );
    FormData fdResponseTimeField = new FormData();
    fdResponseTimeField.left = new FormAttachment( middle, 0 );
    fdResponseTimeField.top = new FormAttachment( lastControl, margin );
    fdResponseTimeField.right = new FormAttachment( 100, 0 );
    wResponseTimeField.setLayoutData( fdResponseTimeField );
    lastControl = wResponseTimeField;
    
    // Response size field
    //
    Label wlResponseSizeField = new Label( shell, SWT.RIGHT );
    wlResponseSizeField.setText( BaseMessages.getString( PKG, "HCPGetDialog.ResponseSizeField.Label" ) );
    props.setLook( wlResponseSizeField );
    FormData fdlResponseSizeField = new FormData();
    fdlResponseSizeField.left = new FormAttachment( 0, 0 );
    fdlResponseSizeField.right = new FormAttachment( middle, -margin );
    fdlResponseSizeField.top = new FormAttachment( lastControl, margin );
    wlResponseSizeField.setLayoutData( fdlResponseSizeField );
    wResponseSizeField = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wResponseSizeField );
    wResponseSizeField.addModifyListener( lsMod );
    FormData fdResponseSizeField = new FormData();
    fdResponseSizeField.left = new FormAttachment( middle, 0 );
    fdResponseSizeField.top = new FormAttachment( lastControl, margin );
    fdResponseSizeField.right = new FormAttachment( 100, 0 );
    wResponseSizeField.setLayoutData( fdResponseSizeField );
    lastControl = wResponseSizeField;

    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

    BaseStepDialog.positionBottomButtons( shell, new Button[] { wOK, wCancel }, margin, lastControl );

    // Add listeners
    lsOK = new Listener() {
      public void handleEvent( Event e ) {
        ok();
      }
    };
    lsCancel = new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    };

    wOK.addListener( SWT.Selection, lsOK );
    wCancel.addListener( SWT.Selection, lsCancel );

    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };

    wStepname.addSelectionListener( lsDef );
    
    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    // Set the shell size, based upon previous time...
    setSize();

    getData();
    input.setChanged( changed );

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return stepname;
  }

  protected void newConnection() {
    HCPConnectionUtils.newConnection(shell, getConnectionFactory(metaStore));
  }

  protected void editConnection() {
    String connectionName = wConnection.getText();
    HCPConnectionUtils.editConnection(shell, getConnectionFactory(metaStore), connectionName);
  }

  protected void deleteConnection() {
    String connectionName = wConnection.getText();
    HCPConnectionUtils.deleteConnection(shell, getConnectionFactory(metaStore), connectionName);
  }

  public void getData() {
    wConnection.setText(input.getConnection()==null ? "" : Const.NVL(input.getConnection().getName(), ""));
    wSourceFileField.setText( Const.NVL( input.getSourceFileField(), "" ) );
    wTargetFileField.setText( Const.NVL( input.getTargetFileField(), "" ) );
    wResponseCodeField.setText(Const.NVL(input.getResponseCodeField(), ""));
    wResponseTimeField.setText(Const.NVL(input.getResponseTimeField(), ""));
    wResponseSizeField.setText(Const.NVL(input.getResponseSizeField(), ""));

    wStepname.selectAll();
    wStepname.setFocus();
  }

  private void cancel() {
    stepname = null;
    input.setChanged( changed );
    dispose();
  }

  private void ok() {
    if ( Const.isEmpty( wStepname.getText() ) ) {
      return;
    }

    stepname = wStepname.getText(); // return value

    input.setConnection(null);
    String connectionName = wConnection.getText();
    if (StringUtils.isNotEmpty(connectionName)) {
      try{
        input.setConnection(getConnectionFactory(metaStore).loadElement(connectionName));
      } catch(Exception exception) {
        new ErrorDialog(shell, 
            BaseMessages.getString(PKG, "HCPGetDialog.Error.ErrorLoadingConnectionWithName.Title"), 
            BaseMessages.getString(PKG, "HCPGetDialog.Error.ErrorLoadingConnectionWithName.Message", connectionName), 
            exception);
      }
    }
    input.setSourceFileField(wSourceFileField.getText() );
    input.setTargetFileField(wTargetFileField.getText() );
    input.setResponseCodeField(wResponseCodeField.getText());
    input.setResponseTimeField(wResponseTimeField.getText());
    input.setResponseSizeField(wResponseSizeField.getText());

    dispose();
  }
}
