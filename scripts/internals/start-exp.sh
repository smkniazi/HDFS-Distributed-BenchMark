#!/bin/bash
# Copyright (C) 2022 HopsWorks.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
PSSH=$($DIR/psshcmd.sh)
PRSYNC=$($DIR/prsynccmd.sh)

if test -z "$1"
then
  echo "Provide host name where master will be started"
  exit 0
fi


echo "Starting Slaves on ${BM_Machines_FullList[*]}"
$PSSH -H "${BM_Machines_FullList[*]}"  -l $HopsFS_User -i  $HopsFS_Experiments_Remote_Dist_Folder/start-slave.sh 

echo "sleeping for a while to make sure that the slaves have initialized"
sleep 5

connectStr="$HopsFS_User@$1"
echo "loading new master properties files on $1"
scp ./master.properties $connectStr:$HopsFS_Experiments_Remote_Dist_Folder
echo "Starting Experiment Master on $1"
ssh $connectStr $HopsFS_Experiments_Remote_Dist_Folder/start-master.sh 








