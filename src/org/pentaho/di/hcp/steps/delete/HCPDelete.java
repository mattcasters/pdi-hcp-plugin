package org.pentaho.di.hcp.steps.delete;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

public class HCPDelete extends BaseStep implements StepInterface {
  private static Class<?> PKG = HCPDelete.class; // for i18n purposes, needed by Translator2!!

  public HCPDelete(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
    super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
  }

  @Override
  public boolean init(StepMetaInterface smi, StepDataInterface sdi) {

    HCPDeleteMeta meta = (HCPDeleteMeta) smi;
    HCPDeleteData data = (HCPDeleteData) sdi;

    boolean error = false;
    if (meta.getConnection() == null) {
      log.logError(BaseMessages.getString(PKG, "HCPDelete.Error.HCPConnectionNotSpecified"));
      error = true;
    }
    if (StringUtils.isEmpty(meta.getTargetFileField())) {
      log.logError(BaseMessages.getString(PKG, "HCPDelete.Error.TargetFileFieldNotSpecified"));
      error = true;
    }
    if (error) {
      // Stop right here.
      return false;
    }

    data.bufferSize = 1024;
    data.authorization = meta.getConnection().getAuthorizationHeader();

    data.client = ApacheHttpClient.create(new DefaultApacheHttpClientConfig());
    data.client.setChunkedEncodingSize(data.bufferSize);

    return super.init(smi, sdi);
  }

  @Override
  public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {

    HCPDeleteMeta meta = (HCPDeleteMeta) smi;
    HCPDeleteData data = (HCPDeleteData) sdi;

    Object[] row = getRow();
    if (row == null) {
      setOutputDone();
      return false;
    }

    if (first) {
      first = false;

      data.targetPathIndex = getInputRowMeta().indexOfValue(meta.getTargetFileField());
      if (data.targetPathIndex < 0) {
        throw new KettleException(
            BaseMessages.getString(PKG, "HCPDelete.Error.TargetFileFieldNotFound", meta.getTargetFileField()));
      }
      
      data.outputRowMeta = getInputRowMeta().clone();
      meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);      
    }

    String targetFilePath = getInputRowMeta().getString(row, data.targetPathIndex);

    if (StringUtils.isEmpty(targetFilePath)) {
      log.logError("An empty target file path is not supported at this time. ");
      stopAll();
      return false;
    }
    
    long startTime = System.currentTimeMillis();
    int responseCode = -1;

    String restUrl = meta.getConnection().getRestUrl(this);

    // Remove last slash character of URL
    //
    if (restUrl.length()>0 && restUrl.charAt(restUrl.length()-1)=='/') {
      restUrl = restUrl.substring(0, restUrl.length() - 1);
    }
    
    // Add slash to start of target path
    //
    if (!targetFilePath.substring(0, 1).equals("/")) {
      targetFilePath = '/' + targetFilePath;
    }

    // Calculate the URL for the target file...
    //
    String requestUrl = restUrl + targetFilePath;
    if (log.isDebug()) {
      log.logDebug("Request URL : "+requestUrl);
    }
        
    WebResource webResource = data.client.resource(requestUrl);
    Builder builder = webResource.getRequestBuilder().header("Authorization", data.authorization);

    try {
      
      // Execute an HTTP PUT. This tells HCP to store the data being provided

      ClientResponse response = builder
          .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
          .delete(ClientResponse.class);
      
      responseCode = response.getStatus();
      if (log.isDebug()) {
        log.logDebug(BaseMessages.getString(PKG, "HCPDelete.StatusCode", requestUrl, responseCode));
      }
      
    } catch (Exception e) {
      log.logError(BaseMessages.getString(PKG, "HCPDelete.Error.ErrorUsingHCPService"), e);
    }
    
    long endTime = System.currentTimeMillis();
    
    Object[] outputRow = RowDataUtil.createResizedCopy(row, data.outputRowMeta.size());
    int outputIndex = getInputRowMeta().size();
    if (StringUtils.isNotEmpty(meta.getResponseCodeField())) {
      outputRow[outputIndex++] = Long.valueOf(responseCode);
    }
    if (StringUtils.isNotEmpty(meta.getResponseTimeField())) {
      outputRow[outputIndex++] = Long.valueOf(endTime-startTime);
    }
    
    putRow(data.outputRowMeta, outputRow);

    return true;
  }

  @Override
  public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
    // HCPDeleteMeta meta = (HCPDeleteMeta) smi;
    HCPDeleteData data = (HCPDeleteData) sdi;

    data.client.destroy();

    super.dispose(smi, sdi);
  }
}
