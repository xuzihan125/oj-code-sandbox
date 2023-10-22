package com.oj.ojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.List;

public class DockerDemo {

    public static void main(String[] args) throws Exception{

//        DockerClientConfig custom = DefaultDockerClientConfig.createDefaultConfigBuilder()
//                .withDockerHost("unix:///var/run/docker.sock")
//                .withDockerTlsVerify(true)
//                .withDockerCertPath("/home/user/.docker")
//                .withRegistryUsername(registryUser)
//                .withRegistryPassword(registryPass)
//                .withRegistryEmail(registryMail)
//                .withRegistryUrl(registryUrl)
//                .build();

//        DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost("unix:///var/run/docker.sock").build();
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String image = "nginx:latest";
        // pull image
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println("download image:" + item.getStatus());
//                super.onNext(item);
//            }
//        };
//        pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
        System.out.println("execute complete");
        // create container
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse exec = containerCmd.withCmd("echo", "this is a self-defined sentence").exec();
        System.out.println(exec);
        String id = exec.getId();
        //        PingCmd pingCmd = dockerClient.pingCmd();
//        pingCmd.exec();
        // check container status
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        List<Container> containerList = listContainersCmd.withShowAll(true).exec();
        for(Container container : containerList){
            System.out.println(container);
        }
        // start container
        dockerClient.startContainerCmd(id).exec();
//        Thread.sleep(5000L);
        // get logs
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback() {
            @Override
            public void onNext(Frame item) {
                System.out.println(item.getStreamType());
                System.out.println("logs:" + new String(item.getPayload()));
                super.onNext(item);
            }
        };
        dockerClient.logContainerCmd(id).withStdOut(true).withStdErr(true).exec(logContainerResultCallback).awaitCompletion();
        // remove container
        dockerClient.removeContainerCmd(id).withForce(true).exec();

        // remove image
        dockerClient.removeImageCmd(image).exec();
    }
}
