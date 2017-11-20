@Library('jenkinstools@master') _

import com.splunk.jenkins.DockerRequest;
import com.splunk.tool.plugin.docker.extension.BadDockerExitCode;

def dockerReq = new DockerRequest(steps,
                                  currentBuild,
                                  env,
                                  [imageName: "repo.splunk.com/splunk/products/splact",
                                   userId: "10777",
                                   repoName: "ssh://github.com/splunk/kafka-connect-splunk.git",
                                   runner: "yarn",
                                   remotePath: "/build"])


withSplunkWrapNode("master") {
    try {
        stage("run orca") {
            withCredentials([file(credentialsId: 'srv_releases_orca', variable: 'ORCA_CREDENTIALS')]) {
                sh "tar -ovxf $ORCA_CREDENTIALS";
                splunkPrepareAndCheckOut request: dockerReq,
                                         files: "${WORKSPACE}/.orca, ${WORKSPACE}/.ssh";
            }
            splunkRunScript request:dockerReq,
                            script:
                                """
                                   pwd
                                   pip install splunk_orca==0.8.0 -i https://repo.splunk.com/artifactory/api/pypi/pypi-virtual/simple
                                   cd /build/kakfa-connect-splunk/ci
                                   python kafka_orca_gen.py --data_gen_size 1 --data_gen_eps 100000 --broker_size 3 --zookeeper_size 3 --kafka_connect_size 1 --kafka_connect_max_tasks 20 --indexer_size 3 --default_partitions 10 --perf 0
                                   splunk_orca create --sc kafka-connect
                                """;
        }
    }
    catch (BadDockerExitCode e) {
        currentBuild.result = "FAILURE";
        echo "Exception Caught: ${e.getMessage()}";
        echo "Stack Trace: ${e.printStackTrace()}";
    }
    catch (Exception e) {
        currentBuild.result = "FAILURE";
        echo "Exception Caught: ${e.getMessage()}";
        echo "Stack Trace: ${e.printStackTrace()}";
    }
    finally {
        steps.cleanWs();
    }
}
