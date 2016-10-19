package org.pentaho.di.hcp.steps.delete;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import com.sun.jersey.client.apache.ApacheHttpClient;

public class HCPDeleteData extends BaseStepData implements StepDataInterface {
  
  public ApacheHttpClient client;
  public int sourcePathIndex;
  public int targetPathIndex;
  public String authorization;
  public int bufferSize;
  public RowMetaInterface outputRowMeta;

  public HCPDeleteData() {
  }
}
