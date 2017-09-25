#!/bin/bash

stage_dir=/tmp/repackage-kafka-connect-splunk
stage_dir2=${stage_dir}/cf
kcs_jar=kafka-connect-splunk-1.0-SNAPSHOT.jar
cf_jar=cloudfwd-1.0-SNAPSHOT.jar

/bin/rm -rf ${stage_dir}
mkdir -p ${stage_dir} ${stage_dir2}

cp target/${kcs_jar} ${stage_dir}
cd ${stage_dir} && unzip ${kcs_jar} > /dev/null && rm -f ${kcs_jar}
mv ${cf_jar} ${stage_dir2} && cd ${stage_dir2}
unzip ${cf_jar} > /dev/null && /bin/cp -rf software org com javax mappers.properties bsh ../
cd ..
rm -rf ${stage_dir2}

zip kafka-connect-splunk-1.0-SNAPSHOT.jar * -r > /dev/null
/bin/cp -fv kafka-connect-splunk-1.0-SNAPSHOT.jar /tmp
cd ../
rm -rf ${stage_dir}
