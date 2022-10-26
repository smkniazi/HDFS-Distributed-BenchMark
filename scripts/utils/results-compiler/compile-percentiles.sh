#!/bin/bash
#
#   Licensed to the Apache Software Foundation (ASF) under one or more
#   contributor license agreements.  See the NOTICE file distributed with
#   this work for additional information regarding copyright ownership.
#   The ASF licenses this file to You under the Apache License, Version 2.0
#   (the "License"); you may not use this file except in compliance with
#   the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
# Author: Salman Niazi 2015

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

if [ "$#" -ne 4 ]; then
    echo "Illegal number of parameters. Usage  compile-percentiles {src} {dst} {file prefix for generated files} {no of threads}"
    exit 0
fi

date1=$(date +"%s")
java -Xmx5g -cp $DIR/hammer-bench.jar  io.hops.experiments.results.compiler.CalculatePercentiles $1 $2 $3 $4
date2=$(date +"%s")
diff=$(($date2-$date1))
echo "Execution Time $currentExpDir $(($diff / 60)) minutes and $(($diff % 60)) seconds."

