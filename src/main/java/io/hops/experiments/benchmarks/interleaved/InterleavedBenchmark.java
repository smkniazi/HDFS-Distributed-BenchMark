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
package io.hops.experiments.benchmarks.interleaved;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import io.hops.experiments.benchmarks.common.NamespaceWarmUp;
import io.hops.experiments.coin.MultiFaceCoin;
import io.hops.experiments.controller.Logger;
import io.hops.experiments.controller.commands.WarmUpCommand;
import io.hops.experiments.utils.BenchmarkUtils;
import io.hops.experiments.workload.generator.FilePool;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import io.hops.experiments.benchmarks.common.Benchmark;
import io.hops.experiments.controller.commands.BenchmarkCommand;
import io.hops.experiments.benchmarks.common.BenchmarkOperations;

/**
 *
 * @author salman
 */
public class InterleavedBenchmark extends Benchmark {

    private long duration;
    private long startTime = 0;
    AtomicLong operationsCompleted = new AtomicLong(0);
    AtomicLong operationsFailed = new AtomicLong(0);
    AtomicLong createOperations = new AtomicLong(0);
    AtomicLong readOperations = new AtomicLong(0);
    AtomicLong fileStatOperations = new AtomicLong(0);
    AtomicLong dirStatOperations = new AtomicLong(0);
    AtomicLong renameOperations = new AtomicLong(0);
    AtomicLong deleteOperations = new AtomicLong(0);
    AtomicLong chmodFileOperations = new AtomicLong(0);
    AtomicLong chmodDirOperations = new AtomicLong(0);
    AtomicLong mkdirOperations = new AtomicLong(0);

    private ExecutorService executor;
    public InterleavedBenchmark(Configuration conf, int numThreads) {
        super(conf, numThreads);
        this.executor = Executors.newFixedThreadPool(numThreads);
    }

    @Override
    protected WarmUpCommand.Response warmUp(WarmUpCommand.Request warmUpCommand)
        throws IOException, InterruptedException {
        NamespaceWarmUp.Request namespaceWarmUp = (NamespaceWarmUp.Request) warmUpCommand;
        List workers = new ArrayList<WarmUp>();
        for (int i = 0; i < numThreads; i++) {
            Callable worker = new WarmUp(namespaceWarmUp.getFilesToCreate(),
                namespaceWarmUp.getReplicationFactor(), namespaceWarmUp
                .getFileSize(), namespaceWarmUp.getBaseDir());
            workers.add(worker);
        }
        executor.invokeAll(workers); // blocking call
        return new NamespaceWarmUp.Response();
    }

    public class WarmUp implements Callable {

        private DistributedFileSystem dfs;
        private FilePool filePool;
        private int filesToCreate;
        private short replicationFactor;
        private long fileSize;
        private String baseDir;

        public WarmUp(int filesToCreate, short replicationFactor, long fileSize, String baseDir) throws IOException {
            this.filesToCreate = filesToCreate;
            this.replicationFactor = replicationFactor;
            this.fileSize = fileSize;
            this.baseDir = baseDir;
        }

        @Override
        public Object call() throws Exception {
            dfs = BenchmarkUtils.getDFSClient(conf);
            filePool = BenchmarkUtils.getFilePool(conf, baseDir);

            String filePath = null;

            for (int i = 0; i < filesToCreate; i++) {
                try {
                    filePath = filePool.getFileToCreate();
                    BenchmarkUtils
                        .createFile(dfs, new Path(filePath), replicationFactor,
                            fileSize);
                    filePool.fileCreationSucceeded(filePath);
                    BenchmarkUtils.readFile(dfs, new Path(filePath), fileSize);
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.error(e);
                }
            }
            return null;
        }
    };

    @Override
    protected BenchmarkCommand.Response processCommandInternal(BenchmarkCommand.Request
        command) throws IOException, InterruptedException {
        InterleavedBenchmarkCommand.Request req = (InterleavedBenchmarkCommand
            .Request) command;
        duration = req.getDuration();

        List workers = new ArrayList<Worker>();
        for (int i = 0; i < numThreads; i++) {
            Callable worker = new Worker(req);
            workers.add(worker);
        }
        startTime = System.currentTimeMillis();
        executor.invokeAll(workers); // blocking call
        long totalTime = System.currentTimeMillis() - startTime;

        double speed = (operationsCompleted.get() / (double) totalTime) * 1000;

        InterleavedBenchmarkCommand.Response response =
            new InterleavedBenchmarkCommand.Response(totalTime, operationsCompleted.get(), operationsFailed.get(), speed);
        return response;
    }

