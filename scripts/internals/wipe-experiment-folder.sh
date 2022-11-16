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


if [ -z $BM_Machines_FullList ]; then
  DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
  echo "Loading params "
  source $DIR/../experiment-env.sh
fi

#All Unique Hosts
All_Hosts=${BM_Machines_FullList[*]}
All_Unique_Hosts=$(echo "${All_Hosts[@]}" | tr ' ' '\n' | sort -u | tr '\n' ' ')

for i in ${All_Unique_Hosts[@]}
do
  connectStr="$HopsFS_User@$i"
  echo "Deleting Experiment Folder $Folder on $i"
  ssh $connectStr rm -rf  $HopsFS_Experiments_Remote_Dist_Folder
done





