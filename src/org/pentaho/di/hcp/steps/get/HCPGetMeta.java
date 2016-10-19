package org.pentaho.di.hcp.steps.get;

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
    id = "HCPGet", 
    name = "HCP Get", 
    description = "Hitachi Content Platform : this step allows you to get documents from the store", 
    categoryDescription = "HCP", 
    image = "ui/images/PFI.svg"
  )
public class HCPGetMeta extends BaseStepMeta implements StepMetaInterface {

  private static Class<?> PKG = HCPGetMeta.class; // for i18n purposes, needed by Translator2!!

  private static final String TAG_CONNECTION = "connection";
  private static final String TAG_SOURCE_FILE = "source_field";
  private static final String TAG_TARGET_FILE = "target_field";
  private static final String TAG_RESPONSE_CODE_FIELD = "response_code_field";
  private static final String TAG_RESPONSE_TIME_FIELD = "response_time_field";
  private static final String TAG_RESPONSE_SIZE_FIELD = "response_size_field";

  private HCPConnection connection;

  private String sourceFileField;
  private String targetFileField;

  private String responseCodeField;
  private String responseTimeField;
  private String responseSizeField;

  public HCPGetMeta() {
    super();
  }

  @Override
  public void setDefault() {
  }

  @Override
  public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
      Trans trans) {
    return new HCPGet(stepMeta, stepDataInterface, copyNr, transMeta, trans);
  }

  @Override
  public StepDataInterface getStepData() {
    return new HCPGetData();
  }

  @Override
  public String getDialogClassName() {
    return HCPGetDialog.class.getName();
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
    
    if (StringUtils.isNotEmpty(responseSizeField)) {
      ValueMetaInterface timeValue = new ValueMetaInteger(responseSizeField);
      timeValue.setLength(String.valueOf(Long.MAX_VALUE).length());
      timeValue.setOrigin(name);
      inputRowMeta.addValueMeta(timeValue);
    }
  }

  @Override
  public String getXML() throws KettleException {
    StringBuilder xml = new StringBuilder();

    xml.append(XMLHandler.addTagValue(TAG_CONNECTION, connection == null ? null : connection.getName()));
    xml.append(XMLHandler.addTagValue(TAG_SOURCE_FILE, sourceFileField));
    xml.append(XMLHandler.addTagValue(TAG_TARGET_FILE, targetFileField));
    xml.append(XMLHandler.addTagValue(TAG_RESPONSE_CODE_FIELD, responseCodeField));
    xml.append(XMLHandler.addTagValue(TAG_RESPONSE_TIME_FIELD, responseTimeField));
    xml.append(XMLHandler.addTagValue(TAG_RESPONSE_SIZE_FIELD, responseSizeField));

    return xml.toString();
  }

  @Override
  public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
    try {

      String connectionName = XMLHandler.getTagValue(stepnode, TAG_CONNECTION);
      if (StringUtils.isNotEmpty(connectionName)) {
        try {
          connection = HCPConnectionUtils.getConnectionFactory(metaStore).loadElement(connectionName);
        } catch (MetaStoreException e) {
          // We just log the message but we don't abort the complete meta-data
          // loading.
          //
          log.logError(BaseMessages.getString(PKG, "HCPGetMeta.Error.HCPConnectionNotFound", connectionName));
          connection = null;
        }
      }
      sourceFileField = XMLHandler.getTagValue(stepnode, TAG_SOURCE_FILE);
      targetFileField = XMLHandler.getTagValue(stepnode, TAG_TARGET_FILE);
      responseCodeField = XMLHandler.getTagValue(stepnode, TAG_RESPONSE_CODE_FIELD);
      responseTimeField = XMLHandler.getTagValue(stepnode, TAG_RESPONSE_TIME_FIELD);
      responseSizeField = XMLHandler.getTagValue(stepnode, TAG_RESPONSE_SIZE_FIELD);

    } catch (Exception e) {
      throw new KettleXMLException(BaseMessages.getString(PKG, "HCPGetMeta.Error.CouldNotLoadXML"), e);
    }
  }

  @Override
  public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
      throws KettleException {

    rep.saveStepAttribute(id_transformation, id_step, TAG_CONNECTION, connection == null ? null : connection.getName());
    rep.saveStepAttribute(id_transformation, id_step, TAG_SOURCE_FILE, sourceFileField);
    rep.saveStepAttribute(id_transformation, id_step, TAG_TARGET_FILE, targetFileField);
    rep.saveStepAttribute(id_transformation, id_step, TAG_RESPONSE_CODE_FIELD, responseCodeField);
    rep.saveStepAttribute(id_transformation, id_step, TAG_RESPONSE_TIME_FIELD, responseTimeField);
    rep.saveStepAttribute(id_transformation, id_step, TAG_RESPONSE_SIZE_FIELD, responseSizeField);
  }

  @Override
  public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
      throws KettleException {

    String connectionName = rep.getStepAttributeString(id_step, TAG_CONNECTION);
    if (StringUtils.isNotEmpty(connectionName)) {
      try {
        connection = HCPConnectionUtils.getConnectionFactory(metaStore).loadElement(connectionName);
      } catch (MetaStoreException e) {
        // We just log the message but we don't abort the complete meta-data
        // loading.
        //
        log.logError(BaseMessages.getString(PKG, "HCPGetMeta.Error.HCPConnectionNotFound", connectionName));
        connection = null;
      }
    }
    sourceFileField = rep.getStepAttributeString(id_step, TAG_SOURCE_FILE);
    targetFileField = rep.getStepAttributeString(id_step, TAG_TARGET_FILE);
    responseCodeField = rep.getStepAttributeString(id_step, TAG_RESPONSE_CODE_FIELD);
    responseTimeField = rep.getStepAttributeString(id_step, TAG_RESPONSE_TIME_FIELD);
    responseSizeField = rep.getStepAttributeString(id_step, TAG_RESPONSE_SIZE_FIELD);
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

  public String getResponseSizeField() {
    return responseSizeField;
  }

  public void setResponseSizeField(String responseSizeField) {
    this.responseSizeField = responseSizeField;
  }
}