    public class Worker implements Callable {

        private DistributedFileSystem dfs;
        private FilePool filePool;
        private InterleavedBenchmarkCommand.Request req;
        private MultiFaceCoin coin;

        public Worker(InterleavedBenchmarkCommand.Request req) throws IOException {
            this.req = req;
        }

        @Override
        public Object call() throws Exception {
            dfs = BenchmarkUtils.getDFSClient(conf);
            filePool = BenchmarkUtils.getFilePool(conf, req.getBaseDir());
            coin = new MultiFaceCoin(req.getCreatePercent(), req.getReadPercent(), req.getRenamePercent(), req.getDeletePercent(), 
                    req.getFileStatPercent(),req.getDirStatPercent(), 
                    req.getChmodFilePercent(), req.getChmodDirsPercent(), req.getMkdirPercent());
            String filePath = null;

            while (true) {
                try {
                    if ((System.currentTimeMillis() - startTime) > duration) {
                        return null;
                    }

                    BenchmarkOperations op = coin.flip();

                    switch (op) {
                        case CREATE_FILE:
                            createFile();
                            break;
                        case READ_FILE:
                            readFile();
                            break;
                        case LS_FILE:
                            statFile();
                            break;
                            case LS_DIR:
                            statDir();
                            break;
                        case RENAME_FILE:
                            renameFile();
                            break;
                        case DELETE_FILE:
                            deleteFile();
                            break;
                        case CHMOD_FILE:
                        case CHMOD_DIR:
                            chmod(op == BenchmarkOperations.CHMOD_DIR);
                            break;
                        case MKDIRS:
                            mkdir();
                            break;
                        default: {
                            throw new UnsupportedOperationException("Operation not supported");
                        }
                    }

                    if (Logger.canILog()) {
                        String message = "Total Completed Ops: " + operationsCompleted +" "+
                                "Total Failed Ops: " + operationsFailed +" "+
                                "Speed: " + speedPSec(operationsCompleted.get(), startTime)+" ops/s "+
                                " Creates: "+createOperations+" ["+BenchmarkUtils.round(((double)createOperations.get()/operationsCompleted.get())*100)+"%] "+
                                " Mkdirs: "+mkdirOperations+" ["+BenchmarkUtils.round(((double)mkdirOperations.get()/operationsCompleted.get())*100)+"%] "+
                                " Reads: "+readOperations+" ["+BenchmarkUtils.round(((double)readOperations.get()/operationsCompleted.get())*100)+"%] "+
                                " file Stat: "+fileStatOperations+" ["+BenchmarkUtils.round(((double)fileStatOperations.get()/operationsCompleted.get())*100)+"%] "+
                                " dir Stat: "+dirStatOperations+" ["+BenchmarkUtils.round(((double)dirStatOperations.get()/operationsCompleted.get())*100)+"%] "+
                                " Rename: "+renameOperations+" ["+BenchmarkUtils.round(((double)renameOperations.get()/operationsCompleted.get())*100)+"%] "+
                                " Chmod (File): "+chmodFileOperations+" ["+BenchmarkUtils.round(((double)chmodFileOperations.get()/operationsCompleted.get())*100)+"%] "+
                                " Chmod (Dir): "+chmodDirOperations+" ["+BenchmarkUtils.round(((double)chmodDirOperations.get()/operationsCompleted.get())*100)+"%] "+
                                " Delete: "+deleteOperations+" ["+BenchmarkUtils.round(((double)deleteOperations.get()/operationsCompleted.get())*100)+"%] "
                                ;
                        
                        Logger.printMsg(message);
                    }
                } catch (Exception e) {
                    Logger.error(e);
                }
            }
        }

        private void createFile() throws IOException {
            String file = filePool.getFileToCreate();
            if (file != null) {
                try {
                    BenchmarkUtils.createFile(dfs, new Path(file),
                        req.getReplicationFactor(), req.getFileSize());
                    filePool.fileCreationSucceeded(file);
                    operationsCompleted.incrementAndGet();
                    createOperations.incrementAndGet();
                } catch (Exception e) {
                    Logger.error(e);
                    operationsFailed.incrementAndGet();
                }
            } else {
                Logger.printMsg("Could not Create File. Got Null from the file pool");
            }
        }

