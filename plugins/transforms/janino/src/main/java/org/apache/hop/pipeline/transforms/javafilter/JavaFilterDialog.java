/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.pipeline.transforms.javafilter;

import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transforms.janino.editor.FormulaEditor;
import org.apache.hop.pipeline.transforms.util.JaninoCheckerUtil;
import org.apache.hop.ui.core.PropsUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.dialog.MessageBox;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.StyledTextComp;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class JavaFilterDialog extends BaseTransformDialog {
  private static final Class<?> PKG = JavaFilterMeta.class;

  private Text wTransformName;
  private CCombo wTrueTo;
  private CCombo wFalseTo;
  private StyledTextComp wCondition;
  private Button wEditor;

  private final JavaFilterMeta input;

  private final List<String> inputFields = new ArrayList<>();
  private ColumnInfo[] colinf;

  public JavaFilterDialog(
      Shell parent, IVariables variables, JavaFilterMeta transformMeta, PipelineMeta pipelineMeta) {
    super(parent, variables, transformMeta, pipelineMeta);

    // The order here is important... currentMeta is looked at for changes
    input = transformMeta;
  }

  @Override
  public String open() {
    Shell parent = getParent();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
    PropsUi.setLook(shell);
    setShellImage(shell, input);

    ModifyListener lsMod = e -> input.setChanged();
    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = PropsUi.getFormMargin();
    formLayout.marginHeight = PropsUi.getFormMargin();

    shell.setLayout(formLayout);
    shell.setText(BaseMessages.getString(PKG, "JavaFilterDialog.DialogTitle"));

    int middle = props.getMiddlePct();
    int margin = PropsUi.getMargin();

    // TransformName line
    wlTransformName = new Label(shell, SWT.RIGHT);
    wlTransformName.setText(BaseMessages.getString(PKG, "System.TransformName.Label"));
    wlTransformName.setToolTipText(BaseMessages.getString(PKG, "System.TransformName.Tooltip"));
    PropsUi.setLook(wlTransformName);
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment(0, 0);
    fdlTransformName.right = new FormAttachment(middle, -margin);
    fdlTransformName.top = new FormAttachment(0, margin);
    wlTransformName.setLayoutData(fdlTransformName);
    wTransformName = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
    wTransformName.setText(transformName);
    PropsUi.setLook(wTransformName);
    wTransformName.addModifyListener(lsMod);
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment(middle, 0);
    fdTransformName.top = new FormAttachment(0, margin);
    fdTransformName.right = new FormAttachment(100, 0);
    wTransformName.setLayoutData(fdTransformName);

    // Some buttons
    wOk = new Button(shell, SWT.PUSH);
    wOk.setText(BaseMessages.getString(PKG, "System.Button.OK"));
    wCancel = new Button(shell, SWT.PUSH);
    wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

    setButtonPositions(new Button[] {wOk, wCancel}, margin, null);

    Group wSettingsGroup = new Group(shell, SWT.SHADOW_NONE);
    PropsUi.setLook(wSettingsGroup);
    wSettingsGroup.setText(BaseMessages.getString(PKG, "JavaFIlterDialog.Settings.Label"));
    FormLayout settingsLayout = new FormLayout();
    settingsLayout.marginWidth = 10;
    settingsLayout.marginHeight = 10;
    wSettingsGroup.setLayout(settingsLayout);

    // Send 'True' data to...
    Label wlTrueTo = new Label(wSettingsGroup, SWT.RIGHT);
    wlTrueTo.setText(BaseMessages.getString(PKG, "JavaFilterDialog.SendTrueTo.Label"));
    PropsUi.setLook(wlTrueTo);
    FormData fdlTrueTo = new FormData();
    fdlTrueTo.left = new FormAttachment(0, 0);
    fdlTrueTo.right = new FormAttachment(middle, -margin);
    fdlTrueTo.top = new FormAttachment(wTransformName, margin);
    wlTrueTo.setLayoutData(fdlTrueTo);
    wTrueTo = new CCombo(wSettingsGroup, SWT.BORDER);
    PropsUi.setLook(wTrueTo);

    TransformMeta transforminfo = pipelineMeta.findTransform(transformName);
    if (transforminfo != null) {
      List<TransformMeta> nextTransforms = pipelineMeta.findNextTransforms(transforminfo);
      for (TransformMeta transformMeta : nextTransforms) {
        wTrueTo.add(transformMeta.getName());
      }
    }

    wTrueTo.addModifyListener(lsMod);
    FormData fdTrueTo = new FormData();
    fdTrueTo.left = new FormAttachment(middle, 0);
    fdTrueTo.top = new FormAttachment(wTransformName, margin);
    fdTrueTo.right = new FormAttachment(100, 0);
    wTrueTo.setLayoutData(fdTrueTo);

    // Send 'False' data to...
    Label wlFalseTo = new Label(wSettingsGroup, SWT.RIGHT);
    wlFalseTo.setText(BaseMessages.getString(PKG, "JavaFilterDialog.SendFalseTo.Label"));
    PropsUi.setLook(wlFalseTo);
    FormData fdlFalseTo = new FormData();
    fdlFalseTo.left = new FormAttachment(0, 0);
    fdlFalseTo.right = new FormAttachment(middle, -margin);
    fdlFalseTo.top = new FormAttachment(wTrueTo, margin);
    wlFalseTo.setLayoutData(fdlFalseTo);
    wFalseTo = new CCombo(wSettingsGroup, SWT.BORDER);
    PropsUi.setLook(wFalseTo);

    transforminfo = pipelineMeta.findTransform(transformName);
    if (transforminfo != null) {
      List<TransformMeta> nextTransforms = pipelineMeta.findNextTransforms(transforminfo);
      for (TransformMeta transformMeta : nextTransforms) {
        wFalseTo.add(transformMeta.getName());
      }
    }

    wFalseTo.addModifyListener(lsMod);
    FormData fdFalseFrom = new FormData();
    fdFalseFrom.top = new FormAttachment(wTrueTo, margin);
    fdFalseFrom.left = new FormAttachment(middle, 0);
    fdFalseFrom.right = new FormAttachment(100, 0);
    wFalseTo.setLayoutData(fdFalseFrom);

    // Condition field
    Label wlCondition = new Label(wSettingsGroup, SWT.RIGHT);
    wlCondition.setText(BaseMessages.getString(PKG, "JavaFIlterDialog.Condition.Label"));
    PropsUi.setLook(wlCondition);
    FormData fdlCondition = new FormData();
    fdlCondition.top = new FormAttachment(wFalseTo, margin);
    fdlCondition.left = new FormAttachment(0, 0);
    fdlCondition.right = new FormAttachment(middle, -margin);
    wlCondition.setLayoutData(fdlCondition);
    wCondition =
        new StyledTextComp(
            variables,
            wSettingsGroup,
            SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    PropsUi.setLook(wCondition);
    wCondition.addModifyListener(lsMod);
    FormData fdCondition = new FormData();
    fdCondition.top = new FormAttachment(wFalseTo, margin);
    fdCondition.left = new FormAttachment(middle, 0);
    fdCondition.right = new FormAttachment(100, 0);
    wCondition.setLayoutData(fdCondition);

    wEditor = new Button(wSettingsGroup, SWT.PUSH | SWT.CENTER);
    wEditor.setText(BaseMessages.getString(PKG, "JavaFilterDialog.Editor.Button"));
    PropsUi.setLook(wEditor);
    FormData fdEditor = new FormData();
    fdEditor.top = new FormAttachment(wCondition, margin);
    fdEditor.left = new FormAttachment(middle, 0);
    wEditor.setLayoutData(fdEditor);

    FormData fdSettingsGroup = new FormData();
    fdSettingsGroup.left = new FormAttachment(0, margin);
    fdSettingsGroup.top = new FormAttachment(wTransformName, margin);
    fdSettingsGroup.right = new FormAttachment(100, -margin);
    fdSettingsGroup.bottom = new FormAttachment(wOk, -margin);
    wSettingsGroup.setLayoutData(fdSettingsGroup);

    //
    // Search the fields in the background
    //
    final Runnable runnable =
        () -> {
          TransformMeta transformMeta = pipelineMeta.findTransform(transformName);
          if (transformMeta != null) {
            try {
              IRowMeta row = pipelineMeta.getPrevTransformFields(variables, transformMeta);

              // Remember these fields...
              for (int i = 0; i < row.size(); i++) {
                inputFields.add(row.getValueMeta(i).getName());
              }
            } catch (HopException e) {
              logError(BaseMessages.getString(PKG, "JaninoDialog.Log.UnableToFindInput"));
            }
          }
        };
    new Thread(runnable).start();

    // Add listeners
    wCancel.addListener(SWT.Selection, e -> cancel());
    wOk.addListener(SWT.Selection, e -> ok());
    wEditor.addListener(SWT.Selection, e -> editorDialog());

    getData();
    input.setChanged(changed);

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return transformName;
  }

  /** Copy information from the meta-data currentMeta to the dialog fields. */
  public void getData() {

    wTrueTo.setText(Const.NVL(input.getTrueTransform(), ""));
    wFalseTo.setText(Const.NVL(input.getFalseTransform(), ""));
    wCondition.setText(Const.NVL(input.getCondition(), ""));

    wTransformName.selectAll();
    wTransformName.setFocus();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(changed);
    dispose();
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }

    // Check if code contains content that is not allowed
    JaninoCheckerUtil janinoCheckerUtil = new JaninoCheckerUtil();
    List<String> codeCheck = janinoCheckerUtil.checkCode(wCondition.getText());
    if (!codeCheck.isEmpty()) {
      MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
      mb.setText("Invalid Code");
      mb.setMessage("Script contains code that is not allowed : " + codeCheck);
      mb.open();
      return;
    }

    transformName = wTransformName.getText(); // return value

    input.setCondition(wCondition.getText());
    input.setTrueTransform(Const.NVL(wTrueTo.getText(), null));
    input.setFalseTransform(Const.NVL(wFalseTo.getText(), null));

    dispose();
  }

  private void editorDialog() {
    try {
      if (!shell.isDisposed()) {
        FormulaEditor libFormulaEditor =
            new FormulaEditor(
                variables,
                shell,
                SWT.APPLICATION_MODAL | SWT.SHEET,
                Const.NVL(wCondition.getText(), ""),
                inputFields);
        String formula = libFormulaEditor.open();
        if (formula != null) {
          wCondition.setText(formula);
        }
      }
    } catch (Exception ex) {
      new ErrorDialog(shell, "Error", "There was an unexpected error in the formula editor", ex);
    }
  }
}
