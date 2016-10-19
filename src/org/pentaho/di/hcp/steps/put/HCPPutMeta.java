package org.pentaho.di.hcp.steps.put;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.hcp.shared.HCPConnection;
import org.pentaho.di.hcp.shared.HCPConnectionUtils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.api.exceptions.MetaStoreException;
import org.w3c.dom.Node;

@Step( 
    id="HCPPut",
    name="HCP Put",
    description="Hitachi Content Platform : this step allows you to put documents into the store",
    categoryDescription = "HCP", 
    image = "ui/images/PFO.svg"    
  )
public class HCPPutMeta extends BaseStepMeta implements StepMetaInterface {
  
  private static Class<?> PKG = HCPPutMeta.class; // for i18n purposes, needed by Translator2!!
  
  private static final String TAG_CONNECTION = "connection";
  private static final String TAG_SOURCE_FILE_FIELD = "source_field";
  private static final String TAG_TARGET_FILE_FIELD = "target_field";
  private static final String TAG_UPDATING = "updating";
  private static final String TAG_RESPONSE_CODE_FIELD = "response_code_field";
  private static final String TAG_RESPONSE_TIME_FIELD = "response_time_field";
  
  private HCPConnection connection;
  
  private String sourceFileField;
  private String targetFileField;
  
  private boolean updating;
  
  private String responseCodeField;
  private String responseTimeField;

  public HCPPutMeta() {
    super();
  }
  
  @Override
  public void setDefault() {
  }

  @Override
  public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
      Trans trans) {
    return new HCPPut(stepMeta, stepDataInterface, copyNr, transMeta, trans);
  }

  @Override
  public StepDataInterface getStepData() {
    return new HCPPutData();
  }
  
  @Override
  public String getDialogClassName() {
    return HCPPutDialog.class.getName();
  }
  
  @Override
  public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info, StepMeta nextStep,
      VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
    
    // Optionally add a fields...
    //
    if (StringUtils.isNotEmpty(responseCodeField)) {
      ValueMetaInterface codeValue = new ValueMetaInteger(responseCodeField);
      codeValue.setLength(3);
      codeValue.setOrigin(name);
      inputRowMeta.addValueMeta(codeValue);
    }
    
    if (StringUtils.isNotEmpty(responseTimeField)) {
      ValueMetaInterface timeValue = new ValueMetaInteger(responseTimeField);
      timeValue.setLength(7);
      timeValue.setOrigin(name);
      inputRowMeta.addValueMeta(timeValue);
    }
  }
  
  @Override
  public String getXML() throws KettleException {
    StringBuilder xml = new StringBuilder();
    
    xml.append(XMLHandler.addTagValue(TAG_CONNECTION, connection==null ? null : connection.getName()));
    xml.append(XMLHandler.addTagValue(TAG_SOURCE_FILE_FIELD, sourceFileField));
    xml.append(XMLHandler.addTagValue(TAG_TARGET_FILE_FIELD, targetFileField));
    xml.append(XMLHandler.addTagValue(TAG_UPDATING, targetFileField));
    xml.append(XMLHandler.addTagValue(TAG_RESPONSE_CODE_FIELD, responseCodeField));
    xml.append(XMLHandler.addTagValue(TAG_RESPONSE_TIME_FIELD, responseTimeField));
    
    return xml.toString();
  }
  
  @Override
  public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
    try {
      
      String connectionName = XMLHandler.getTagValue(stepnode, TAG_CONNECTION);
      if (StringUtils.isNotEmpty(connectionName)) {
        try {
          System.out.println("Loading HCP connection "+connectionName);
          connection = HCPConnectionUtils.getConnectionFactory(metaStore).loadElement(connectionName);
        } catch(Exception e) {
          // We just log the message but we don't abort the complete meta-data loading.
          //
          log.logError(BaseMessages.getString(PKG, "HCPPutMeta.Error.HCPConnectionNotFound", connectionName));
          connection = null;
        }
      }
      sourceFileField = XMLHandler.getTagValue(stepnode, TAG_SOURCE_FILE_FIELD);
      targetFileField = XMLHandler.getTagValue(stepnode, TAG_TARGET_FILE_FIELD);
      updating = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, TAG_UPDATING));
      responseCodeField = XMLHandler.getTagValue(stepnode, TAG_RESPONSE_CODE_FIELD);
      responseTimeField = XMLHandler.getTagValue(stepnode, TAG_RESPONSE_TIME_FIELD);
      
    } catch(Exception e) {
      throw new KettleXMLException(BaseMessages.getString(PKG, "HCPPutMeta.Error.CouldNotLoadXML"), e);
    }
  }
  
  @Override
  public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
      throws KettleException {
    
    rep.saveStepAttribute(id_transformation, id_step, TAG_CONNECTION, connection==null ? null : connection.getName());
    rep.saveStepAttribute(id_transformation, id_step, TAG_SOURCE_FILE_FIELD, sourceFileField);
    rep.saveStepAttribute(id_transformation, id_step, TAG_TARGET_FILE_FIELD, targetFileField);
    rep.saveStepAttribute(id_transformation, id_step, TAG_UPDATING, updating);
    rep.saveStepAttribute(id_transformation, id_step, TAG_RESPONSE_CODE_FIELD, responseCodeField);
    rep.saveStepAttribute(id_transformation, id_step, TAG_RESPONSE_TIME_FIELD, responseTimeField);
  }
  
  @Override
  public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
      throws KettleException {

    String connectionName = rep.getStepAttributeString(id_step, TAG_CONNECTION);
    if (StringUtils.isNotEmpty(connectionName)) {
      try {
        connection = HCPConnectionUtils.getConnectionFactory(metaStore).loadElement(connectionName);
      } catch(MetaStoreException e) {
        // We just log the message but we don't abort the complete meta-data loading.
        //
        log.logError(BaseMessages.getString(PKG, "HCPPutMeta.Error.HCPConnectionNotFound", connectionName));
        connection = null;
      }
    }
    sourceFileField = rep.getStepAttributeString(id_step, TAG_SOURCE_FILE_FIELD);
    targetFileField = rep.getStepAttributeString(id_step, TAG_TARGET_FILE_FIELD);
    updating = rep.getStepAttributeBoolean(id_step, TAG_UPDATING);
    responseCodeField = rep.getStepAttributeString(id_step, TAG_RESPONSE_CODE_FIELD);
    responseTimeField = rep.getStepAttributeString(id_step, TAG_RESPONSE_TIME_FIELD);
  }
  
  public HCPConnection getConnection() {
    return connection;
  }

  public void setConnection(HCPConnection connection) {
    this.connection = connection;
  }

  public String getSourceFileField() {
    return sourceFileField;
  }

  public void setSourceFileField(String sourceFileField) {
    this.sourceFileField = sourceFileField;
  }

  public String getTargetFileField() {
    return targetFileField;
  }

  public void setTargetFileField(String targetFileField) {
    this.targetFileField = targetFileField;
  }

  public boolean isUpdating() {
    return updating;
  }

  public void setUpdating(boolean updating) {
    this.updating = updating;
  }

  public String getResponseCodeField() {
    return responseCodeField;
  }

  public void setResponseCodeField(String responseCodeField) {
    this.responseCodeField = responseCodeField;
  }

  public String getResponseTimeField() {
    return responseTimeField;
  }

  public void setResponseTimeField(String responseTimeField) {
    this.responseTimeField = responseTimeField;
  }

}