        private void statDir() throws IOException {
            String file = filePool.getDirToStat();
            if (file != null) {
                try {
                    BenchmarkUtils.ls(dfs, new Path(file));
                    operationsCompleted.incrementAndGet();
                    dirStatOperations.incrementAndGet();
                } catch (Exception e) {
                    Logger.error(e);
                    operationsFailed.incrementAndGet();
                }
            } else {
                Logger.printMsg("Could not list path. Got Null from the file pool");
            }
        }
        
        private void statFile() throws IOException {
            String file = filePool.getFileToStat();
            if (file != null) {
                try {
                    BenchmarkUtils.ls(dfs, new Path(file));
                    operationsCompleted.incrementAndGet();
                    fileStatOperations.incrementAndGet();
                } catch (Exception e) {
                    Logger.error(e);
                    operationsFailed.incrementAndGet();
                }
            } else {
                Logger.printMsg("Could not list path. Got Null from the file pool");
            }
        }


        private void readFile() throws IOException {
            String file = filePool.getFileToRead();
            if (file != null) {
                try {
                    BenchmarkUtils
                        .readFile(dfs, new Path(file), req.getFileSize());
                    operationsCompleted.incrementAndGet();
                    readOperations.incrementAndGet();
                } catch (Exception e) {
                    Logger.error(e);
                    operationsFailed.incrementAndGet();
                }
            } else {
                Logger.printMsg("Could not read path. Got Null from the file pool");
            }
        }

        private void renameFile() throws IOException {
            String from = filePool.getFileToRename();
            if (from != null) {
                try {
                    String to = from + "_rend";
                    BenchmarkUtils.renameFile(dfs, new Path(from), new Path(to));
                    operationsCompleted.incrementAndGet();
                    filePool.fileRenamed(from, to);
                    renameOperations.incrementAndGet();
                } catch (Exception e) {
                    Logger.error(e);
                    operationsFailed.incrementAndGet();
                }
            } else {
                Logger.printMsg("Could not rename path. Got Null from the file pool");
            }
        }

        private void deleteFile() throws IOException {
            String file = filePool.getFileToDelete();
            if (file != null) {
                try {
                    BenchmarkUtils.deleteFile(dfs, new Path(file));
                    operationsCompleted.incrementAndGet();
                    deleteOperations.incrementAndGet();
                } catch (Exception e) {
                    Logger.error(e);
                    operationsFailed.incrementAndGet();
                }
            } else {
                Logger.printMsg("Could not delete path. Got Null from the file pool");
            }
        }
        
        
        private void chmod(boolean isDirOp) throws IOException {
            String path = null;
            if(isDirOp){
              path = filePool.getDirPathToChangePermissions();
            }else{
              path = filePool.getFilePathToChangePermissions();
            }
            if (path != null) {
                try {
                    BenchmarkUtils.chmodPath(dfs, new Path(path));
                    operationsCompleted.incrementAndGet();
                    
                    if(isDirOp){
                      chmodDirOperations.incrementAndGet();
                    }else{
                      chmodFileOperations.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    Logger.error(e);
                    operationsFailed.incrementAndGet();
                }
            } else {
                Logger.printMsg("Could not chmod path. Got Null from the file pool");
            }
        }
        
        private void mkdir() throws IOException {
            String dir = filePool.getDirToCreate();
            if (dir != null) {
                try {
                    BenchmarkUtils.mkdirs(dfs, new Path(dir));
                    operationsCompleted.incrementAndGet();
                    mkdirOperations.incrementAndGet();
                } catch (Exception e) {
                    Logger.error(e);
                    operationsFailed.incrementAndGet();
                }
            } else {
                Logger.printMsg("Could not chmod path. Got Null from the file pool");
            }
        }
    }

    private double speedPSec(long ops, long startTime) {
        long timePassed = (System.currentTimeMillis() - startTime);
        double opsPerMSec = (double) (ops) / (double) timePassed;
        return BenchmarkUtils.round(opsPerMSec * 1000);
    }
}
