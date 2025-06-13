#!/bin/sh
echo Building console version of Traffcik
echo Make sure u performed: cd asconsole
echo 
echo path: $1/traffcik


cp -r files $1/traffcik
mkdir -p $1/traffcik/src/main
mkdir -p $1/traffcik/src/test
cp -r ../app/traffcik $1/traffcik/src/main/scala
cp -r ../test/traffcik $1/traffcik/src/test/scala
cp Start.tobescala $1/traffcik/src/main/scala/Start.scala
cd $1/traffcik
echo Running: sbt compile
sbt compile


echo All done.
echo You can start your app with 'sbt run arg1 arg2', it will be executed in SBT VM
echo But I suggest assembling an executable with 'sbt assembly' and run it with:
echo java -cp $1/target/scala-3.7.1/traffcikByAmwojcik.jar traffcik.simulation.Start traafcik.simulation.Start infile outfile -simple
echo note: exec jar only after assembly, but after compile it will not be fatJAR again!