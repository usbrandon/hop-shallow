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
 *
 */

package org.apache.hop.workflow.actions.checkfilelocked;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.hop.core.HopClientEnvironment;
import org.apache.hop.metadata.serializer.memory.MemoryMetadataProvider;
import org.apache.hop.workflow.action.ActionSerializationTestUtil;
import org.junit.Assert;
import org.junit.Test;

public class ActionCheckFilesLockedTest {

  @Test
  public void testSerialization() throws Exception {
    HopClientEnvironment.init();
    MemoryMetadataProvider provider = new MemoryMetadataProvider();

    ActionCheckFilesLocked action =
        ActionSerializationTestUtil.testSerialization(
            "/check-files-locked-action.xml", ActionCheckFilesLocked.class, provider);

    assertFalse(action.isArgFromPrevious());
    assertTrue(action.isIncludeSubfolders());
    Assert.assertEquals(2, action.getCheckedFiles().size());
    Assert.assertEquals("/tmp/folder1", action.getCheckedFiles().get(0).getName());
    Assert.assertEquals(".*\\.txt$", action.getCheckedFiles().get(0).getWildcard());
    Assert.assertEquals("/tmp/folder2", action.getCheckedFiles().get(1).getName());
    Assert.assertEquals(".*\\.zip$", action.getCheckedFiles().get(1).getWildcard());
  }
}
