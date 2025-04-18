/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.mysqlbulkloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import org.apache.hop.core.Const;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.row.value.ValueMetaDate;
import org.apache.hop.core.row.value.ValueMetaNumber;
import org.apache.hop.core.util.StreamLogger;
import org.apache.hop.core.util.Utils;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;

public class MySqlBulkLoader extends BaseTransform<MySqlBulkLoaderMeta, MySqlBulkLoaderData> {
  private static final Class<?> PKG = MySqlBulkLoaderMeta.class;
  public static final String MESSAGE_ERRORSERIALIZING = "MySqlBulkLoader.Message.ERRORSERIALIZING";

  private static final long THREAD_WAIT_TIME = 300000;
  private static final String THREAD_WAIT_TIME_TEXT = "5min";

  public MySqlBulkLoader(
      TransformMeta transformMeta,
      MySqlBulkLoaderMeta meta,
      MySqlBulkLoaderData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  public boolean execute(MySqlBulkLoaderMeta meta) throws HopException {
    Runtime rt = Runtime.getRuntime();

    try {
      // 1) Create the FIFO file using the "mkfifo" command...
      // Make sure to log all the possible output, also from STDERR
      //
      data.fifoFilename = resolve(meta.getFifoFileName());

      File fifoFile = new File(data.fifoFilename);
      if (!fifoFile.exists()) {
        // MKFIFO!
        //
        String mkFifoCmd = "mkfifo " + data.fifoFilename;
        //
        logBasic(
            BaseMessages.getString(
                PKG, "MySqlBulkLoader.Message.CREATINGFIFO", data.dbDescription, mkFifoCmd));
        Process mkFifoProcess = rt.exec(mkFifoCmd);
        StreamLogger errorLogger =
            new StreamLogger(getLogChannel(), mkFifoProcess.getErrorStream(), "mkFifoError");
        StreamLogger outputLogger =
            new StreamLogger(getLogChannel(), mkFifoProcess.getInputStream(), "mkFifoOuptut");
        new Thread(errorLogger).start();
        new Thread(outputLogger).start();
        int result = mkFifoProcess.waitFor();
        if (result != 0) {
          throw new HopException(
              BaseMessages.getString(
                  PKG, "MySqlBulkLoader.Message.ERRORFIFORC", result, mkFifoCmd));
        }

        String chmodCmd = "chmod 666 " + data.fifoFilename;
        logBasic(
            BaseMessages.getString(
                PKG,
                "MySqlBulkLoader.Message.SETTINGPERMISSIONSFIFO",
                data.dbDescription,
                chmodCmd));
        Process chmodProcess = rt.exec(chmodCmd);
        errorLogger =
            new StreamLogger(getLogChannel(), chmodProcess.getErrorStream(), "chmodError");
        outputLogger =
            new StreamLogger(getLogChannel(), chmodProcess.getInputStream(), "chmodOuptut");
        new Thread(errorLogger).start();
        new Thread(outputLogger).start();
        result = chmodProcess.waitFor();
        if (result != 0) {
          throw new HopException(
              BaseMessages.getString(PKG, "MySqlBulkLoader.Message.ERRORFIFORC", result, chmodCmd));
        }
      }

      // 2) Make a connection to MySql for sending SQL commands
      // (Also, we need a clear cache for getting up-to-date target metadata)
      if (meta.getConnection() == null) {
        logError(
            BaseMessages.getString(
                PKG, "MySqlBulkLoader.Init.ConnectionMissing", getTransformName()));
        return false;
      }
      data.db =
          new Database(
              this, variables, getPipelineMeta().findDatabase(meta.getConnection(), variables));
      data.db.connect();

      logBasic(
          BaseMessages.getString(PKG, "MySqlBulkLoader.Message.CONNECTED", data.dbDescription));

      // 3) Now we are ready to run the load command...
      //
      executeLoadCommand();
    } catch (Exception ex) {
      throw new HopException(ex);
    }

    return true;
  }

  private void executeLoadCommand() throws Exception {

    StringBuilder loadCommand = new StringBuilder();
    loadCommand.append(
        "LOAD DATA "
            + (meta.isLocalFile() ? "LOCAL" : "")
            + " INFILE '"
            + resolve(meta.getFifoFileName())
            + "' ");
    if (meta.isReplacingData()) {
      loadCommand.append("REPLACE ");
    } else if (meta.isIgnoringErrors()) {
      loadCommand.append("IGNORE ");
    }
    loadCommand.append("INTO TABLE " + data.schemaTable + " ");
    if (!Utils.isEmpty(resolve(meta.getLoadCharSet()))) {
      loadCommand.append("CHARACTER SET " + resolve(meta.getLoadCharSet()) + " ");
    }
    String delStr = meta.getDelimiter();
    if ("\t".equals(delStr)) {
      delStr = "\\t";
    }

    loadCommand.append("FIELDS TERMINATED BY '" + delStr + "' ");
    if (!Utils.isEmpty(meta.getEnclosure())) {
      loadCommand.append("OPTIONALLY ENCLOSED BY '" + meta.getEnclosure() + "' ");
    }
    loadCommand.append(
        "ESCAPED BY '"
            + meta.getEscapeChar()
            + ("\\".equals(meta.getEscapeChar()) ? meta.getEscapeChar() : "")
            + "' ");

    // Build list of column names to set
    loadCommand.append("(");
    for (int cnt = 0; cnt < meta.getFields().size(); cnt++) {
      DatabaseMeta databaseMeta = getPipelineMeta().findDatabase(meta.getConnection(), variables);
      loadCommand.append(databaseMeta.quoteField(meta.getFields().get(cnt).getFieldTable()));
      if (cnt < meta.getFields().size() - 1) {
        loadCommand.append(",");
      }
    }

    loadCommand.append(");" + Const.CR);

    logBasic(
        BaseMessages.getString(
            PKG, "MySqlBulkLoader.Message.STARTING", data.dbDescription, loadCommand));

    data.sqlRunner = new SqlRunner(data, loadCommand.toString());
    data.sqlRunner.start();

    // Ready to start writing rows to the FIFO file now...
    //
    if (!Const.isWindows()) {
      logBasic(BaseMessages.getString(PKG, "MySqlBulkLoader.Message.OPENFIFO", data.fifoFilename));
      OpenFifo openFifo = new OpenFifo(data.fifoFilename, 1000);
      openFifo.start();

      // Wait for either the sql statement to throw an error or the
      // fifo writer to throw an error
      while (true) {
        openFifo.join(200);
        if (openFifo.getState() == Thread.State.TERMINATED) {
          break;
        }

        try {
          data.sqlRunner.checkExcn();
        } catch (Exception e) {
          // We need to open a stream to the fifo to unblock the fifo writer
          // that was waiting for the sqlRunner that now isn't running
          new BufferedInputStream(new FileInputStream(data.fifoFilename)).close();
          openFifo.join();
          logError(BaseMessages.getString(PKG, "MySqlBulkLoader.Message.ERRORFIFO"));
          logError("");
          throw e;
        }

        try {
          openFifo.checkExcn();
        } catch (Exception e) {
          throw e;
        }
      }
      data.fifoStream = openFifo.getFifoStream();
    }
  }

  @Override
  public boolean processRow() throws HopException {
    try {
      Object[] r = getRow(); // Get row from input rowset & set row busy!
      if (r == null) { // no more input to be expected...

        setOutputDone();

        closeOutput();

        return false;
      }

      if (first) {
        first = false;

        // Cache field indexes.
        //
        data.keynrs = new int[meta.getFields().size()];
        for (int i = 0; i < data.keynrs.length; i++) {
          data.keynrs[i] = getInputRowMeta().indexOfValue(meta.getFields().get(i).getFieldStream());
        }

        data.bulkFormatMeta = new IValueMeta[data.keynrs.length];
        for (int i = 0; i < data.keynrs.length; i++) {
          IValueMeta sourceMeta = getInputRowMeta().getValueMeta(data.keynrs[i]);
          if (sourceMeta.isDate()) {
            if (MySqlBulkLoaderMeta.getFieldFormatType(meta.getFields().get(i).getFieldFormatType())
                == MySqlBulkLoaderMeta.FIELD_FORMAT_TYPE_DATE) {
              data.bulkFormatMeta[i] = data.bulkDateMeta.clone();
            } else if (MySqlBulkLoaderMeta.getFieldFormatType(
                    meta.getFields().get(i).getFieldFormatType())
                == MySqlBulkLoaderMeta.FIELD_FORMAT_TYPE_TIMESTAMP) {
              data.bulkFormatMeta[i] = data.bulkTimestampMeta.clone(); // default to timestamp
            }
          } else if (sourceMeta.isNumeric()
              && MySqlBulkLoaderMeta.getFieldFormatType(
                      meta.getFields().get(i).getFieldFormatType())
                  == MySqlBulkLoaderMeta.FIELD_FORMAT_TYPE_NUMBER) {
            data.bulkFormatMeta[i] = data.bulkNumberMeta.clone();
          }

          if (data.bulkFormatMeta[i] == null && !sourceMeta.isStorageBinaryString()) {
            data.bulkFormatMeta[i] = sourceMeta.clone();
          }
        }

        // execute the client statement...
        //
        execute(meta);
      }

      // Every nr of rows we re-start the bulk load process to allow indexes etc to fit into the
      // MySql server memory
      // Performance could degrade if we don't do this.
      //
      if (data.bulkSize > 0 && getLinesOutput() > 0 && (getLinesOutput() % data.bulkSize) == 0) {
        closeOutput();
        executeLoadCommand();
      }

      writeRowToBulk(getInputRowMeta(), r);
      putRow(getInputRowMeta(), r);
      incrementLinesOutput();

      return true;
    } catch (Exception e) {
      logError(BaseMessages.getString(PKG, "MySqlBulkLoader.Log.ErrorInTransform"), e);
      setErrors(1);
      stopAll();
      setOutputDone(); // signal end to receiver(s)
      return false;
    }
  }

  private void closeOutput() throws Exception {

    if (data.fifoStream != null) {
      // Close the fifo file...
      //
      data.fifoStream.close();
      data.fifoStream = null;
    }

    if (data.sqlRunner != null) {

      // wait for the INSERT statement to finish and check for any error and/or warning...
      logDebug(
          "Waiting up to "
              + THREAD_WAIT_TIME_TEXT
              + " for the MySql load command thread to finish processing."); // no requirement for
      // NLS debug messages
      data.sqlRunner.join(THREAD_WAIT_TIME);
      SqlRunner sqlRunner = data.sqlRunner;
      data.sqlRunner = null;
      sqlRunner.checkExcn();
    }
  }

  private void writeRowToBulk(IRowMeta rowMeta, Object[] r) throws HopException {

    try {
      // So, we have this output stream to which we can write CSV data to.
      // Basically, what we need to do is write the binary data (from strings to it as part of this
      // proof of concept)
      //
      // The data format required is essentially:
      //
      for (int i = 0; i < data.keynrs.length; i++) {
        if (i > 0) {
          // Write a separator
          //
          data.fifoStream.write(data.separator);
        }

        int index = data.keynrs[i];
        IValueMeta valueMeta = rowMeta.getValueMeta(index);
        Object valueData = r[index];

        if (valueData == null) {
          data.fifoStream.write("NULL".getBytes());
        } else {
          switch (valueMeta.getType()) {
            case IValueMeta.TYPE_STRING:
              data.fifoStream.write(data.quote);
              if (valueMeta.isStorageBinaryString()
                  && MySqlBulkLoaderMeta.getFieldFormatType(
                          meta.getFields().get(i).getFieldFormatType())
                      == MySqlBulkLoaderMeta.FIELD_FORMAT_TYPE_OK) {
                // We had a string, just dump it back.
                data.fifoStream.write((byte[]) valueData);
              } else {
                String string = valueMeta.getString(valueData);
                if (string != null) {
                  if (MySqlBulkLoaderMeta.getFieldFormatType(
                          meta.getFields().get(i).getFieldFormatType())
                      == MySqlBulkLoaderMeta.FIELD_FORMAT_TYPE_STRING_ESCAPE) {
                    string =
                        Const.replace(
                            string,
                            meta.getEscapeChar(),
                            meta.getEscapeChar() + meta.getEscapeChar());
                    string =
                        Const.replace(
                            string,
                            meta.getEnclosure(),
                            meta.getEscapeChar() + meta.getEnclosure());
                  }
                  data.fifoStream.write(string.getBytes());
                }
              }
              data.fifoStream.write(data.quote);
              break;
            case IValueMeta.TYPE_INTEGER:
              if (valueMeta.isStorageBinaryString() && data.bulkFormatMeta[i] == null) {
                data.fifoStream.write(valueMeta.getBinaryString(valueData));
              } else {
                Long integer = valueMeta.getInteger(valueData);
                if (integer != null) {
                  data.fifoStream.write(data.bulkFormatMeta[i].getString(integer).getBytes());
                }
              }
              break;
            case IValueMeta.TYPE_DATE:
              if (valueMeta.isStorageBinaryString() && data.bulkFormatMeta[i] == null) {
                data.fifoStream.write(valueMeta.getBinaryString(valueData));
              } else {
                Date date = valueMeta.getDate(valueData);
                if (date != null) {
                  data.fifoStream.write(data.bulkFormatMeta[i].getString(date).getBytes());
                }
              }
              break;
            case IValueMeta.TYPE_BOOLEAN:
              if (valueMeta.isStorageBinaryString() && data.bulkFormatMeta[i] == null) {
                data.fifoStream.write(valueMeta.getBinaryString(valueData));
              } else {
                Boolean b = valueMeta.getBoolean(valueData);
                if (b != null) {
                  data.fifoStream.write(data.bulkFormatMeta[i].getString(b).getBytes());
                }
              }
              break;
            case IValueMeta.TYPE_NUMBER:
              if (valueMeta.isStorageBinaryString() && data.bulkFormatMeta[i] == null) {
                data.fifoStream.write((byte[]) valueData);
              } else {
                /**
                 * If this is the first line, reset default conversion mask for Number type
                 * (#.#;-#.#). This will make conversion mask to be calculated according to meta
                 * data (length, precision).
                 *
                 * <p>http://jira.pentaho.com/browse/PDI-11421
                 */
                if (getLinesWritten() == 0) {
                  data.bulkFormatMeta[i].setConversionMask(null);
                }

                Double d = valueMeta.getNumber(valueData);
                if (d != null) {
                  data.fifoStream.write(data.bulkFormatMeta[i].getString(d).getBytes());
                }
              }
              break;
            case IValueMeta.TYPE_BIGNUMBER:
              if (valueMeta.isStorageBinaryString() && data.bulkFormatMeta[i] == null) {
                data.fifoStream.write((byte[]) valueData);
              } else {
                BigDecimal bn = valueMeta.getBigNumber(valueData);
                if (bn != null) {
                  data.fifoStream.write(data.bulkFormatMeta[i].getString(bn).getBytes());
                }
              }
              break;
            default:
              break;
          }
        }
      }

      // finally write a newline
      //
      data.fifoStream.write(data.newline);

      if ((getLinesOutput() % 5000) == 0) {
        data.fifoStream.flush();
      }
    } catch (IOException e) {
      // If something went wrong with writing to the fifo, get the underlying error from MySql
      try {
        logError(
            BaseMessages.getString(PKG, "MySqlBulkLoader.Message.IOERROR", THREAD_WAIT_TIME_TEXT));
        try {
          data.sqlRunner.join(THREAD_WAIT_TIME);
        } catch (InterruptedException ex) {
          // Ignore errors
        }
        data.sqlRunner.checkExcn();
      } catch (Exception loadEx) {
        throw new HopException(BaseMessages.getString(PKG, MESSAGE_ERRORSERIALIZING), loadEx);
      }

      // MySql didn't finish, throw the generic "Pipe" exception.
      throw new HopException(BaseMessages.getString(PKG, MESSAGE_ERRORSERIALIZING), e);

    } catch (Exception e2) {
      // Null pointer exceptions etc.
      throw new HopException(BaseMessages.getString(PKG, MESSAGE_ERRORSERIALIZING), e2);
    }
  }

  protected void verifyDatabaseConnection() throws HopException {
    // Confirming Database Connection is defined.
    if (meta.getConnection() == null) {
      throw new HopException(
          BaseMessages.getString(PKG, "MySqlBulkLoaderMeta.GetSQL.NoConnectionDefined"));
    }
  }

  @Override
  public boolean init() {

    if (super.init()) {

      // Confirming Database Connection is defined.
      try {
        verifyDatabaseConnection();
      } catch (HopException ex) {
        logError(ex.getMessage());
        return false;
      }

      if (Utils.isEmpty(meta.getEnclosure())) {
        data.quote = new byte[] {};
      } else {
        data.quote = resolve(meta.getEnclosure()).getBytes();
      }
      if (Utils.isEmpty(meta.getDelimiter())) {
        data.separator = "\t".getBytes();
      } else {
        data.separator = resolve(meta.getDelimiter()).getBytes();
      }
      data.newline = Const.CR.getBytes();

      String realEncoding = resolve(meta.getEncoding());
      data.bulkTimestampMeta = new ValueMetaDate("timestampMeta");
      data.bulkTimestampMeta.setConversionMask("yyyy-MM-dd HH:mm:ss");
      data.bulkTimestampMeta.setStringEncoding(realEncoding);

      data.bulkDateMeta = new ValueMetaDate("dateMeta");
      data.bulkDateMeta.setConversionMask("yyyy-MM-dd");
      data.bulkDateMeta.setStringEncoding(realEncoding);

      data.bulkNumberMeta = new ValueMetaNumber("numberMeta");
      data.bulkNumberMeta.setConversionMask("#.#");
      data.bulkNumberMeta.setGroupingSymbol(",");
      data.bulkNumberMeta.setDecimalSymbol(".");
      data.bulkNumberMeta.setStringEncoding(realEncoding);

      data.bulkSize = Const.toLong(resolve(meta.getBulkSize()), -1L);

      // Schema-table combination...
      DatabaseMeta databaseMeta = getPipelineMeta().findDatabase(meta.getConnection(), variables);
      data.schemaTable =
          databaseMeta.getQuotedSchemaTableCombination(
              variables, resolve(meta.getSchemaName()), resolve(meta.getTableName()));

      return true;
    }
    return false;
  }

  @Override
  public void dispose() {

    // Close the output streams if still needed.
    //
    try {
      if (data.fifoStream != null) {
        data.fifoStream.close();
      }

      // Stop the SQL execution thread
      //
      if (data.sqlRunner != null) {
        data.sqlRunner.join();
        data.sqlRunner = null;
      }
      // Release the database connection
      //
      if (data.db != null) {
        data.db.disconnect();
        data.db = null;
      }

      // remove the fifo file...
      //
      try {
        if (data.fifoFilename != null) {
          new File(data.fifoFilename).delete();
        }
      } catch (Exception e) {
        logError(
            BaseMessages.getString(
                PKG, "MySqlBulkLoader.Message.UNABLETODELETE", data.fifoFilename),
            e);
      }
    } catch (Exception e) {
      setErrors(1L);
      logError(BaseMessages.getString(PKG, "MySqlBulkLoader.Message.UNEXPECTEDERRORCLOSING"), e);
    }

    super.dispose();
  }

  // Class to try and open a writer to a fifo in a different thread.
  // Opening the fifo is a blocking call, so we need to check for errors
  // after a small waiting period
  static class OpenFifo extends Thread {
    private BufferedOutputStream fifoStream = null;
    private Exception ex;
    private String fifoName;
    private int size;

    OpenFifo(String fifoName, int size) {
      this.fifoName = fifoName;
      this.size = size;
    }

    @Override
    public void run() {
      try {
        fifoStream =
            new BufferedOutputStream(new FileOutputStream(OpenFifo.this.fifoName), this.size);
      } catch (Exception exception) {
        this.ex = exception;
      }
    }

    void checkExcn() throws Exception {
      // This is called from the main thread context to rethrow any saved
      // excn.
      if (ex != null) {
        throw ex;
      }
    }

    BufferedOutputStream getFifoStream() {
      return fifoStream;
    }
  }

  static class SqlRunner extends Thread {
    private MySqlBulkLoaderData data;

    private String loadCommand;

    private Exception ex;

    SqlRunner(MySqlBulkLoaderData data, String loadCommand) {
      this.data = data;
      this.loadCommand = loadCommand;
    }

    @Override
    public void run() {
      try {
        data.db.execStatement(loadCommand);
      } catch (Exception exception) {
        this.ex = exception;
      }
    }

    void checkExcn() throws Exception {
      // This is called from the main thread context to rethrow any saved
      // excn.
      if (ex != null) {
        throw ex;
      }
    }
  }
}
