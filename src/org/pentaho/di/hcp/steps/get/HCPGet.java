package org.pentaho.di.hcp.steps.get;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
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

public class HCPGet extends BaseStep implements StepInterface {
  private static Class<?> PKG = HCPGet.class; // for i18n purposes, needed by
                                              // Translator2!!

  public HCPGet(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
    super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
  }

  @Override
  public boolean init(StepMetaInterface smi, StepDataInterface sdi) {

    HCPGetMeta meta = (HCPGetMeta) smi;
    HCPGetData data = (HCPGetData) sdi;

    boolean error = false;
    if (meta.getConnection() == null) {
      log.logError(BaseMessages.getString(PKG, "HCPGet.Error.HCPConnectionNotSpecified"));
      error = true;
    }
    if (StringUtils.isEmpty(meta.getSourceFileField())) {
      log.logError(BaseMessages.getString(PKG, "HCPGet.Error.SourceFileFieldNotSpecified"));
      error = true;
    }
    if (StringUtils.isEmpty(meta.getTargetFileField())) {
      log.logError(BaseMessages.getString(PKG, "HCPGet.Error.TargetFileFieldNotSpecified"));
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

    HCPGetMeta meta = (HCPGetMeta) smi;
    HCPGetData data = (HCPGetData) sdi;

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
            BaseMessages.getString(PKG, "HCPGet.Error.SourceFileFieldNotFound", meta.getSourceFileField()));
      }
      data.targetPathIndex = getInputRowMeta().indexOfValue(meta.getTargetFileField());
      if (data.targetPathIndex < 0) {
        throw new KettleException(
            BaseMessages.getString(PKG, "HCPGet.Error.TargetFileFieldNotFound", meta.getTargetFileField()));
      }
      
      data.outputRowMeta = getInputRowMeta().clone();
      meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);
    }

    String sourceFilePath = getInputRowMeta().getString(row, data.sourcePathIndex);
    String targetFilePath = getInputRowMeta().getString(row, data.targetPathIndex);

    String restUrl = meta.getConnection().getRestUrl(this);

    // Remove last slash character of URL
    //
    if (restUrl.length()>0 && restUrl.charAt(restUrl.length()-1)=='/') {
      restUrl = restUrl.substring(0, restUrl.length() - 1);
    }
    
    // Add slash to start of source (HCP) path
    //
    if (!sourceFilePath.substring(0, 1).equals("/")) {
      sourceFilePath = '/' + sourceFilePath;
    }

    long startTime = System.currentTimeMillis();
    int responseCode = -1;
    long responseSize = -1;

    // Calculate the URL for the target file...
    //
    String requestUrl = restUrl + sourceFilePath;
    if (log.isDebug()) {
      log.logDebug("Request URL : "+requestUrl);
    }

    WebResource webResource = data.client.resource(requestUrl);
    Builder builder = webResource.getRequestBuilder().header("Authorization", data.authorization);

    OutputStream outputStream = null;
    InputStream inputStream = null;
    
    try {
      outputStream = KettleVFS.getOutputStream(targetFilePath, false);

      // Execute an HTTP GET. This tells HCP to get the data on HCP

      ClientResponse response = builder
          .type(MediaType.APPLICATION_OCTET_STREAM_TYPE)
          .get(ClientResponse.class);
      
      responseCode = response.getStatus();
      if (log.isDebug()) {
        log.logDebug(BaseMessages.getString(PKG, "HCPGet.StatusCode", requestUrl, responseCode));
      }
      if (responseCode==200) {
        // Download the file...
        //
        inputStream = response.getEntityInputStream();
        
        /*
        byte[] buffer = new byte[data.bufferSize];
        int length = 0;
        while ((length = inputStream.read(buffer)) !=-1) {
         outputStream.write(buffer, 0, length);
        }
        */
         
        responseSize = IOUtils.copyLarge(inputStream, outputStream);
      }
      
      if (log.isDebug()) {
        log.logDebug(BaseMessages.getString(PKG, "HCPGet.StatusCode", requestUrl, responseCode));
      }

    } catch (Exception e) {
      log.logError(BaseMessages.getString(PKG, "HCPGet.Error.ErrorUsingHCPService"), e);
    } finally {
      if (outputStream != null) {
        try {
          outputStream.flush();
          outputStream.close();
        } catch (IOException e) {
          // Ignore this error for logging brevity: doesn't reveal the originating error.
        }
      }
      if (inputStream != null) {
        try {
          inputStream.close();
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
    if (StringUtils.isNotEmpty(meta.getResponseSizeField())) {
      outputRow[outputIndex++] = Long.valueOf(responseSize);
    }
    
    putRow(data.outputRowMeta, outputRow);


    return true;
  }

  @Override
  public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
    // HCPGetMeta meta = (HCPGetMeta) smi;
    HCPGetData data = (HCPGetData) sdi;

    data.client.destroy();

    super.dispose(smi, sdi);
  }
}
