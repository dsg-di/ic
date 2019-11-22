#!/bin/bash

if [ "$#" -lt 3 ]; then
  echo "Usage ./keyGeneration <path_to_store_keys> <path_to_Ca-baka_script> <number_of_certs> [<key_length>]"
  exit -1
fi

if [[ ! "$1" = /* ]] && [[ ! "$2" = /* ]]; then
  echo "Please use absolute paths instead..."
  exit -1
fi

if [ ! -d "$2" ]; then
  echo "There is no repo CA-baka. Please clone the repo from https://github.com/SethRobertson/CA-baka";
  exit -1
fi

if [ ! -d "$1" ]; then
  echo "Generating CA directory..."
  mkdir -p $1
else
  echo "Cleaning CA directory..."
  rm -rf $1/ca
fi

CAPATH=$1/ca
SCRIPT=$2/CA-baka
NUMBER_OF_CERTS=$3
KEY_LENGHT=2048

if [ ! -z "$4" ]; then
  KEY_LENGHT=$4;
fi
echo "Using key length of size $KEY_LENGHT";

#generate certficate authority
echo "Generating Certificate authority...."
$SCRIPT/CA-baka --keylen $KEY_LENGHT --workdir $CAPATH --country GR --state Attica --locality "Athens" --organization "DSG Group at di.uoa.gr" --organizationalunit "Developers" --newca "dsg.di.uoa.gr CA" ""

echo "Generating Certificate for Server/Client..."
for (( i=0; i < ${NUMBER_OF_CERTS}; i++ )); 
do
  echo Generating client-server key and certificate for node $i
  $SCRIPT/CA-baka --keylen $KEY_LENGHT --workdir $CAPATH --newclient "node${i}c" "node${i}.dsg.di.uoa.gr"
  $SCRIPT/CA-baka --keylen $KEY_LENGHT --workdir $CAPATH --newserver "node${i}" "node${i}.dsg.di.uoa.gr"
done

echo "Exporting CA certificate..."
openssl x509 -in $CAPATH/ca.crt -out $CAPATH/ca.crt.pem -outform PEM

cd $CAPATH/archive/
for ((i=0; i < ${NUMBER_OF_CERTS}; i++));
do
   echo "Converting to pem format the key and cert for node $i"
   cd node${i}c
     openssl x509 -in client.crt -out node${i}c.crt.pem -outform PEM
     openssl pkcs12 -in client.p12 -nocerts -out temp.key.pem -passin pass:mypass -passout pass:password
     openssl pkcs8 -in temp.key.pem -topk8 -out node${i}cP8.key.pem -passin pass:password -passout pass:password
     rm temp.key.pem
   cd ..
   cd node${i}
     openssl x509 -in server.crt -out node${i}.crt.pem -outform PEM
     openssl pkcs12 -in server.p12 -nocerts -out temp.key.pem -passin pass:mypass -passout pass:password
     openssl pkcs8 -in temp.key.pem -topk8 -out node${i}P8.key.pem -passin pass:password -passout pass:password
     rm temp.key.pem
     cp $CAPATH/ca.crt.pem .
     cp ../node${i}c/*.pem .
  cd ..
  rm -rf "node${i}c"
done

 for ((i=0; i < ${NUMBER_OF_CERTS}; i++));
 do
   echo "Generating Java Keystore for node$i..."
   cd node${i}
   keytool -importkeystore -deststorepass password -destkeypass password -destkeystore node${i}.jks -srckeystore server.p12 -srcstoretype PKCS12 -srcstorepass mypass -alias 1
   keytool -changealias -alias "1" -destalias "node${i}" -keypass password -keystore node${i}.jks -storepass password
   keytool -import -trustcacerts -alias "ca" -file ca.crt.pem -keystore node${i}.jks -storepass password -noprompt

   for ((j=0; j < ${NUMBER_OF_CERTS}; j++));
   do
      if [ "$i" -ne "$j" ]; then
         echo "yes" | keytool -import -trustcacerts -alias node${j} -file ../node${j}/server.crt -keystore node${i}.jks -storepass password
      fi
   done
   cd ..
 done




