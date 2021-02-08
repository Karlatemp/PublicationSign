#!/usr/bin/env bash

#
# Copyright (c) 2018-2021 Karlatemp. All rights reserved.
# @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
#
# PublicationSign/PublicationSign/key-gen.sh
#
# Use of this source code is governed by the MIT license that can be found via the following link.
#
# https://github.com/Karlatemp/PublicationSign/blob/master/LICENSE
#

echo GPG CI Key Generator v0.0.0 by Karlatemp

workflow=key-gen-workflow

echo
echo "Setting up workflow"

rm -rf $workflow
mkdir $workflow

function sgpg() {
    gpg --homedir $workflow "$@"
}
sgpg --version

echo
echo "Please select gen mode"
echo "N for Generating a new GPG key"
echo "I for Export key from current user"
read -r -p '> ' runMode

if [ "$runMode" == 'N' ]; then
  echo "N Mode"
  echo "Generating a new GPG key"
  echo "Please don't setup key password"
  sleep 3
  sgpg --default-new-key-algo rsa4096 --gen-key
  echo "Saving keys to keys.pri & keys.pub"
  sgpg -a --export-secret-keys --output keys.pri
  sgpg -a --export --output keys.pub

elif [ "$runMode" == 'I' ]; then
  echo "I Mode"
  gpg --list-keys --keyid-format LONG
  echo "Listed your keys. Please enter the key want to export"
  read -r -p '> ' keyId
  gpg --export-secret-keys "$keyId" > $workflow/tmp.gpg
  gpg --export "$keyId" > $workflow/tmp.gpg.pub
  sgpg --batch --import $workflow/tmp.gpg
  sgpg --batch --import $workflow/tmp.gpg.pub
  echo "Need clear the password. Please clear the password"
  echo "After passwd clear; type 'quit' for quit gpg edit key"
  sleep 5
  sgpg --edit-key "$keyId" passwd

  echo "Saving keys to keys.pri & keys.pub"
  sgpg -a --export-secret-keys --output keys.pri
  sgpg -a --export --output keys.pub

else
  echo "Unknown mode $runMode"
fi
