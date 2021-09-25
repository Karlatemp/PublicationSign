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

clear

function splitLine() {
  echo "########################################################"
}

splitLine

echo "GPG CI Key Generator v0.0.1"
echo "This script is licensed under the MIT LICENSE"
echo "                Copyright 2017-2021 Karlatemp"
echo "@source https://github.com/Karlatemp/PublicationSign"

splitLine

workflow=key-gen-workflow

echo "Setting up workflow....."
splitLine

rm -rf $workflow
mkdir $workflow

function sgpg() {
    gpg --homedir $workflow "$@"
}
sgpg --version

if [ $? != 0 ]; then
  echo "gpg not found. Please install gpg first."
  exit $?
fi

splitLine

echo "Please select generation mode"
echo "[N] Generating a new GPG key"
echo "[I] Export exists GPG key from your local key storage"
read -r -p '> ' runMode

if [ "$runMode" == 'N' ]; then
  clear
  splitLine
  echo "Generating a new GPG key"
  echo "Please don't setup key password"
  splitLine
  sleep 3
  sgpg --default-new-key-algo rsa4096 --gen-key
  echo "Saving keys to keys.pri & keys.pub"
  sgpg -a --export-secret-keys --output keys.pri
  sgpg -a --export --output keys.pub

elif [ "$runMode" == 'I' ]; then
  clear
  splitLine
  gpg --list-keys --keyid-format LONG
  splitLine
  echo "Listed your keys. Please enter the key want to export"
  read -r -p '> ' keyId
  gpg --export-secret-keys "$keyId" > $workflow/tmp.gpg
  gpg --export "$keyId" > $workflow/tmp.gpg.pub
  sgpg --batch --import $workflow/tmp.gpg
  sgpg --batch --import $workflow/tmp.gpg.pub
  clear
  splitLine
  echo "Need clear the password. Please clear the password"
  echo "After passwd clear; type 'quit' for quit gpg edit key"
  splitLine
  sleep 5
  sgpg --edit-key "$keyId" passwd

  echo "Saving keys to keys.pri & keys.pub"
  sgpg -a --export-secret-keys --output keys.pri
  sgpg -a --export --output keys.pub

else
  echo "Unknown mode $runMode"
  exit 5
fi

clear
splitLine

echo "Your key was generated. Keep your private key security"
echo "Please upload your public key to a key server"
echo "Eg. keyserver.ubuntu.com, keys.openpgp.org, pgp.mit.edu"
echo "@see https://central.sonatype.org/publish/requirements/gpg/#distributing-your-public-key"
splitLine

cat keys.pub
