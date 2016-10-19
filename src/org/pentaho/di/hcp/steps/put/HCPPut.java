package org.pentaho.di.hcp.steps.put;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.vfs.KettleVFS;
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

public class HCPPut extends BaseStep implements StepInterface {
  private static Class<?> PKG = HCPPut.class; // for i18n purposes, needed by
                                              // Translator2!!

  public HCPPut(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
    super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
  }

  @Override
  public boolean init(StepMetaInterface smi, StepDataInterface sdi) {

    HCPPutMeta meta = (HCPPutMeta) smi;
    HCPPutData data = (HCPPutData) sdi;

    boolean error = false;
    if (meta.getConnection() == null) {
      log.logError(BaseMessages.getString(PKG, "HCPPut.Error.HCPConnectionNotSpecified"));
      error = true;
    }
    if (StringUtils.isEmpty(meta.getSourceFileField())) {
      log.logError(BaseMessages.getString(PKG, "HCPPut.Error.SourceFileFieldNotSpecified"));
      error = true;
    }
    if (StringUtils.isEmpty(meta.getTargetFileField())) {
      log.logError(BaseMessages.getString(PKG, "HCPPut.Error.TargetFileFieldNotSpecified"));
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

    HCPPutMeta meta = (HCPPutMeta) smi;
    HCPPutData data = (HCPPutData) sdi;

    Object[] row = getRow();
    if (row == null) {
      setOutputDone();
      return false;
    }

    if (first) {
      first = false;

      data.sourcePathIndex = getInputRowMeta().indexOfValue(meta.getSourceFileField());
      if (data.sourcePathIndex < 0) {
        throw new KettleException(
            BaseMessages.getString(PKG, "HCPPut.Error.SourceFileFieldNotFound", meta.getSourceFileField()));
      }
      data.targetPathIndex = getInputRowMeta().indexOfValue(meta.getTargetFileField());
      if (data.targetPathIndex < 0) {
        throw new KettleException(
            BaseMessages.getString(PKG, "HCPPut.Error.TargetFileFieldNotFound", meta.getTargetFileField()));
      }
      
      data.outputRowMeta = getInputRowMeta().clone();
      meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);      
    }

    String sourceFilePath = getInputRowMeta().getString(row, data.sourcePathIndex);
    String targetFilePath = getInputRowMeta().getString(row, data.targetPathIndex);

    if (StringUtils.isEmpty(sourceFilePath)) {
      log.logError("An empty source file path is not supported at this time.");
      stopAll();
      return false;
    }
    if (StringUtils.isEmpty(targetFilePath)) {
      log.logError("An empty target file path is not supported at this time. Source file path: "+sourceFilePath);
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

    BufferedInputStream fileInputStream = null;
    try {
      
      URL url = new URL(sourceFilePath);
      System.out.print("protocol : "+url.getProtocol());
      System.out.print("host : "+url.getHost());
      URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
      url = uri.toURL();
      
      System.out.println("Cleaned URL : "+url.toString());
      
      
      fileInputStream = new BufferedInputStream(KettleVFS.getInputStream(url.toString()), data.bufferSize);

      // Execute an HTTP PUT. This tells HCP to store the data being provided

      ClientResponse response;
      if (meta.isUpdating()) {
        response = builder.type(MediaType.APPLICATION_OCTET_STREAM_TYPE).post(ClientResponse.class, fileInputStream);
      } else {
        response = builder.type(MediaType.APPLICATION_OCTET_STREAM_TYPE).put(ClientResponse.class, fileInputStream);
      }
      
      responseCode = response.getStatus();
      if (log.isDebug()) {
        log.logDebug(BaseMessages.getString(PKG, "HCPPut.StatusCode", requestUrl, responseCode));
      }
      
    } catch (Exception e) {
      log.logError(BaseMessages.getString(PKG, "HCPPut.Error.ErrorUsingHCPService"), e);
    } finally {
      if (fileInputStream != null) {
        try {
          fileInputStream.close();
        } catch (IOException e) {
          // Ignore this error for logging brevity: doesn't reveal the originating error.
        }
      }
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
    // HCPPutMeta meta = (HCPPutMeta) smi;
    HCPPutData data = (HCPPutData) sdi;

    data.client.destroy();

    super.dispose(smi, sdi);
  }
}
