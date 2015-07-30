/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.experiments.benchmarks.common;

import io.hops.experiments.controller.commands.WarmUpCommand;

/**
 *
 * @author salman
 */
public class NamespaceWarmUp {

    public static class Request implements WarmUpCommand.Request {
        private BenchmarkType benchMarkType;
        private int filesToCreate;
        private short replicationFactor;
        private long fileSize;
        private String baseDir;
        

        public Request(BenchmarkType benchMarkType, int filesToCreate,
                short replicationFactor, long fileSize,String baseDir) {
            this.benchMarkType = benchMarkType;
            this.filesToCreate = filesToCreate;
            this.replicationFactor = replicationFactor;
            this.fileSize = fileSize;
            this.baseDir = baseDir;
        }

        public String getBaseDir() {
            return baseDir;
        }

        public int getFilesToCreate() {
            return filesToCreate;
        }
        
        
        public BenchmarkType getBenchMarkType() {
            return benchMarkType;
        }

        public short getReplicationFactor() {
            return replicationFactor;
        }

        public long getFileSize() {
            return fileSize;
        }

    }
  
   public static class Response implements WarmUpCommand.Response {
       
   }
}
